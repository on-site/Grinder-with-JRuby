// Copyright (C) 2001, 2002 Dirk Feufel
// Copyright (C) 2001 - 2008 Philip Aston
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

package net.grinder.messages.console;

import net.grinder.common.processidentity.ProcessIdentity;
import net.grinder.common.processidentity.WorkerIdentity;
import net.grinder.common.processidentity.WorkerProcessReport;
import net.grinder.communication.Message;


/**
 * Message for informing the console of worker process status.
 *
 * @author Dirk Feufel
 * @author Philip Aston
 * @version $Revision: 3878 $
 */
public final class WorkerProcessReportMessage
  implements Message, WorkerProcessReport {

  private static final long serialVersionUID = -2073574340466531680L;

  private final WorkerIdentity m_identity;
  private final short m_state;
  private final short m_totalNumberOfThreads;
  private final short m_numberOfRunningThreads;

  /**
   * Creates a new <code>WorkerProcessReportMessage</code> instance.
   *
   * @param identity Process identity.
   * @param state The process state. See {@link
   * net.grinder.common.processidentity.WorkerProcessReport}.
   * @param totalThreads The total number of threads.
   * @param runningThreads The number of threads that are still running.
   */
  public WorkerProcessReportMessage(WorkerIdentity identity,
                                    short state,
                                    short runningThreads,
                                    short totalThreads) {
    m_identity = identity;
    m_state = state;
    m_numberOfRunningThreads = runningThreads;
    m_totalNumberOfThreads = totalThreads;
  }

  /**
   * Accessor for the process identity.
   *
   * @return The process identity.
   */
  public ProcessIdentity getIdentity() {
    return m_identity;
  }

  /**
   * Accessor for the process identity.
   *
   * @return The process identity.
   */
  public WorkerIdentity getWorkerIdentity() {
    return m_identity;
  }

  /**
   * Accessor for the process state.
   *
   * @return The process state.
   */
  public short getState() {
    return m_state;
  }

  /**
   * Accessor for the number of running threads for the process.
   *
   * @return The number of running threads.
   */
  public short getNumberOfRunningThreads() {
    return m_numberOfRunningThreads;
  }

  /**
   * Accessor for the maximum number of threads for the process.
   *
   * @return The maximum number of threads for the process.
   */
  public short getMaximumNumberOfThreads() {
    return m_totalNumberOfThreads;
  }
}
