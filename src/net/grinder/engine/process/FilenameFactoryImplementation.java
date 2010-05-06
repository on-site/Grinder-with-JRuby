// Copyright (C) 2001, 2002, 2003 Philip Aston
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

package net.grinder.engine.process;

import java.io.File;

import net.grinder.common.FilenameFactory;


/**
 * Implementation of {@link FilenameFactory}.
 *
 * @author Philip Aston
 * @version $Revision: 3762 $
 */
final class FilenameFactoryImplementation implements FilenameFactory {
  private final String m_logDirectory;
  private final String m_contextString;

  FilenameFactoryImplementation(File logDirectory, String grinderID) {
    this(logDirectory.getPath(), "_" + grinderID);
  }

  private FilenameFactoryImplementation(String logDirectory,
                                        String contextString) {
    m_logDirectory = logDirectory;
    m_contextString = contextString;
  }

  FilenameFactoryImplementation createSubContextFilenameFactory(
    String subContext) {

    return
      new FilenameFactoryImplementation(m_logDirectory,
                                        m_contextString + "_" + subContext);
  }

  public String createFilename(String prefix, String suffix) {
    final StringBuffer result = new StringBuffer();

    result.append(m_logDirectory);
    result.append(File.separator);
    result.append(prefix);
    result.append(m_contextString);
    result.append(suffix);

    return result.toString();
  }

  public String createFilename(String prefix) {
    return createFilename(prefix, ".log");
  }
}
