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

import java.io.File;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.TimerTask;

import net.grinder.common.GrinderProperties;
import net.grinder.common.UncheckedInterruptedException;
import net.grinder.common.processidentity.AgentIdentity;
import net.grinder.communication.Address;
import net.grinder.communication.CommunicationException;
import net.grinder.communication.ConnectionType;
import net.grinder.communication.Message;
import net.grinder.communication.SendToEveryoneAddress;
import net.grinder.communication.Sender;
import net.grinder.communication.StreamSender;
import net.grinder.communication.StubConnector;
import net.grinder.console.common.DisplayMessageConsoleException;
import net.grinder.console.common.ErrorHandler;
import net.grinder.console.common.Resources;
import net.grinder.console.common.ResourcesImplementation;
import net.grinder.console.communication.ProcessControl.ProcessReports;
import net.grinder.console.model.ConsoleProperties;
import net.grinder.engine.agent.StubAgentIdentity;
import net.grinder.messages.agent.CacheHighWaterMark;
import net.grinder.messages.agent.ClearCacheMessage;
import net.grinder.messages.agent.DistributeFileMessage;
import net.grinder.messages.agent.DistributionCacheCheckpointMessage;
import net.grinder.messages.agent.ResetGrinderMessage;
import net.grinder.messages.agent.StartGrinderMessage;
import net.grinder.messages.agent.StopGrinderMessage;
import net.grinder.messages.agent.StubCacheHighWaterMark;
import net.grinder.messages.console.AgentAddress;
import net.grinder.messages.console.AgentProcessReportMessage;
import net.grinder.messages.console.WorkerProcessReportMessage;
import net.grinder.testutility.AbstractFileTestCase;
import net.grinder.testutility.RandomStubFactory;
import net.grinder.testutility.StubTimer;
import net.grinder.util.FileContents;


/**
 * Unit test case for {@link ConsoleCommunicationImplementation}. Also tests
 * {@link ProcessControlImplementation} and
 * {@link DistributionControlImplementation}.
 *
 * @author Philip Aston
 * @version $Revision: 4006 $
 */
