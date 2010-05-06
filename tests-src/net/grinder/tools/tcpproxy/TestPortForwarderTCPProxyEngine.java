// Copyright (C) 2005 - 2008 Philip Aston
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

package net.grinder.tools.tcpproxy;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

import junit.framework.TestCase;
import net.grinder.common.Logger;
import net.grinder.common.LoggerStubFactory;
import net.grinder.testutility.CallData;
import net.grinder.util.StreamCopier;
import net.grinder.util.TerminalColour;


/**
 * Unit test case for {@link PortForwarderTCPProxyEngine}.
 *
 * @author Philip Aston
 * @version $Revision: 4006 $
 */
public class TestPortForwarderTCPProxyEngine extends TestCase {

  private final MyFilterStubFactory m_requestFilterStubFactory =
    new MyFilterStubFactory();
  private final TCPProxyFilter m_requestFilter =
    m_requestFilterStubFactory.getStub();

  private final MyFilterStubFactory m_responseFilterStubFactory =
    new MyFilterStubFactory();
  private final TCPProxyFilter m_responseFilter =
    m_responseFilterStubFactory.getStub();

  private final LoggerStubFactory m_loggerStubFactory =
    new LoggerStubFactory();
  private final Logger m_logger = m_loggerStubFactory.getLogger();
  private int m_localPort;

  protected void setUp() throws Exception {
    final ServerSocket serverSocket = new ServerSocket(0);
    m_localPort = serverSocket.getLocalPort();
    serverSocket.close();
  }

  public void testBadLocalPort() throws Exception {
    final ConnectionDetails badConnectionDetails =
      new ConnectionDetails(new EndPoint("unknownhost", 111),
                            new EndPoint("to", 222),
                            false);

    try {
      new PortForwarderTCPProxyEngine(m_requestFilter,
                                      m_responseFilter,
                                      m_logger,
                                      badConnectionDetails,
                                      false,
                                      1000);
      fail("Expected UnknownHostException");
    }
    catch (UnknownHostException e) {
    }
  }

  public void testTimeOut() throws Exception {

    final ConnectionDetails connectionDetails =
      new ConnectionDetails(new EndPoint("localhost", m_localPort),
                            new EndPoint("wherever", 9999),
                            false);

    final TCPProxyEngine engine =
      new PortForwarderTCPProxyEngine(m_requestFilter,
                                      m_responseFilter,
                                      m_logger,
                                      connectionDetails,
                                      false,
                                      10);

    m_loggerStubFactory.resetCallHistory();

    // If this ends up spinning its probably because
    // some other test has not terminated all of its filter
    // threads correctly.
    engine.run();

    m_loggerStubFactory.assertSuccess("error", "Listen time out");

    m_loggerStubFactory.assertNoMoreCalls();
  }

