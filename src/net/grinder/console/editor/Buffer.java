// Copyright (C) 2005 Philip Aston
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

import net.grinder.console.common.DisplayMessageConsoleException;
import net.grinder.console.editor.BufferImplementation.Listener;


/**
 * Buffer state.
 *
 * @author Philip Aston
 * @version $Revision: 3762 $
 */
public interface Buffer {

  /** Buffer type constant. */
  Type HTML_BUFFER = new BufferImplementation.TypeImplementation("HTML");

  /** Buffer type constant. */
  Type JAVA_BUFFER = new BufferImplementation.TypeImplementation("Java");

  /** Buffer type constant. */
  Type MSDOS_BATCH_BUFFER =
    new BufferImplementation.TypeImplementation("MSDOS batch");

  /** Buffer type constant. */
  Type PROPERTIES_BUFFER =
    new BufferImplementation.TypeImplementation("Properties");

  /** Buffer type constant. */
  Type PYTHON_BUFFER = new BufferImplementation.TypeImplementation("Python");

  /** Buffer type constant. */
  Type SHELL_BUFFER = new BufferImplementation.TypeImplementation("Shell");

  /** Buffer type constant. */
  Type TEXT_BUFFER = new BufferImplementation.TypeImplementation("Text");

  /** Buffer type constant. */
  Type XML_BUFFER = new BufferImplementation.TypeImplementation("XML");

  /** Buffer type constant. */
  Type UNKNOWN_BUFFER = new BufferImplementation.TypeImplementation("Unknown");

  /**
   * Return the buffer's {@link TextSource}.
   *
   * @return The text source.
   */
  TextSource getTextSource();

  /**
   * Update the text source from the file.
   *
   * @exception DisplayMessageConsoleException If the file could not
   * be read from.
   * @exception EditorException If an unexpected problem occurs.
   */
  void load() throws DisplayMessageConsoleException, EditorException;

  /**
   * Update the buffer's file from the text source.
   *
   * @exception DisplayMessageConsoleException If the file could not
   * be written to.
   * @exception EditorException If an unexpected problem occurs.
   */
  void save() throws DisplayMessageConsoleException, EditorException;

  /**
   * Update a file from the text source and, if successful, associate
   * the buffer with the new file.
   *
   * @param file The file.
   * @exception DisplayMessageConsoleException If the file could not
   * be written to.
   */
  void save(File file) throws DisplayMessageConsoleException;

  /**
   * Return whether the buffer's text has been changed since the last
   * save.
   *
   * @return <code>true</code> => the text has changed.
   */
  boolean isDirty();

  /**
   * Return the buffer's associated file.
   *
   * @return The file. <code>null</code> if there is no associated file.
   */
  File getFile();

  /**
   * Return whether the file has been independently modified since the
   * last save.
   *
   * @return <code>true</code> => the file has changed independently
   * of the buffer.
   */
  boolean isUpToDate();

  /**
   * Get the type of the buffer.
   *
   * @return The buffer's type.
   */
  Type getType();

  /**
   * Return display name of buffer.
   *
   * @return The buffer's name.
   */
  String getDisplayName();

  /**
   * Useful for debugging.
   *
   * @return Description of the Buffer.
   */
  String toString();

  /**
   * Add a new listener.
   *
   * @param listener The listener.
   */
  void addListener(Listener listener);

  /**
   * Instances of this opaque class represent a buffer's type.
   *
   */
  interface Type {
  }
}
