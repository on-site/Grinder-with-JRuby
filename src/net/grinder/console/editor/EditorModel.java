// Copyright (C) 2004 - 2009 Philip Aston
// All rights reserved.
//
// This file is part of The Grinder software distribution. Refer to
// the file LICENSE which is part of The Grinder distribution for
// licensing details. The Grinder distribution is available on the
// Internet at http://grinder.sourceforge.net/
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
// "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
// LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
// FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
// COPYRIGHT HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
// INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
// (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
// SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
// HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
// STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
// ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
// OF THE POSSIBILITY OF SUCH DAMAGE.

package net.grinder.console.editor;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.EventListener;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import net.grinder.common.GrinderProperties;
import net.grinder.common.GrinderProperties.PersistenceException;
import net.grinder.console.common.ConsoleException;
import net.grinder.console.common.DisplayMessageConsoleException;
import net.grinder.console.common.Resources;
import net.grinder.console.distribution.AgentCacheState;
import net.grinder.console.distribution.FileChangeWatcher;
import net.grinder.console.distribution.FileChangeWatcher.FileChangedListener;
import net.grinder.util.ListenerSupport;


/**
 * Editor model.
 *
 * @author Philip Aston
 * @version $Revision: 4003 $
 */
public final class EditorModel {

  private final Resources m_resources;
  private final TextSource.Factory m_textSourceFactory;
  private final AgentCacheState m_agentCacheState;
  //private final Buffer m_defaultBuffer;

  private final ListenerSupport<Listener> m_listeners =
    new ListenerSupport<Listener>();

  // Guarded by itself.
  private final LinkedList<Buffer> m_bufferList = new LinkedList<Buffer>();

  // Guarded by itself.
  private final Map<File, Buffer> m_fileBuffers =
    Collections.synchronizedMap(new HashMap<File, Buffer>());

  // Guarded by this.
  private int m_nextNewBufferNameIndex = 0;

  // Guarded by this.
  private Buffer m_selectedBuffer;

  // Guarded by this.
  private File m_selectedProperties;

  // Guarded by this.
  private File m_selectedFile;

  // Guarded by this.
  private ExternalEditor m_externalEditor;

  /**
   * Constructor.
   *
   * @param resources ResourcesImplementation.
   * @param textSourceFactory Factory for {@link TextSource}s.
   * @param agentCacheState Notified when the model updates a file.
   * @param fileChangeWatcher A FileDistribution.
   */
  public EditorModel(Resources resources,
                     TextSource.Factory textSourceFactory,
                     AgentCacheState agentCacheState,
                     FileChangeWatcher fileChangeWatcher) {
    m_resources = resources;
    m_textSourceFactory = textSourceFactory;
    m_agentCacheState = agentCacheState;

    fileChangeWatcher.addFileChangedListener(new FileChangedListener() {
      public void filesChanged(File[] files) {
        synchronized (m_fileBuffers) {
          for (int i = 0; i < files.length; ++i) {
            final Buffer buffer = getBufferForFile(files[i]);

            if (buffer != null && !buffer.isUpToDate()) {
              fireBufferNotUpToDate(buffer);
            }

            parseSelectedProperties(files[i]);
          }
        }
      }
    });
  }

  /**
   * Get the currently active buffer.
   *
   * @return The active buffer.
   */
  public Buffer getSelectedBuffer() {
    synchronized (this) {
      return m_selectedBuffer;
    }
  }

  /**
   * Select a new buffer.
   */
  public void selectNewBuffer() {
    final Buffer buffer = new BufferImplementation(m_resources,
                                                   m_textSourceFactory.create(),
                                                   createNewBufferName());
    addBuffer(buffer);

    selectBuffer(buffer);
  }

  /**
   * Select the buffer for the given file.
   *
   * @param file
   *          The file.
   * @return The buffer.
   * @throws ConsoleException
   *           If a buffer could not be selected for the file.
   */
  public Buffer selectBufferForFile(File file) throws ConsoleException {
    final Buffer existingBuffer = getBufferForFile(file);
    final Buffer buffer;

    if (existingBuffer != null) {
      buffer = existingBuffer;

      selectBuffer(buffer);

      if (!buffer.isUpToDate()) {
        // The user's edits conflict with a file system change.
        // We ensure the buffer is selected before firing this event because
        // the UI might only raise out of date warnings for selected buffers.
        fireBufferNotUpToDate(buffer);
      }
    }
    else {
      buffer = new BufferImplementation(m_resources,
                                        m_textSourceFactory.create(),
                                        file);
      buffer.load();
      addBuffer(buffer);

      m_fileBuffers.put(file, buffer);

      selectBuffer(buffer);
    }

    return buffer;
  }

  /**
   * Get the buffer for the given file.
   *
   * @param file
   *          The file.
   * @return The buffer; <code>null</code> => there is no buffer for the file.
   */
  public Buffer getBufferForFile(File file) {
    return m_fileBuffers.get(file);
  }

  /**
   * Return a copy of the current buffer list.
   *
   * @return The buffer list.
   */
  public Buffer[] getBuffers() {
    synchronized (m_bufferList) {
      return m_bufferList.toArray(new Buffer[m_bufferList.size()]);
    }
  }

