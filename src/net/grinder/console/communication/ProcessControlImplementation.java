// Copyright (C) 2007 - 2008 Philip Aston
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

import java.util.Timer;

import net.grinder.common.GrinderProperties;
import net.grinder.common.processidentity.AgentIdentity;
import net.grinder.communication.Message;
import net.grinder.communication.MessageDispatchRegistry;
import net.grinder.communication.MessageDispatchRegistry.AbstractHandler;
import net.grinder.messages.agent.ResetGrinderMessage;
import net.grinder.messages.agent.StartGrinderMessage;
import net.grinder.messages.agent.StopGrinderMessage;
import net.grinder.messages.console.AgentAddress;
import net.grinder.messages.console.AgentProcessReportMessage;
import net.grinder.messages.console.WorkerProcessReportMessage;
import net.grinder.util.AllocateLowestNumber;
import net.grinder.util.AllocateLowestNumberImplementation;


/**
 * Implementation of {@link ProcessControl}.
 *
 * @author Philip Aston
 * @version $Revision: 3995 $
 */
public class ProcessControlImplementation implements ProcessControl {

  private final ConsoleCommunication m_consoleCommunication;

  private final ProcessStatusImplementation m_processStatusSet;

  private final AllocateLowestNumber m_agentNumberMap =
    new AllocateLowestNumberImplementation();

  /**
   * Constructor.
   *
   * @param timer
   *          Timer that can be used to schedule housekeeping tasks.
   * @param consoleCommunication
   *          The console communication handler.
   */
  public ProcessControlImplementation(
    Timer timer,
    ConsoleCommunication consoleCommunication) {

    m_consoleCommunication = consoleCommunication;
    m_processStatusSet =
      new ProcessStatusImplementation(timer, m_agentNumberMap);

    final MessageDispatchRegistry messageDispatchRegistry =
      consoleCommunication.getMessageDispatchRegistry();

    messageDispatchRegistry.set(
      AgentProcessReportMessage.class,
      new AbstractHandler() {
        public void send(Message message) {
          m_processStatusSet.addAgentStatusReport(
            (AgentProcessReportMessage)message);
        }
      }
    );

    messageDispatchRegistry.set(
      WorkerProcessReportMessage.class,
      new AbstractHandler() {
        public void send(Message message) {
          m_processStatusSet.addWorkerStatusReport(
            (WorkerProcessReportMessage)message);
        }
      }
    );
  }

  /**
   * Signal the worker processes to start.
   *
   * @param properties
   *            Properties that override the agent's local properties.
   */
  public void startWorkerProcesses(GrinderProperties properties) {
    final GrinderProperties propertiesToSend =
      properties != null ? properties : new GrinderProperties();

    m_agentNumberMap.forEach(new AllocateLowestNumber.IteratorCallback() {
      public void objectAndNumber(Object object, int number) {
        m_consoleCommunication.sendToAddressedAgents(
          new AgentAddress((AgentIdentity)object),
          new StartGrinderMessage(propertiesToSend, number));
        }
      });
  }

  /**
   * Signal the worker processes to reset.
   */
  public void resetWorkerProcesses() {
    m_consoleCommunication.sendToAgents(new ResetGrinderMessage());
  }

  /**
   * Signal the agent and worker processes to stop.
   */
  public void stopAgentAndWorkerProcesses() {
    m_consoleCommunication.sendToAgents(new StopGrinderMessage());
  }

  /**
   * Add a listener for process status data.
   *
   * @param listener The listener.
   */
  public void addProcessStatusListener(Listener listener) {
    m_processStatusSet.addListener(listener);
  }

  /**
   * How many agents are live?
   *
   * @return The number of agents.
   */
  public int getNumberOfLiveAgents() {
    return m_processStatusSet.getNumberOfLiveAgents();
  }
}