  private void engineTests(AbstractTCPProxyEngine engine,
                           ConnectionDetails connectionDetails)
    throws Exception {

    final Thread engineThread = new Thread(engine, "Run engine");
    engineThread.start();

    final Socket clientSocket =
      new Socket(engine.getListenEndPoint().getHost(),
                 engine.getListenEndPoint().getPort());

    final OutputStreamWriter outputStreamWriter =
      new OutputStreamWriter(clientSocket.getOutputStream());
    
    final PrintWriter clientWriter =
      new PrintWriter(outputStreamWriter, true);

    final String message =
      "This is some stuff\r\nWe expect to be echoed.\u00ff\u00fe";
    clientWriter.print(message);
    clientWriter.flush();

    final InputStream clientInputStream = clientSocket.getInputStream();

    while (clientInputStream.available() <= 0) {
      Thread.sleep(10);
    }

    final ByteArrayOutputStream response = new ByteArrayOutputStream();

    // Don't use a StreamCopier because it will block reading the
    // input stream.
    final byte[] buffer = new byte[100];

    while (clientInputStream.available() > 0) {
      final int bytesRead = clientInputStream.read(buffer, 0, buffer.length);
      response.write(buffer, 0, bytesRead);
    }

    // Not sure why, but on some JVMs, this fails if we use BAOS.toString().
    // Why should the default encoding used by OSW differ from that used
    // by BAOS?
    assertEquals(message, response.toString(outputStreamWriter.getEncoding()));

    clientSocket.close();

    engine.stop();
    engineThread.join();

    final CallData callData =
      m_requestFilterStubFactory.assertSuccess("connectionOpened",
                                               ConnectionDetails.class);

    // Check the remote endpoint and isSecure of the connection details matches
    // those of our remote endpoint.
    final ConnectionDetails localConnectionDetails =
      (ConnectionDetails)callData.getParameters()[0];

    assertEquals(connectionDetails.getRemoteEndPoint(),
      localConnectionDetails.getRemoteEndPoint());
    assertEquals(connectionDetails.isSecure(),
      localConnectionDetails.isSecure());

    m_requestFilterStubFactory.assertSuccess("handle",
                                             ConnectionDetails.class,
                                             new byte[0].getClass(),
                                             Integer.class);
    m_requestFilterStubFactory.assertSuccess("connectionClosed",
                                             ConnectionDetails.class);
    m_requestFilterStubFactory.assertNoMoreCalls();

    m_responseFilterStubFactory.assertSuccess("connectionOpened",
                                             ConnectionDetails.class);
    m_responseFilterStubFactory.assertSuccess("handle",
                                             ConnectionDetails.class,
                                             new byte[0].getClass(),
                                             Integer.class);

    m_responseFilterStubFactory.setIgnoreCallOrder(true);

    m_responseFilterStubFactory.assertSuccess(
      "connectionClosed", ConnectionDetails.class);
    m_responseFilterStubFactory.assertNoMoreCalls();

    m_loggerStubFactory.assertNoMoreCalls();

    // Stopping engine or filter again doesn't do anything.
    engine.stop();

    m_requestFilterStubFactory.assertNoMoreCalls();
    m_responseFilterStubFactory.assertNoMoreCalls();
  }

  public void testEngine() throws Exception {

    final AcceptSingleConnectionAndEcho echoer =
      new AcceptSingleConnectionAndEcho();

    final EndPoint localEndPoint = new EndPoint("localhost", m_localPort);

    final ConnectionDetails connectionDetails =
      new ConnectionDetails(localEndPoint,
                            echoer.getEndPoint(),
                            false);

    // Set the filters not to randomly generate output.
    m_requestFilterStubFactory.setResult(null);
    m_responseFilterStubFactory.setResult(null);

    final AbstractTCPProxyEngine engine =
      new PortForwarderTCPProxyEngine(m_requestFilter,
                                      m_responseFilter,
                                      m_logger,
                                      connectionDetails,
                                      false,
                                      100000);

    m_responseFilterStubFactory.assertNoMoreCalls();
    m_requestFilterStubFactory.assertNoMoreCalls();

    assertEquals(localEndPoint, engine.getListenEndPoint());
    assertNotNull(engine.getSocketFactory());
    m_requestFilterStubFactory.assertIsWrappedBy(engine.getRequestFilter());
    m_responseFilterStubFactory.assertIsWrappedBy(engine.getResponseFilter());
    assertEquals("", engine.getRequestColour());
    assertEquals("", engine.getResponseColour());

    m_loggerStubFactory.resetCallHistory();
    m_requestFilterStubFactory.resetCallHistory();
    m_responseFilterStubFactory.resetCallHistory();

    engineTests(engine, connectionDetails);
  }

