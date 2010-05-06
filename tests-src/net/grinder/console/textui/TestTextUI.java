// Copyright (C) 2008 - 2009 Philip Aston
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

package net.grinder.console.textui;

import java.util.HashMap;

import net.grinder.common.Logger;
import net.grinder.common.LoggerStubFactory;
import net.grinder.common.processidentity.ProcessReport;
import net.grinder.common.processidentity.WorkerProcessReport;
import net.grinder.console.common.ErrorHandler;
import net.grinder.console.common.Resources;
import net.grinder.console.common.StubResources;
import net.grinder.console.common.processidentity.StubAgentProcessReport;
import net.grinder.console.common.processidentity.StubWorkerProcessReport;
import net.grinder.console.communication.ProcessControl;
import net.grinder.console.communication.StubProcessReports;
import net.grinder.console.communication.ProcessControl.ProcessReports;
import net.grinder.console.model.SampleModel;
import net.grinder.engine.agent.StubAgentIdentity;
import net.grinder.testutility.AssertUtilities;
import net.grinder.testutility.CallData;
import net.grinder.testutility.RandomStubFactory;
import junit.framework.TestCase;


/**
 * Unit tests for {@link TextUI}.
 *
 * @author Philip Aston
 * @version $Revision: 4006 $
 */
public class TestTextUI extends TestCase {

  private final Resources m_resources = new StubResources<String>(
    new HashMap<String, String>() {{
      put("finished.text", "done");
      put("noConnectedAgents.text", "no agents!");
      put("processTable.threads.label", "strings");
      put("processTable.agentProcess.label", "AG");
      put("processTable.workerProcess.label", "WK");
      put("processState.started.label", "hot to trot");
      put("processState.running.label", "rolling");
      put("processState.connected.label", "plugged in");
      put("processState.finished.label", "fini");
      put("processState.disconnected.label", "that's all folks");
      put("processState.unknown.label", "huh");
    }}
  );

  private final LoggerStubFactory m_loggerStubFactory = new LoggerStubFactory();
  private final Logger m_logger = m_loggerStubFactory.getLogger();

  private final RandomStubFactory<ProcessControl> m_processControlStubFactory =
    RandomStubFactory.create(ProcessControl.class);
  private final ProcessControl m_processControl =
    m_processControlStubFactory.getStub();

  final RandomStubFactory<SampleModel> m_sampleModelStubFactory =
    RandomStubFactory.create(SampleModel.class);
  final SampleModel m_sampleModel =
    m_sampleModelStubFactory.getStub();

  public void testErrorHandler() throws Exception {
    final TextUI textUI =
      new TextUI(m_resources, m_processControl, m_sampleModel, m_logger);
    m_loggerStubFactory.assertOutputMessageContains("The Grinder");
    m_loggerStubFactory.assertNoMoreCalls();

    final ErrorHandler errorHandler = textUI.getErrorHandler();

    errorHandler.handleErrorMessage("I let down their tyres");
    m_loggerStubFactory.assertErrorMessage("I let down their tyres");
    m_loggerStubFactory.assertNoMoreCalls();

    errorHandler.handleErrorMessage("with matches", "seeyamate");
    final CallData callData =
      m_loggerStubFactory.assertErrorMessageContains("with matches");
    AssertUtilities.assertContains(callData.getParameters()[0].toString(),
                                   "seeyamate");
    m_loggerStubFactory.assertNoMoreCalls();

    final RuntimeException exception = new RuntimeException("wild dogs");
    errorHandler.handleException(exception);
    m_loggerStubFactory.assertErrorMessage("wild dogs");
    m_loggerStubFactory.assertSuccess("getErrorLogWriter");
    m_loggerStubFactory.assertSuccess("getErrorLogWriter");
    m_loggerStubFactory.assertNoMoreCalls();

    errorHandler.handleException(exception, "the residents");
    m_loggerStubFactory.assertErrorMessage("the residents");
    m_loggerStubFactory.assertSuccess("getErrorLogWriter");
    m_loggerStubFactory.assertSuccess("getErrorLogWriter");
    m_loggerStubFactory.assertNoMoreCalls();

    errorHandler.handleInformationMessage("austin maxi");
    m_loggerStubFactory.assertOutputMessageContains("austin maxi");
    m_loggerStubFactory.assertNoMoreCalls();

    Runtime.getRuntime().removeShutdownHook(textUI.getShutdownHook());
    m_loggerStubFactory.assertNoMoreCalls();

    m_sampleModelStubFactory.assertSuccess(
      "addModelListener", SampleModel.Listener.class);
  }

