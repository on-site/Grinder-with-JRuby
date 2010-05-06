// Copyright (C) 2006 - 2008 Philip Aston
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

import net.grinder.common.GrinderProperties;
import net.grinder.common.Logger;
import net.grinder.messages.console.ReportStatisticsMessage;
import net.grinder.messages.console.WorkerProcessReportMessage;
import net.grinder.statistics.StatisticsServices;
import net.grinder.statistics.TestStatisticsMap;
import net.grinder.util.Sleeper;


/**
 * Interface to worker process state.
 *
 * @author Philip Aston
 * @version $Revision: 3965 $
 */
interface ProcessContext {

  WorkerProcessReportMessage createStatusMessage(short state,
    short numberOfThreads, short totalNumberOfThreads);

  ReportStatisticsMessage createReportStatisticsMessage(
    TestStatisticsMap sample);

  Logger getProcessLogger();

  TestRegistryImplementation getTestRegistry();

  GrinderProperties getProperties();

  ThreadContextLocator getThreadContextLocator();

  /**
   * {@link GrinderProcess} calls {@link #setExecutionStartTime} just
   * before launching threads, after which it is never called again.
   */
  void setExecutionStartTime();

  /**
   * {@link GrinderProcess} calls {@link #setExecutionStartTime} just before
   * launching threads, after which it is never called again.
   *
   * @return Start of execution, in milliseconds since the Epoch.
   */
  long getExecutionStartTime();

  /**
   * Elapsed time since execution was started.
   *
   * @return The time in milliseconds.
   * @see #getExecutionStartTime()
   */
  long getElapsedTime();

  void shutdown();

  Sleeper getSleeper();

  StatisticsServices getStatisticsServices();

  void fireThreadCreatedEvent(ThreadContext threadContext);
}
