// Copyright (C) 2008 - 2010 Philip Aston
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

package net.grinder.console;

import java.io.File;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Pattern;

import net.grinder.common.Logger;
import net.grinder.common.LoggerStubFactory;
import net.grinder.common.Test;
import net.grinder.communication.MessageDispatchRegistry;
import net.grinder.communication.Sender;
import net.grinder.console.ConsoleFoundation.UI;
import net.grinder.console.client.ConsoleConnection;
import net.grinder.console.client.ConsoleConnectionException;
import net.grinder.console.client.ConsoleConnectionFactory;
import net.grinder.console.common.ErrorHandler;
import net.grinder.console.common.Resources;
import net.grinder.console.common.StubResources;
import net.grinder.console.communication.ConsoleCommunication;
import net.grinder.console.communication.DistributionControl;
import net.grinder.console.communication.ProcessControl;
import net.grinder.console.communication.server.DispatchClientCommands;
import net.grinder.console.distribution.FileDistribution;
import net.grinder.console.model.ConsoleProperties;
import net.grinder.console.model.SampleModel;
import net.grinder.console.model.SampleModelViews;
import net.grinder.messages.console.RegisterExpressionViewMessage;
import net.grinder.messages.console.RegisterTestsMessage;
import net.grinder.messages.console.ReportStatisticsMessage;
import net.grinder.statistics.ExpressionView;
import net.grinder.statistics.StatisticsServices;
import net.grinder.statistics.StatisticsServicesImplementation;
import net.grinder.statistics.TestStatisticsMap;
import net.grinder.testutility.AbstractFileTestCase;
import net.grinder.testutility.CallData;
import net.grinder.testutility.RandomStubFactory;
import net.grinder.testutility.StubTimer;
import net.grinder.util.Directory;


/**
 * Unit tests for {@link ConsoleFoundation}.
 *
 * @author Philip Aston
 * @version $Revision: 4239 $
 */
public class TestConsoleFoundation extends AbstractFileTestCase {

  private static TestConsoleFoundation s_instance;

  private static void setInstance(TestConsoleFoundation instance) {
    s_instance = instance;
  }

  private final Resources m_resources =
    new StubResources<Object>(new HashMap<String, Object>() {{
    }});

  private final LoggerStubFactory m_loggerStubFactory = new LoggerStubFactory();
  private final Logger m_logger = m_loggerStubFactory.getLogger();

  public void testConstruction() throws Exception {

    final ConsoleFoundation foundation =
      new ConsoleFoundation(m_resources, m_logger);

    setInstance(this);

    final MyUI ui = (MyUI)foundation.createUI(MyUI.class);
    assertNotNull(ui);

    final Thread runConsole = new Thread(new Runnable() {
      public void run() {
        try {
          foundation.run();
        }
        catch (Exception e) {
          // Ignore.
        }
      }
    });

    runConsole.start();

    foundation.shutdown();

    m_loggerStubFactory.assertNoMoreCalls();
  }

  public void testSimpleRun() throws Exception {

    final Timer timer = new Timer(true);

    final ConsoleProperties consoleProperties =
      new ConsoleProperties(m_resources, new File(getDirectory(), "props"));

    final ConsoleFoundation foundation =
      new ConsoleFoundation(m_resources, m_logger, timer, consoleProperties);

    m_loggerStubFactory.assertNoMoreCalls();

    final Thread runConsole = new Thread(new Runnable() {
      public void run() { foundation.run(); }
    });

    // Figure out a free local port.

    final ServerSocket serverSocket =
      new ServerSocket(0, 50, InetAddress.getLocalHost());
    final String hostName = serverSocket.getInetAddress().getHostName();
    final int port = serverSocket.getLocalPort();
    serverSocket.close();
    consoleProperties.setConsoleHost(hostName);
    consoleProperties.setConsolePort(port);

    runConsole.start();

    final ConsoleConnectionFactory ccf = new ConsoleConnectionFactory();

    final int retries = 3;

    for (int i = 0; i < retries; ++i) {
      try {
	final ConsoleConnection client = ccf.connect(hostName, port);
	assertEquals(0, client.getNumberOfAgents());
	client.close();
      }
      catch (ConsoleConnectionException e) {
	if (i == retries - 1) {
	  throw e;
	}
	
	Thread.sleep(50);
      }
    }

    foundation.shutdown();

    runConsole.join();
  }

