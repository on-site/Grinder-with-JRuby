// Copyright (C) 2007, 2008 Philip Aston
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

package net.grinder.engine.common;

import java.io.File;
import java.io.Serializable;

import net.grinder.util.Directory;
import net.grinder.util.Directory.DirectoryException;


/**
 * Pairing of a script file and its root directory. The directory is not
 * necessarily the immediate parent of the file.
 *
 * @author Philip Aston
 * @version $Revision: 4000 $
 */
public final class ScriptLocation implements Serializable {
  private static final long serialVersionUID = 771173195260716872L;

  private final Directory m_directory;
  private final File m_shortFile;
  private final File m_absoluteFile;

  /**
   * Constructor for ScriptLocation.
   *
   * @param directory
   *            Script working directory. May be relative (to the CWD).
   * @param file
   *            The script file. May be relative (to <code>directory</code>).
   *            If absolute, it needn't be below the root directory.
   */
  public ScriptLocation(Directory directory, File file) {

    m_directory = directory;

    // Try to shorten the name.
    final File relativeFile = directory.getRelativePath(file);

    if (relativeFile != null) {
      m_shortFile = relativeFile;
    }
    else {
      m_shortFile = file;
    }

    if (file.isAbsolute()) {
      m_absoluteFile = file;
    }
    else {
      m_absoluteFile = directory.getFile(file);
    }
  }

  /**
   * Constructor for ScriptLocation based on the current working directory.
   *
   * @param file
   *            The script file.
   * @throws DirectoryException
   *            If the parent directory is invalid.
   */
  public ScriptLocation(File file) throws DirectoryException {
    this(new Directory(), file);
  }

  /**
   * Accessor for the script working directory.
   *
   * @return The directory.
   */
  public Directory getDirectory() {
    return m_directory;
  }

  /**
   * Accessor for the script file. The returned <code>File</code> always
   * represents an absolute path.
   *
   * @return The file.
   */
  public File getFile() {
    return m_absoluteFile;
  }

  /**
   * String representation.
   *
   * @return The string.
   */
  public String toString() {
    return m_shortFile.getPath();
  }

  /**
   * Hash code.
   *
   * @return The hash code.
   */
  public int hashCode() {
    return getDirectory().hashCode() ^ getFile().hashCode();
  }

  /**
   * Equality.
   *
   * @param other Object to compare.
   * @return <code>true</code> if and only if we're equal to <code>other</code>.
   */
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }

    if (other == null || other.getClass() != ScriptLocation.class) {
      return false;
    }

    final ScriptLocation otherScriptLocation = (ScriptLocation)other;

    return getDirectory().equals(otherScriptLocation.getDirectory()) &&
           getFile().equals(otherScriptLocation.getFile());
  }
}