public class TestConsoleCommunicationImplementation
  extends AbstractFileTestCase {

  private static final Resources s_resources =
      new ResourcesImplementation(
        "net.grinder.console.common.resources.Console");

  private ConsoleCommunication m_consoleCommunication;
  private ConsoleProperties m_properties;
  private ServerSocket m_usedServerSocket;
  private final ProcessMessagesThread m_processMessagesThread =
    new ProcessMessagesThread();
  private StubTimer m_timer;

  private final RandomStubFactory<ErrorHandler> m_errorHandlerStubFactory =
    RandomStubFactory.create(ErrorHandler.class);
  private final ErrorHandler m_errorHandler =
    m_errorHandlerStubFactory.getStub();

  protected void setUp() throws Exception {
    super.setUp();

    m_timer = new StubTimer();

    // Figure out a used and free local port.
    m_usedServerSocket = new ServerSocket(0, 50, InetAddress.getByName(null));
    final ServerSocket freeSocket =
      new ServerSocket(0, 50, InetAddress.getByName(null));
    freeSocket.close();


    final File file = new File(getDirectory(), "properties");
    m_properties = new ConsoleProperties(s_resources, file);

    m_properties.setConsolePort(freeSocket.getLocalPort());

    m_consoleCommunication =
      new ConsoleCommunicationImplementation(s_resources,
                                             m_properties,
                                             m_errorHandler,
                                             10);
  }

  protected void tearDown() throws Exception {
    super.tearDown();

    m_consoleCommunication.shutdown();

    m_processMessagesThread.interrupt();
    m_processMessagesThread.join();

    m_timer.cancel();

    m_usedServerSocket.close();

    waitForNumberOfConnections(0);
  }

  public void testConstruction() throws Exception {
    // Need a thread to be attempting to process messages or
    // ConsoleCommunicationImplementation.reset() will not complete.
    m_processMessagesThread.start();

    // Cause the sender to be invalid.
    m_properties.setConsolePort(m_usedServerSocket.getLocalPort());

    final ConsoleCommunicationImplementation consoleCommunication =
      new ConsoleCommunicationImplementation(s_resources,
                                             m_properties,
                                             m_errorHandler,
                                             500);

    assertEquals(0, consoleCommunication.getNumberOfConnections());

    final ConsoleCommunicationImplementation consoleCommunication2 =
      new ConsoleCommunicationImplementation(s_resources,
                                             m_properties,
                                             m_errorHandler);

    assertEquals(0, consoleCommunication2.getNumberOfConnections());
  }

  public void testShutdown() throws Exception {

    m_processMessagesThread.start();

    new StubConnector(InetAddress.getByName(null).getHostName(),
                      m_properties.getConsolePort(),
                      ConnectionType.AGENT)
      .connect();

    waitForNumberOfConnections(1);

    m_consoleCommunication.shutdown();

    waitForNumberOfConnections(0);
  }

  private Message readMessage(Socket socket) throws Exception {
    final ObjectInputStream objectStream =
      new ObjectInputStream(socket.getInputStream());

    return (Message)objectStream.readObject();
  }

  private void sendMessage(Socket socket, Message message) throws Exception {
    final ObjectOutputStream objectStream =
      new ObjectOutputStream(socket.getOutputStream());

    objectStream.writeObject(message);
    objectStream.flush();
  }

  public void testWithProcessControl() throws Exception {
    // We need to associate the agent id with the connection or we'll never
    // get a start message.
    final AgentIdentity agentIdentity = new StubAgentIdentity("foo");

    final Socket socket =
      new StubConnector(InetAddress.getByName(null).getHostName(),
                        m_properties.getConsolePort(),
                        ConnectionType.AGENT)
      .connect(new AgentAddress(agentIdentity));

    waitForNumberOfConnections(1);

    final ProcessControl processControl =
      new ProcessControlImplementation(m_timer, m_consoleCommunication);

    final RandomStubFactory<ProcessControl.Listener> listenerStubFactory =
      RandomStubFactory.create(ProcessControl.Listener.class);

    final CacheHighWaterMark cacheHighWaterMark =
      new StubCacheHighWaterMark("cache", 100);

    processControl.addProcessStatusListener(listenerStubFactory.getStub());

    processControl.resetWorkerProcesses();
    processControl.stopAgentAndWorkerProcesses();

    assertTrue(readMessage(socket) instanceof ResetGrinderMessage);
    assertTrue(readMessage(socket) instanceof StopGrinderMessage);

    final GrinderProperties properties = new GrinderProperties();
    properties.setProperty("foo", "bah");

    // Need a thread to be attempting to process messages.
    m_processMessagesThread.start();

    new StreamSender(socket.getOutputStream()).send(
      new AgentProcessReportMessage(
        agentIdentity,
        AgentProcessReportMessage.STATE_RUNNING,
        cacheHighWaterMark));

    // Wait until the message has been received.
    do {
      final TimerTask timerTask = m_timer.getTaskByPeriod(500L);
      timerTask.run();
      Thread.sleep(10);
    }
    while (listenerStubFactory.peekFirst() == null);
    listenerStubFactory.assertSuccess("update", ProcessReports[].class);
    listenerStubFactory.assertNoMoreCalls();

    processControl.startWorkerProcesses(properties);
    final StartGrinderMessage startGrinderMessage =
      (StartGrinderMessage)readMessage(socket);

    assertEquals(properties, startGrinderMessage.getProperties());
    assertEquals(0, startGrinderMessage.getAgentNumber());

    processControl.startWorkerProcesses(null);
    final StartGrinderMessage startGrinderMessage2 =
      (StartGrinderMessage)readMessage(socket);
    assertEquals(0, startGrinderMessage2.getProperties().size());

    // This shouldn't call reset. If it does, we'll block because
    // nothing's processing the messages.
    m_properties.setIgnoreSampleCount(99);

    // Reset by changing properties and do another test.
    final ServerSocket freeServerSocket = new ServerSocket(0);
    freeServerSocket.close();
    m_properties.setConsolePort(freeServerSocket.getLocalPort());

    // Changing the port drops the existing connections.
    waitForNumberOfConnections(0);

    final Socket socket2 =
      new StubConnector(InetAddress.getByName(null).getHostName(),
                        m_properties.getConsolePort(),
                        ConnectionType.AGENT)
      .connect();

    // Make sure something is listening to our new connection.
    waitForNumberOfConnections(1);

    processControl.resetWorkerProcesses();

    assertTrue(readMessage(socket2) instanceof ResetGrinderMessage);
  }

  public void testDistributionControl() throws Exception {
    final Socket socket =
      new StubConnector(InetAddress.getByName(null).getHostName(),
                        m_properties.getConsolePort(),
                        ConnectionType.AGENT)
      .connect();

    final DistributionControl distributionControl =
      new DistributionControlImplementation(m_consoleCommunication);

    final Socket socket2 =
      new StubConnector(InetAddress.getByName(null).getHostName(),
                        m_properties.getConsolePort(),
                        ConnectionType.AGENT)
      .connect();

    waitForNumberOfConnections(2);

    socket2.close();

    final Address address = new SendToEveryoneAddress();

    // Closing the socket isn't enough for the ConsoleCommunication's Sender to
    // know we've gone (and so close its end of the connection); we need to send
    // something too.
    // Sadly it appears we sometimes need to chuck more than one message the
    // socket before it figures out the other end is stuffed.
    int n = 0;

    while (m_consoleCommunication.getNumberOfConnections() != 1) {
      distributionControl.clearFileCaches(address);
      ++n;
      assertTrue(n < 10);
    }

    for (int i = 0; i < n; ++i) {
      assertTrue(readMessage(socket) instanceof ClearCacheMessage);
    }

    final File relativePath = new File("foo");
    final File fullPath = new File(getDirectory(), relativePath.getPath());
    createRandomFile(fullPath);

    final FileContents fileContents = new FileContents(getDirectory(),
      relativePath);

    distributionControl.sendFile(address, fileContents);

    assertTrue(readMessage(socket) instanceof DistributeFileMessage);
    socket.close();

    // Need a thread to be attempting to process messages or
    // ConsoleCommunicationImplementation.reset() will not complete.
    m_processMessagesThread.start();

    // Reset by changing properties and do another test.
    m_properties.setConsoleHost("localhost");

    // Reseting the properties should ditch the existing connections.
    waitForNumberOfConnections(0);

    final Socket socket3 =
      new StubConnector(InetAddress.getByName(null).getHostName(),
                        m_properties.getConsolePort(),
                        ConnectionType.AGENT)
      .connect();

    waitForNumberOfConnections(1);

    distributionControl.clearFileCaches(address);
    assertTrue(readMessage(socket3) instanceof ClearCacheMessage);

    distributionControl.setHighWaterMark(address,
                                         new StubCacheHighWaterMark("", 100));
    assertTrue(
      readMessage(socket3) instanceof DistributionCacheCheckpointMessage);
  }

  /**
   * Connections are accepted by separate threads so we need to spin a while.
   * @param n - Wait until there are this number of accepted connections.
   * @throws InterruptedException
   */
  private void waitForNumberOfConnections(int n) throws InterruptedException {
    for (int retry = 0;
         m_consoleCommunication.getNumberOfConnections() != n && retry < 200;
         ++retry) {
      Thread.sleep(10);
    }

    assertEquals(n, m_consoleCommunication.getNumberOfConnections());
  }

  public void testProcessOneMessage() throws Exception {
    final MessageHandlerStubFactory messageHandlerStubFactory =
      new MessageHandlerStubFactory();

    m_consoleCommunication.getMessageDispatchRegistry().addFallback(
      messageHandlerStubFactory.getStub());

    m_processMessagesThread.start();

    final ProcessControl processControl =
      new ProcessControlImplementation(m_timer, m_consoleCommunication);

    assertEquals(0, processControl.getNumberOfLiveAgents());

    final Socket socket =
      new StubConnector(InetAddress.getByName(null).getHostName(),
                        m_properties.getConsolePort(),
                        ConnectionType.AGENT)
      .connect();

    final StubAgentIdentity agentIdentity = new StubAgentIdentity("agent");

    // We can currently send agent messages over a worker channel.
    sendMessage(socket,
      new AgentProcessReportMessage(agentIdentity, (short)0, null));

    sendMessage(
      socket,
      new WorkerProcessReportMessage(agentIdentity.createWorkerIdentity(),
                                     (short)0,
                                     (short)0,
                                     (short)0));

    sendMessage(socket, new MyMessage());

    messageHandlerStubFactory.waitUntilCalled(10000);

    messageHandlerStubFactory.assertSuccess("send", MyMessage.class);

    assertEquals(1, processControl.getNumberOfLiveAgents());

    // ConsoleCommunication should have handled the original
    // AgentProcessReportMessage and WorkerProcessReportMessage. We check here
    // so we're sure the've been processed.
    messageHandlerStubFactory.assertNoMoreCalls();

    sendMessage(socket, new StopGrinderMessage());

    messageHandlerStubFactory.waitUntilCalled(10000);

    messageHandlerStubFactory.assertSuccess("send",
                                            StopGrinderMessage.class);
  }

  public void testSendExceptions() throws Exception {
    // Need a thread to be attempting to process messages or
    // ConsoleCommunicationImplementation.reset() will not complete.
    m_processMessagesThread.start();

    // Cause the sender to be invalid.
    m_properties.setConsolePort(m_usedServerSocket.getLocalPort());

    m_errorHandlerStubFactory.assertSuccess(
      "handleException", DisplayMessageConsoleException.class);
    m_errorHandlerStubFactory.assertNoMoreCalls();

    m_consoleCommunication.sendToAddressedAgents(
      new AgentAddress(new StubAgentIdentity("agent")), new MyMessage());

    m_errorHandlerStubFactory.assertSuccess(
      "handleException", DisplayMessageConsoleException.class);
    m_errorHandlerStubFactory.assertNoMoreCalls();

    m_consoleCommunication.sendToAgents(new MyMessage());
    m_errorHandlerStubFactory.assertSuccess(
      "handleException", DisplayMessageConsoleException.class);
    m_errorHandlerStubFactory.assertNoMoreCalls();


    m_properties.setConsolePort(m_usedServerSocket.getLocalPort());
    final ConsoleCommunication brokenConsoleCommunication =
      new ConsoleCommunicationImplementation(s_resources,
                                             m_properties,
                                             m_errorHandler,
                                             100);

    m_errorHandlerStubFactory.assertSuccess(
      "handleException", DisplayMessageConsoleException.class);
    m_errorHandlerStubFactory.assertNoMoreCalls();

    brokenConsoleCommunication.sendToAddressedAgents(
      new AgentAddress(new StubAgentIdentity("agent")), new MyMessage());
    m_errorHandlerStubFactory.assertSuccess("handleErrorMessage", String.class);

    brokenConsoleCommunication.sendToAgents(new MyMessage());
    m_errorHandlerStubFactory.assertSuccess("handleErrorMessage", String.class);

    m_errorHandlerStubFactory.assertNoMoreCalls();
  }

  public void testErrorHandling() throws Exception {
    // Need a thread to be attempting to process messages or the
    // receiver will never be shutdown correctly.
    m_processMessagesThread.start();

    m_errorHandlerStubFactory.assertNoMoreCalls();

    m_properties.setConsolePort(m_usedServerSocket.getLocalPort());

    m_errorHandlerStubFactory.assertSuccess(
      "handleException", DisplayMessageConsoleException.class);
    m_errorHandlerStubFactory.assertNoMoreCalls();

    final Address address = new SendToEveryoneAddress();

    new DistributionControlImplementation(m_consoleCommunication)
    .clearFileCaches(address);

    m_errorHandlerStubFactory.assertSuccess(
      "handleException", DisplayMessageConsoleException.class);
    m_errorHandlerStubFactory.assertNoMoreCalls();

    final RandomStubFactory<ErrorHandler> errorHandlerStubFactory2 =
      RandomStubFactory.create(ErrorHandler.class);

    // Test a ConsoleCommunication with an invalid Sender.
    m_properties.setConsolePort(m_usedServerSocket.getLocalPort());
    final ConsoleCommunication brokenConsoleCommunication =
      new ConsoleCommunicationImplementation(s_resources,
                                             m_properties,
                                             errorHandlerStubFactory2.getStub(),
                                             100);

    errorHandlerStubFactory2.assertSuccess(
      "handleException", DisplayMessageConsoleException.class);
    errorHandlerStubFactory2.assertNoMoreCalls();

    new DistributionControlImplementation(brokenConsoleCommunication)
    .clearFileCaches(address);

    errorHandlerStubFactory2.assertSuccess("handleErrorMessage", String.class);
    errorHandlerStubFactory2.assertNoMoreCalls();
  }

  public void testErrorHandlingWithFurtherCommunicationProblems()
    throws Exception {

    final ServerSocket freeServerSocket = new ServerSocket(0);
    freeServerSocket.close();

    // Need a thread to be attempting to process messages or
    // ConsoleCommunicationImplementation.reset() will not complete.
    m_processMessagesThread.start();

    m_properties.setConsolePort(freeServerSocket.getLocalPort());

    final Socket socket = new Socket(freeServerSocket.getInetAddress(),
                                     freeServerSocket.getLocalPort());

    socket.getOutputStream().close();

    // Will be called via the Acceptor problem listener.
    m_errorHandlerStubFactory.waitUntilCalled(1000);

    m_errorHandlerStubFactory.assertSuccess("handleException",
                                          CommunicationException.class);
    m_errorHandlerStubFactory.assertNoMoreCalls();

    final Socket socket2 =
      new StubConnector(InetAddress.getByName(null).getHostName(),
                        m_properties.getConsolePort(),
                        ConnectionType.AGENT)
      .connect();

    socket2.getOutputStream().write(new byte[100]);

    m_errorHandlerStubFactory.waitUntilCalled(1000);

    m_errorHandlerStubFactory.assertSuccess("handleException",
                                          CommunicationException.class);
    socket.close();
    socket2.close();

    m_errorHandlerStubFactory.assertNoMoreCalls();
  }

  private static final class MyMessage implements Message, Serializable {
    private static final long serialVersionUID = 1L;
  }

  public static final class MessageHandlerStubFactory
    extends RandomStubFactory<Sender> {

    public MessageHandlerStubFactory() {
      super(Sender.class);
    }

    public boolean override_process(Object proxy, Message message) {
      return message instanceof MyMessage;
    }
  }

  private final class ProcessMessagesThread extends Thread {
    public ProcessMessagesThread() {
      super("Process messages");
    }

    public void run() {
      try {
        while (m_consoleCommunication.processOneMessage()) { }
      }
      catch (UncheckedInterruptedException e) {
        // Time to go.
      }
    }
  }
}
