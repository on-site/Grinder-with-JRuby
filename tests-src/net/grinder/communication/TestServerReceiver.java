// Copyright (C) 2003 - 2008 Philip Aston
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

package net.grinder.communication;

import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;

import net.grinder.common.UncheckedInterruptedException;
import net.grinder.testutility.IsolatedObjectFactory;

import junit.framework.TestCase;


/**
 *  Unit tests for <code>ServerReceiver</code>.
 *
 * @author Philip Aston
 * @version $Revision: 3824 $
 */
public class TestServerReceiver extends TestCase {

  public TestServerReceiver(String name) {
    super(name);
  }

  public void testConstructor() throws Exception {

    final Acceptor acceptor = new Acceptor("localhost", 0, 1);

    final ServerReceiver serverReceiver = new ServerReceiver();

    // No op.
    serverReceiver.receiveFrom(acceptor, new ConnectionType[0], 1, 2);

    serverReceiver.receiveFrom(
      acceptor, new ConnectionType[] { ConnectionType.AGENT }, 3, 10);

    serverReceiver.shutdown();
    acceptor.shutdown();
  }

  public void testWaitForMessage() throws Exception {

    final Acceptor acceptor = new Acceptor("localhost", 0, 1);

    final ServerReceiver serverReceiver = new ServerReceiver();
    serverReceiver.receiveFrom(
      acceptor, new ConnectionType[] { ConnectionType.AGENT }, 3, 10);

    final Socket[] socket = new Socket[5];

    for (int i=0; i<socket.length; ++i) {
      socket[i] =
        new Connector(InetAddress.getByName(null).getHostName(),
                      acceptor.getPort(),
                      ConnectionType.AGENT)
        .connect();
    }

    // Sleep until we've accepted all connections. Give up after a few
    // seconds.
    final ResourcePool socketSet =
      acceptor.getSocketSet(ConnectionType.AGENT);

    for (int i=0; socketSet.countActive() != 5 && i<10; ++i) {
      Thread.sleep(i * i * 10);
    }

    final SimpleMessage message1 = new SimpleMessage();
    final SimpleMessage message2 = new SimpleMessage();
    final SimpleMessage message3 = new SimpleMessage();

    final ObjectOutputStream objectStream1 =
      new ObjectOutputStream(socket[0].getOutputStream());
    objectStream1.writeObject(message1);
    objectStream1.flush();

    final ObjectOutputStream objectStream2 =
      new ObjectOutputStream(socket[1].getOutputStream());
    objectStream2.writeObject(message2);
    objectStream2.flush();

    final ObjectOutputStream objectStream3 =
      new ObjectOutputStream(socket[0].getOutputStream());
    objectStream3.writeObject(message3);
    objectStream3.flush();

    Message receivedMessage1 = serverReceiver.waitForMessage();
    Message receivedMessage2 = serverReceiver.waitForMessage();
    Message receivedMessage3 = serverReceiver.waitForMessage();

    assertEquals(
      UncheckedInterruptedException.class,
      new BlockingActionThread() {
        protected void blockingAction() throws CommunicationException {
          serverReceiver.waitForMessage();
        }
      }.getException().getClass());

    if (receivedMessage1.equals(message2)) {
      final Message temp = receivedMessage2;
      receivedMessage2 = receivedMessage1;
      receivedMessage1 = temp;
    }
    else if (receivedMessage3.equals(message2)) {
      final Message temp = receivedMessage3;
      receivedMessage3 = receivedMessage2;
      receivedMessage2 = temp;
    }
    else {
      assertEquals(message2, receivedMessage2);
    }

    assertEquals(message1, receivedMessage1);
    assertEquals(message2, receivedMessage2);
    assertEquals(message3, receivedMessage3);

    serverReceiver.shutdown();
    acceptor.shutdown();
  }

  public void testWaitForBadMessage() throws Exception {

    final Acceptor acceptor = new Acceptor("localhost", 0, 1);

    final ServerReceiver serverReceiver = new ServerReceiver();
    serverReceiver.receiveFrom(
      acceptor, new ConnectionType[] { ConnectionType.AGENT }, 3, 10);

    final Socket socket =
      new Connector(InetAddress.getByName(null).getHostName(),
                    acceptor.getPort(),
                    ConnectionType.AGENT)
      .connect();

    // Sleep until we've accepted the connections. Give up after a few
    // seconds.
    final ResourcePool socketSet = acceptor.getSocketSet(ConnectionType.AGENT);

    for (int i=0; socketSet.countActive() != 1 && i<10; ++i) {
      Thread.sleep(i * i * 10);
    }

    // Message that we can't read using the standard class loaders.
    final SimpleMessage message = new SimpleMessage();
    message.setPayload(IsolatedObjectFactory.getIsolatedObject());

    final ObjectOutputStream objectStream =
      new ObjectOutputStream(socket.getOutputStream());
    objectStream.writeObject(message);
    objectStream.flush();

    try {
      serverReceiver.waitForMessage();
      fail("Expected CommunicationException");
    }
    catch (CommunicationException e) {
    }

    serverReceiver.shutdown();
    acceptor.shutdown();
  }

