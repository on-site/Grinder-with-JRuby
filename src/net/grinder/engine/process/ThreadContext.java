// Copyright (C) 2004, 2005, 2006, 2007 Philip Aston
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
import net.grinder.common.ThreadLifeCycleListener;
import net.grinder.common.SSLContextFactory;
import net.grinder.plugininterface.PluginThreadContext;
import net.grinder.script.Statistics.StatisticsForTest;


/**
 * Package scope.
 *
 * @author Philip Aston
 * @version $Revision: 4186 $
 */
interface ThreadContext extends PluginThreadContext {

  ThreadLogger getThreadLogger();

  FilenameFactory getFilenameFactory();

  DispatchResultReporter getDispatchResultReporter();

  SSLContextFactory getThreadSSLContextFactory();

  void setThreadSSLContextFactory(SSLContextFactory threadSSLFactory);

  void registerThreadLifeCycleListener(ThreadLifeCycleListener listener);

  void removeThreadLifeCycleListener(ThreadLifeCycleListener listener);

  void fireBeginThreadEvent();

  void fireBeginRunEvent();

  void fireEndRunEvent();

  void fireBeginShutdownEvent();

  void fireEndThreadEvent();

  void pushDispatchContext(DispatchContext dispatchContext)
    throws ShutdownException;

  void popDispatchContext();

  StatisticsForTest getStatisticsForCurrentTest();

  StatisticsForTest getStatisticsForLastTest();

  void setDelayReports(boolean b);

  void reportPendingDispatchContext();

  void shutdown();
}

