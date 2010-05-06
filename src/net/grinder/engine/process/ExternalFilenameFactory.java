// Copyright (C) 2004 Philip Aston
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

import net.grinder.common.FilenameFactory;


/**
 * {@link net.grinder.common.Logger} implementation for external
 * consumption. Delegates to the appropriate {@link
 * LoggerImplementation} depending on whether it is called from the
 * process thread or a worker thread.
 *
 * <p>This effectively makes <code>ExternalLogger</code> thread safe
 * unless the script creates its own threads.</p>
 *
 * @author Philip Aston
 * @version $Revision: 3762 $
 */
final class ExternalFilenameFactory implements FilenameFactory {

  private final FilenameFactory m_processFilenameFactory;
  private final ThreadContextLocator m_threadContextLocator;

  public ExternalFilenameFactory(FilenameFactory processFilenameFactory,
                                 ThreadContextLocator threadContextLocator) {
    m_processFilenameFactory = processFilenameFactory;
    m_threadContextLocator = threadContextLocator;
  }

  public String createFilename(String prefix) {
    return getFilenameFactory().createFilename(prefix);
  }

  public String createFilename(String prefix, String suffix) {
    return getFilenameFactory().createFilename(prefix, suffix);
  }

  private FilenameFactory getFilenameFactory() {
    final ThreadContext threadContext = m_threadContextLocator.get();

    if (threadContext != null) {
      return threadContext.getFilenameFactory();
    }

    return m_processFilenameFactory;
  }
}