  public void testColourEngine() throws Exception {

    final AcceptSingleConnectionAndEcho echoer =
      new AcceptSingleConnectionAndEcho();

    final EndPoint localEndPoint = new EndPoint("localhost", m_localPort);

    final ConnectionDetails connectionDetails =
      new ConnectionDetails(localEndPoint,
                            echoer.getEndPoint(),
                            true);

    // Set the filters not to randomly generate output.
    m_requestFilterStubFactory.setResult(null);
    m_responseFilterStubFactory.setResult(null);

    final AbstractTCPProxyEngine engine =
      new PortForwarderTCPProxyEngine(m_requestFilter,
                                      m_responseFilter,
                                      m_logger,
                                      connectionDetails,
                                      true,
                                      100000);

    m_responseFilterStubFactory.assertNoMoreCalls();
    m_requestFilterStubFactory.assertNoMoreCalls();

    assertEquals(localEndPoint, engine.getListenEndPoint());
    assertNotNull(engine.getSocketFactory());
    m_requestFilterStubFactory.assertIsWrappedBy(engine.getRequestFilter());
    m_responseFilterStubFactory.assertIsWrappedBy(engine.getResponseFilter());
    assertEquals(TerminalColour.RED, engine.getRequestColour());
    assertEquals(TerminalColour.BLUE, engine.getResponseColour());

    m_loggerStubFactory.resetCallHistory();
    m_requestFilterStubFactory.resetCallHistory();
    m_responseFilterStubFactory.resetCallHistory();

    engineTests(engine, connectionDetails);
  }

  public void testOutputStreamFilterTeeWithBadFilters() throws Exception {

    final EndPoint localEndPoint = new EndPoint("localhost", m_localPort);

    final ConnectionDetails connectionDetails =
      new ConnectionDetails(localEndPoint,
                            new EndPoint("bah", 456),
                            false);

    final AbstractTCPProxyEngine engine =
      new PortForwarderTCPProxyEngine(m_requestFilter,
                                      m_responseFilter,
                                      m_logger,
                                      connectionDetails,
                                      true,
                                      100000);

    final AbstractTCPProxyEngine.OutputStreamFilterTee filterTee =
      engine.new OutputStreamFilterTee(connectionDetails,
                                       new ByteArrayOutputStream(),
                                       new BadFilter(),
                                       "");

    m_loggerStubFactory.resetCallHistory();

    filterTee.connectionOpened();
    m_loggerStubFactory.assertSuccess("getErrorLogWriter");
    m_loggerStubFactory.assertNoMoreCalls();

    filterTee.connectionClosed();
    m_loggerStubFactory.assertSuccess("getErrorLogWriter");
    m_loggerStubFactory.assertNoMoreCalls();

    filterTee.handle(new byte[0], 0);
    m_loggerStubFactory.assertSuccess("getErrorLogWriter");
    m_loggerStubFactory.assertNoMoreCalls();
  }

  private static final class BadFilter implements TCPProxyFilter {

    public byte[] handle(ConnectionDetails connectionDetails,
                         byte[] buffer,
                         int bytesRead)
      throws FilterException {
      throw new FilterException("Problem", null);
    }

    public void connectionOpened(ConnectionDetails connectionDetails)
      throws FilterException {
      throw new FilterException("Problem", null);
    }

    public void connectionClosed(ConnectionDetails connectionDetails)
      throws FilterException {
      throw new FilterException("Problem", null);
    }
  }

  private static final class AcceptSingleConnectionAndEcho implements Runnable {
    private final ServerSocket m_serverSocket;

    public AcceptSingleConnectionAndEcho() throws IOException {
      m_serverSocket = new ServerSocket(0);
      new Thread(this, getClass().getName()).start();
    }

    public EndPoint getEndPoint() {
      return EndPoint.serverEndPoint(m_serverSocket);
    }

    public void run() {
      try {
        final Socket socket = m_serverSocket.accept();

        new StreamCopier(1000, true).copy(socket.getInputStream(),
                                          socket.getOutputStream());
      }
      catch (IOException e) {
        System.err.println("Got a " + e.getMessage());
      }
    }
  }
}