  public void testShutdown() throws Exception {

    final Acceptor acceptor = new Acceptor("localhost", 0, 1);

    final ServerReceiver serverReceiver = new ServerReceiver();
    serverReceiver.receiveFrom(
      acceptor, new ConnectionType[] { ConnectionType.AGENT }, 3, 10);

    assertEquals(1, acceptor.getThreadGroup().activeCount());
    assertEquals(3, serverReceiver.getActveThreadCount());

    final Socket socket =
      new Connector(InetAddress.getByName(null).getHostName(),
                    acceptor.getPort(),
                    ConnectionType.AGENT)
      .connect();

    // Sleep until we've accepted the connection. Give up after a few
    // seconds.
    final ResourcePool socketSet =
      acceptor.getSocketSet(ConnectionType.AGENT);

    for (int i=0; socketSet.countActive() != 1 && i<10; ++i) {
      Thread.sleep(i * i * 10);
    }

    final SimpleMessage message = new SimpleMessage();

    final ObjectOutputStream objectStream =
      new ObjectOutputStream(socket.getOutputStream());
    objectStream.writeObject(message);
    objectStream.flush();

    final Message receivedMessage = serverReceiver.waitForMessage();
    assertNotNull(receivedMessage);

    serverReceiver.shutdown();

    try {
      serverReceiver.receiveFrom(
        acceptor, new ConnectionType[] { ConnectionType.AGENT }, 3, 10);
      fail("Expected a CommunicationException");
    }
    catch (CommunicationException e) {
    }

    assertNull(serverReceiver.waitForMessage());

    acceptor.shutdown();
  }

  public void testCloseCommunicationMessage() throws Exception {

    final Acceptor acceptor = new Acceptor("localhost", 0, 1);

    final ServerReceiver serverReceiver = new ServerReceiver();
    serverReceiver.receiveFrom(
      acceptor, new ConnectionType[] { ConnectionType.AGENT }, 5, 10);

    assertEquals(1, acceptor.getThreadGroup().activeCount());
    assertEquals(5, serverReceiver.getActveThreadCount());

    final Socket socket =
      new Connector(InetAddress.getByName(null).getHostName(),
                    acceptor.getPort(),
                    ConnectionType.AGENT)
      .connect();

    // Sleep until we've accepted the connection. Give up after a few
    // seconds.
    final ResourcePool socketSet =
      acceptor.getSocketSet(ConnectionType.AGENT);

    for (int i=0; socketSet.countActive() != 1 && i<10; ++i) {
      Thread.sleep(i * i * 10);
    }

    final SimpleMessage message = new SimpleMessage();

    final ObjectOutputStream objectStream1 =
      new ObjectOutputStream(socket.getOutputStream());
    objectStream1.writeObject(message);
    objectStream1.flush();

    final Message receivedMessage = serverReceiver.waitForMessage();
    assertNotNull(receivedMessage);

    final Message closeCommunicationMessage = new CloseCommunicationMessage();

    final ObjectOutputStream objectStream2 =
      new ObjectOutputStream(socket.getOutputStream());
    objectStream2.writeObject(closeCommunicationMessage);
    objectStream2.flush();

    // For a ServerReceiver, a CloseCommunicationMessage only closes
    // the individual connection.
    assertEquals(
      UncheckedInterruptedException.class,
      new BlockingActionThread() {
        protected void blockingAction() throws CommunicationException {
          serverReceiver.waitForMessage();
        }
      }.getException().getClass());

    serverReceiver.shutdown();

    acceptor.shutdown();
  }

  public void testWithResponseSender() throws Exception {
    final Acceptor acceptor = new Acceptor("localhost", 0, 1);

    final ServerReceiver serverReceiver = new ServerReceiver();
    serverReceiver.receiveFrom(
      acceptor, new ConnectionType[] { ConnectionType.AGENT }, 3, 10);

    final Socket socket =
      new Connector(InetAddress.getByName(null).getHostName(),
                    acceptor.getPort(),
                    ConnectionType.AGENT)
      .connect();

    // Sleep until we've accepted the connections. Give up after a few
    // seconds.
    final ResourcePool socketSet = acceptor.getSocketSet(ConnectionType.AGENT);

    for (int i=0; socketSet.countActive() != 1 && i<10; ++i) {
      Thread.sleep(i * i * 10);
    }

    // This bit is what package internal code would do to send a message
    // on behalf of the client.
    final SimpleMessage message = new SimpleMessage();
    final MessageRequiringResponse responseSender = new MessageRequiringResponse(message);

    final ObjectOutputStream objectStream =
      new ObjectOutputStream(socket.getOutputStream());
    objectStream.writeObject(responseSender);
    objectStream.flush();

    // The server side app code can get hold of the original message...
    final Message received = serverReceiver.waitForMessage();
    assertTrue(received instanceof MessageRequiringResponse);
    final MessageRequiringResponse receivedResponseSender = (MessageRequiringResponse)received;
    assertTrue(receivedResponseSender.getMessage() instanceof SimpleMessage);

    // ...and send a response.
    final SimpleMessage responseMessage = new SimpleMessage();
    receivedResponseSender.sendResponse(responseMessage);

    // Which the end client can get hold of.
    final StreamReceiver receiver = new StreamReceiver(socket.getInputStream());
    final Message clientMessage = receiver.waitForMessage();
    assertEquals(responseMessage, clientMessage);

    serverReceiver.shutdown();
    acceptor.shutdown();
    receiver.shutdown();
  }
}