  /**
   * Return whether one of our buffers is dirty.
   *
   * @return <code>true</code> => a buffer is dirty.
   */
  public boolean isABufferDirty() {
    final Buffer[] buffers = getBuffers();

    for (int i = 0; i < buffers.length; ++i) {
      if (buffers[i].isDirty()) {
        return true;
      }
    }

    return false;
  }

  /**
   * Select a buffer.
   *
   * @param buffer The buffer.
   */
  public void selectBuffer(Buffer buffer) {
    final Buffer oldBuffer = getSelectedBuffer();

    if (buffer == null || !buffer.equals(oldBuffer)) {

      synchronized (this) {
        m_selectedBuffer = buffer;
      }

      if (oldBuffer != null) {
        fireBufferStateChanged(oldBuffer);
      }

      if (buffer != null) {
        fireBufferStateChanged(buffer);
      }
    }
  }

  /**
   * Close a buffer.
   *
   * @param buffer The buffer.
   */
  public void closeBuffer(final Buffer buffer) {
    final boolean removed;

    synchronized (m_bufferList) {
      removed = m_bufferList.remove(buffer);
    }

    if (removed) {
      final File file = buffer.getFile();

      if (buffer.equals(getBufferForFile(file))) {
        m_fileBuffers.remove(file);
      }

      if (buffer.equals(getSelectedBuffer())) {
        final Buffer bufferToSelect;

        synchronized (m_bufferList) {
          final int numberOfBuffers = m_bufferList.size();

          bufferToSelect = numberOfBuffers > 0 ?
              (Buffer)m_bufferList.get(numberOfBuffers - 1) : null;
        }

        selectBuffer(bufferToSelect);
      }

      m_listeners.apply(
        new ListenerSupport.Informer<Listener>() {
          public void inform(Listener l) { l.bufferRemoved(buffer); }
        });
    }
  }

  /**
   * Get the currently selected properties.
   *
   * @return The selected properties.
   */
  public File getSelectedPropertiesFile() {
    synchronized (this) {
      return m_selectedProperties;
    }
  }

  /**
   * Set the currently selected properties.
   *
   * @param selectedProperties The selected properties.
   */
  public void setSelectedPropertiesFile(final File selectedProperties) {
    synchronized (this) {
      m_selectedProperties = selectedProperties;

      if (selectedProperties == null) {
        m_selectedFile = null;
      }
    }

    parseSelectedProperties(selectedProperties);
  }

  private void addBuffer(final Buffer buffer) {
    buffer.getTextSource().addListener(new TextSource.Listener() {
        public void textSourceChanged(boolean dirtyStateChanged) {
          if (dirtyStateChanged) {
            fireBufferStateChanged(buffer);
          }
        }
      });

    buffer.addListener(
      new BufferImplementation.Listener() {
        public void bufferSaved(Buffer buffer, File oldFile) {
          final File newFile = buffer.getFile();

          m_agentCacheState.setNewFileTime(newFile.lastModified());

          if (!newFile.equals(oldFile)) {
            if (oldFile != null) {
              m_fileBuffers.remove(oldFile);
            }

            m_fileBuffers.put(newFile, buffer);

            // Fire that bufferChanged because it is associated with a new
            // file.
            fireBufferStateChanged(buffer);
          }

          parseSelectedProperties(newFile);
        }
      }
      );

    synchronized (m_bufferList) {
      m_bufferList.add(buffer);
    }

    m_listeners.apply(
      new ListenerSupport.Informer<Listener>() {
        public void inform(Listener l) { l.bufferAdded(buffer); }
      });
  }

  private void fireBufferStateChanged(final Buffer buffer) {
    m_listeners.apply(
      new ListenerSupport.Informer<Listener>() {
        public void inform(Listener l) { l.bufferStateChanged(buffer); }
      });
  }

  /**
   * The UI doesn't currently listen to this event, but might want to in the
   * future.
   */
  private void fireBufferNotUpToDate(final Buffer buffer) {
    m_listeners.apply(
      new ListenerSupport.Informer<Listener>() {
        public void inform(Listener l) { l.bufferNotUpToDate(buffer); }
      });
  }

  private String createNewBufferName() {

    final String prefix = m_resources.getString("newBuffer.text");

    synchronized (this) {
      try {
        if (m_nextNewBufferNameIndex == 0) {
          return prefix;
        }
        else {
          return prefix + " " + m_nextNewBufferNameIndex;
        }
      }
      finally {
        ++m_nextNewBufferNameIndex;
      }
    }
  }

  private void parseSelectedProperties(File file) {

    if (file != null && file.equals(getSelectedPropertiesFile())) {
      File selectedFile;

      try {
        final GrinderProperties properties = new GrinderProperties(file);

        selectedFile =
          properties.resolveRelativeFile(
            properties.getFile(GrinderProperties.SCRIPT,
                               GrinderProperties.DEFAULT_SCRIPT))
          .getCanonicalFile();
      }
      catch (PersistenceException e) {
        selectedFile = null;
      }
      catch (IOException e) {
        selectedFile = null;
      }

      synchronized (this) {
        m_selectedFile = selectedFile;
      }
    }
  }