  public void testWireFileDistribution() throws Exception {

    final RandomStubFactory<FileDistribution> fileDistributionStubFactory =
      RandomStubFactory.create(FileDistribution.class);
    final ConsoleProperties consoleProperties =
      new ConsoleProperties(m_resources, new File(getDirectory(), "props"));

    final StubTimer timer = new StubTimer();

    new ConsoleFoundation.WireFileDistribution(
      fileDistributionStubFactory.getStub(),
      consoleProperties,
      timer);

    fileDistributionStubFactory.assertNoMoreCalls();

    assertEquals(6000, timer.getLastDelay());
    assertEquals(6000, timer.getLastPeriod());

    final TimerTask scanFileTask = timer.getLastScheduledTimerTask();
    scanFileTask.run();
    fileDistributionStubFactory.assertSuccess("scanDistributionFiles");
    fileDistributionStubFactory.assertNoMoreCalls();

    consoleProperties.setDistributionFileFilterExpression(".*");
    final CallData setFileFilterCall =
      fileDistributionStubFactory.assertSuccess("setFileFilterPattern",
                                                Pattern.class);
    final Pattern pattern = (Pattern)setFileFilterCall.getParameters()[0];
    assertEquals(".*", pattern.pattern());
    fileDistributionStubFactory.assertNoMoreCalls();

    final Directory directory = new Directory(new File(getDirectory(), "foo"));
    consoleProperties.setAndSaveDistributionDirectory(directory);
    final CallData setDirectoryCall =
      fileDistributionStubFactory.assertSuccess("setDirectory",
                                                Directory.class);
    assertSame(directory, setDirectoryCall.getParameters()[0]);

    consoleProperties.setConsolePort(999);
    fileDistributionStubFactory.assertNoMoreCalls();
  }

  public void testWireMessageDispatch() throws Exception {
    final RandomStubFactory<MessageDispatchRegistry>
      messageDispatchRegistryStubFactory =
        RandomStubFactory.create(MessageDispatchRegistry.class);

    final RandomStubFactory<ConsoleCommunication>
      consoleCommunicationStubFactory =
        RandomStubFactory.create(ConsoleCommunication.class);
    consoleCommunicationStubFactory.setResult(
      "getMessageDispatchRegistry",
      messageDispatchRegistryStubFactory.getStub());

    final RandomStubFactory<SampleModel> modelStubFactory =
      RandomStubFactory.create(SampleModel.class);

    final RandomStubFactory<SampleModelViews> sampleModelViewsStubFactory =
      RandomStubFactory.create(SampleModelViews.class);

    final DispatchClientCommands dispatchClientCommands =
      new DispatchClientCommands(null, null, null);

    new ConsoleFoundation.WireMessageDispatch(
      consoleCommunicationStubFactory.getStub(),
      modelStubFactory.getStub(),
      sampleModelViewsStubFactory.getStub(),
      dispatchClientCommands);

    consoleCommunicationStubFactory.assertSuccess("getMessageDispatchRegistry");
    consoleCommunicationStubFactory.assertNoMoreCalls();

    final CallData call1 =
      messageDispatchRegistryStubFactory.assertSuccess(
        "set", Class.class, Sender.class);
    assertEquals(RegisterTestsMessage.class, call1.getParameters()[0]);

    final Sender sender1 = (Sender) call1.getParameters()[1];
    final Collection<Test> tests = Collections.emptySet();
    sender1.send(new RegisterTestsMessage(tests));
    modelStubFactory.assertSuccess("registerTests", tests);
    modelStubFactory.assertNoMoreCalls();

    final CallData call2 =
      messageDispatchRegistryStubFactory.assertSuccess(
        "set", Class.class, Sender.class);
    assertEquals(ReportStatisticsMessage.class, call2.getParameters()[0]);

    final Sender sender2 = (Sender) call2.getParameters()[1];
    final TestStatisticsMap delta = new TestStatisticsMap();
    sender2.send(new ReportStatisticsMessage(delta));
    modelStubFactory.assertSuccess("addTestReport", delta);
    modelStubFactory.assertNoMoreCalls();

    final CallData call3 =
      messageDispatchRegistryStubFactory.assertSuccess(
        "set", Class.class, Sender.class);
    assertEquals(RegisterExpressionViewMessage.class, call3.getParameters()[0]);

    final Sender sender3 = (Sender) call3.getParameters()[1];
    final ExpressionView expressionView =
      StatisticsServicesImplementation.getInstance()
      .getStatisticExpressionFactory().createExpressionView(
        "blah", "userLong0", false);
    sender3.send(new RegisterExpressionViewMessage(expressionView));
    sampleModelViewsStubFactory.assertSuccess(
      "registerStatisticExpression", expressionView);
    sampleModelViewsStubFactory.assertNoMoreCalls();
  }

  public static class MyUI implements UI {
    public MyUI(Logger logger,
                Resources resources,
                ConsoleProperties properties,
                StatisticsServices statisticsServices,
                SampleModel model,
                ConsoleCommunication consoleCommunication,
                DistributionControl distributionControl,
                FileDistribution fileDistribution,
                ProcessControl processControl) {
      assertSame(s_instance.m_logger, logger);
      assertSame(s_instance.m_resources, resources);
      assertNotNull(properties);
      assertSame(StatisticsServicesImplementation.getInstance(),
                 statisticsServices);
      assertNotNull(model);
      assertNotNull(consoleCommunication);
      assertNotNull(distributionControl);
      assertNotNull(fileDistribution);
      assertNotNull(processControl);
    }

    public ErrorHandler getErrorHandler() {
      return null;
    }
  }
}
