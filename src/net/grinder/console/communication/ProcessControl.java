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

package net.grinder.console.communication;

import java.util.Comparator;
import java.util.EventListener;

import net.grinder.common.GrinderProperties;
import net.grinder.common.processidentity.ProcessReport;
import net.grinder.common.processidentity.WorkerProcessReport;
import net.grinder.messages.console.AgentAndCacheReport;


/**
 * Interface for issuing commands to the agent and worker processes.
 *
 * @author Philip Aston
 * @author Dirk Feufel
 * @version $Revision: 4003 $
 */
public interface ProcessControl {

  /**
   * Signal the worker processes to start.
   *
   * @param properties
   *            Properties that override the agent's local properties.
   */
  void startWorkerProcesses(GrinderProperties properties);

  /**
   * Signal the worker processes to reset.
   */
  void resetWorkerProcesses();

  /**
   * Signal the agent and worker processes to stop.
   */
  void stopAgentAndWorkerProcesses();

  /**
   * Add a listener for process status data.
   *
   * @param listener The listener.
   */
  void addProcessStatusListener(Listener listener);

  /**
   * How many agents are live?
   *
   * @return The number of agents.
   */
  int getNumberOfLiveAgents();

  /**
   * Listener interface for receiving updates about process status.
   */
  public interface Listener extends EventListener {

    /**
     * Called with regular updates on process status.
     *
     * @param processReports
     *          Process status information.
     */
    void update(ProcessReports[] processReports);
  }

  /**
   * Interface to the information the console has about an agent and its
   * worker processes.
   */
  interface ProcessReports {

    /**
     * Returns the latest agent process report.
     *
     * @return The agent process report.
     */
    AgentAndCacheReport getAgentProcessReport();

    /**
     * Returns the latest worker process reports.
     *
     * @return The worker process reports.
     */
    WorkerProcessReport[] getWorkerProcessReports();
  }

  /**
   * Comparator for {@link ProcessControl.ProcessReports} that sorts according
   * to the agent report.
   */
  final class ProcessReportsComparator implements Comparator<ProcessReports> {
    private final Comparator<ProcessReport> m_processReportComparator =
      new ProcessReport.StateThenNameThenNumberComparator();

    public int compare(ProcessReports o1, ProcessReports o2) {
      return m_processReportComparator.compare(
        o1.getAgentProcessReport(), o2.getAgentProcessReport());
    }
  }
}