  public void testProcessStatusListener() throws Exception {
    final TextUI textUI =
      new TextUI(m_resources, m_processControl, m_sampleModel, m_logger);
    m_loggerStubFactory.assertOutputMessageContains("The Grinder");
    m_loggerStubFactory.assertNoMoreCalls();

    final CallData processsControlCall =
      m_processControlStubFactory.assertSuccess(
        "addProcessStatusListener", ProcessControl.Listener.class);
    final ProcessControl.Listener processListener =
      (ProcessControl.Listener)processsControlCall.getParameters()[0];

    Runtime.getRuntime().removeShutdownHook(textUI.getShutdownHook());
    m_loggerStubFactory.assertNoMoreCalls();

    final ProcessReports[] reports1 = new ProcessReports[0];
    processListener.update(reports1);
    m_loggerStubFactory.assertOutputMessage("no agents!");
    m_loggerStubFactory.assertNoMoreCalls();

    processListener.update(reports1);
    m_loggerStubFactory.assertNoMoreCalls();

    processListener.update(reports1);
    m_loggerStubFactory.assertNoMoreCalls();

    final StubAgentIdentity agentIdentity1 = new StubAgentIdentity("agent1");
    final StubAgentProcessReport agentReport1 =
      new StubAgentProcessReport(agentIdentity1, ProcessReport.STATE_RUNNING);

    final WorkerProcessReport workerProcessReport1 =
      new StubWorkerProcessReport(agentIdentity1.createWorkerIdentity(),
                                  ProcessReport.STATE_RUNNING, 3, 6);

    final WorkerProcessReport workerProcessReport2 =
      new StubWorkerProcessReport(agentIdentity1.createWorkerIdentity(),
                                  ProcessReport.STATE_FINISHED, 0, 6);

    processListener.update(
      new ProcessReports[] {
        new StubProcessReports(agentReport1,
                               new WorkerProcessReport[] {
                                 workerProcessReport1,
                                 workerProcessReport2,
                               }),
      });

    m_loggerStubFactory.assertOutputMessage(
      "AG agent1 [plugged in] " +
      "{ WK agent1-0 [rolling (3/6 strings)], WK agent1-1 [fini] }");

    m_loggerStubFactory.assertNoMoreCalls();

    processListener.update(
      new ProcessReports[] {
        new StubProcessReports(agentReport1,
                               new WorkerProcessReport[] {
                                 workerProcessReport2,
                                 workerProcessReport1,
                               }),
      });

    m_loggerStubFactory.assertNoMoreCalls();

    final StubAgentIdentity agentIdentity2 = new StubAgentIdentity("agent2");
    final StubAgentProcessReport agentReport2 =
      new StubAgentProcessReport(agentIdentity2, ProcessReport.STATE_FINISHED);

    processListener.update(
      new ProcessReports[] {
          new StubProcessReports(agentReport2,
            new WorkerProcessReport[] { }),
        new StubProcessReports(agentReport1,
                               new WorkerProcessReport[] {
                                 workerProcessReport2,
                                 workerProcessReport1,
                               }),
      });

    m_loggerStubFactory.assertOutputMessage(
      "AG agent1 [plugged in] " +
      "{ WK agent1-0 [rolling (3/6 strings)], WK agent1-1 [fini] }, " +
      "AG agent2 [that's all folks]");

    m_loggerStubFactory.assertNoMoreCalls();
  }

  public void testSampleModelListener() throws Exception {
    new TextUI(m_resources, m_processControl, m_sampleModel, m_logger);
    m_loggerStubFactory.assertOutputMessageContains("The Grinder");
    m_loggerStubFactory.assertNoMoreCalls();

    final Object[] addListenerParameters =
      m_sampleModelStubFactory.assertSuccess(
        "addModelListener", SampleModel.Listener.class).getParameters();

    final SampleModel.Listener listener =
      (SampleModel.Listener)addListenerParameters[0];

    m_sampleModelStubFactory.setResult(
      "getState",
      new SampleModel.State() {
          public String getDescription() {
            return "no pressure son";
          }

          public boolean isCapturing() {
            return false;
          }

          public boolean isStopped() {
            return false;
          }
        } );


    listener.newSample();
    listener.newTests(null, null);
    listener.resetTests();
    m_loggerStubFactory.assertNoMoreCalls();


    listener.stateChanged();
    m_loggerStubFactory.assertOutputMessage("no pressure son");
    m_loggerStubFactory.assertNoMoreCalls();
  }

  public void testShutdownHook() throws Exception {
    final TextUI textUI =
      new TextUI(m_resources, m_processControl, m_sampleModel, m_logger);

    m_loggerStubFactory.assertOutputMessageContains("The Grinder");
    m_loggerStubFactory.assertNoMoreCalls();

    final Thread shutdownHook = textUI.getShutdownHook();
    assertTrue(Runtime.getRuntime().removeShutdownHook(shutdownHook));
    m_loggerStubFactory.assertNoMoreCalls();

    shutdownHook.run();
    m_loggerStubFactory.assertOutputMessage("done");
    m_loggerStubFactory.assertNoMoreCalls();

    shutdownHook.run();
    m_loggerStubFactory.assertNoMoreCalls();
  }
}