  /**
   * Add a new listener.
   *
   * @param listener The listener.
   */
  public void addListener(Listener listener) {
    m_listeners.add(listener);
  }

  /**
   * Return whether the given file should be considered to be a Python
   * file. For now this is just based on name.
   *
   * @param f The file.
   * @return <code>true</code> => its a Python file.
   */
  public boolean isPythonFile(File f) {
    return
      f != null &&
      (!f.exists() || f.isFile()) &&
      f.getName().toLowerCase().endsWith(".py");
  }

  /**
   * Return whether the given file should be considered to be a grinder
   * properties file. For now this is just based on name.
   *
   * @param f The file.
   * @return <code>true</code> => its a properties file.
   */
  public boolean isPropertiesFile(File f) {
    return
      f != null &&
      (!f.exists() || f.isFile()) &&
      f.getName().toLowerCase().endsWith(".properties");
  }

  /**
   * Return whether the given file is the script file specified in the
   * currently selected properties file.
   *
   * @param f The file.
   * @return <code>true</code> => its the selected script.
   */
  public boolean isSelectedScript(File f) {
    // We don't constrain selection to have a .py extension. If the
    // user really wants to use something else, so be it.
    synchronized (this) {
      return f != null && f.equals(m_selectedFile);
    }
  }

  /**
   * Return whether the given file should be marked as boring.
   *
   * @param f The file.
   * @return a <code>true</code> => its boring.
   */
  public boolean isBoringFile(File f) {
    if (f == null) {
      return false;
    }

    final String name = f.getName().toLowerCase();

    return
      f.isHidden() ||
      name.endsWith(".class") ||
      name.startsWith("~") ||
      name.endsWith("~") ||
      name.startsWith("#") ||
      name.endsWith(".exe") ||
      name.endsWith(".gif") ||
      name.endsWith(".jpeg") ||
      name.endsWith(".jpg") ||
      name.endsWith(".tiff");
  }


  /**
   * Open the given file with the external file.
   *
   * @param file The file.
   * @throws ConsoleException If the file could not be opened.
   */
  public void openWithExternalEditor(final File file) throws ConsoleException {
    final ExternalEditor externalEditor;

    synchronized (this) {
      externalEditor = m_externalEditor;
    }

    if (externalEditor == null) {
      throw new DisplayMessageConsoleException(m_resources,
                                               "externalEditorNotSet.text");
    }

    try {
      externalEditor.open(file);
    }
    catch (IOException e) {
      throw new DisplayMessageConsoleException(m_resources,
                                               "externalEditError.text",
                                               e);
    }
  }

  /**
   * Set the external editor command line.
   *
   * @param command
   *            Path to the external editor executable. <code>null</code> =>
   *            no editor set.
   * @param arguments
   *            Arguments to pass to the external editor. Any <code>%f</code>
   *            will be replaced with the absolute path of the file to edit.
   *            If no <code>%f</code> is found, the file path will be appended
   *            to the end of the command line.
   */
  public void setExternalEditor(File command, String arguments) {
    final ExternalEditor externalEditor;
    if (command == null) {
      externalEditor = null;
    }
    else {
      externalEditor =
        new ExternalEditor(m_agentCacheState, this, command, arguments);
    }

    synchronized (this) {
      m_externalEditor = externalEditor;
    }
  }

  /**
   * Interface for listeners.
   */
  public interface Listener extends EventListener {

    /**
     * Called when a buffer has been added.
     *
     * @param buffer The buffer.
     */
    void bufferAdded(Buffer buffer);

    /**
     * Called when a buffer's state has changed. I.e. the buffer has
     * become dirty, or become clean, or has been selected, or has
     * been unselected, or has become associated with a new file.
     *
     * @param buffer The buffer.
     */
    void bufferStateChanged(Buffer buffer);

    /**
     * Called when an independent modification to a buffer's associated
     * file has been detected.
     *
     * @param buffer The buffer.
     */
    void bufferNotUpToDate(Buffer buffer);

    /**
     * Called when a buffer has been removed.
     *
     * @param buffer The buffer.
     */
    void bufferRemoved(Buffer buffer);
  }

  /**
   * Base {@link EditorModel.Listener} implementation that does nothing.
   */
  public abstract static class AbstractListener implements Listener {

    /**
     * @see EditorModel.Listener#bufferAdded
     * @param buffer The buffer.
     */
    public void bufferAdded(Buffer buffer) { }

    /**
     * @see EditorModel.Listener#bufferStateChanged
     * @param buffer The buffer.
     */
    public void bufferStateChanged(Buffer buffer) { }

    /**
     * @see EditorModel.Listener#bufferNotUpToDate
     * @param buffer The buffer.
     */
    public void bufferNotUpToDate(Buffer buffer) { }

    /**
     * @see EditorModel.Listener#bufferRemoved
     * @param buffer The buffer.
     */
    public void bufferRemoved(Buffer buffer) { }
  }
}
