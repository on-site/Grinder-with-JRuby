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

import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.net.Socket;

import junit.framework.TestCase;

import net.grinder.testutility.AssertUtilities;
import net.grinder.testutility.IsolatedObjectFactory;


/**
 *  Unit test case for <code>Connector</code>.
 *
 * @author Philip Aston
 * @version $Revision: 3824 $
 */
public class TestConnector extends TestCase {

  public void testConnnect() throws Exception {
    final SocketAcceptorThread socketAcceptor = new SocketAcceptorThread();

    final Connector connector =
      new Connector(socketAcceptor.getHostName(), socketAcceptor.getPort(),
                    ConnectionType.WORKER);

    final Socket localSocket = connector.connect();

    socketAcceptor.join();

    final Socket serverSocket = socketAcceptor.getAcceptedSocket();
    final InputStream inputStream = serverSocket.getInputStream();

    assertEquals(ConnectionType.WORKER,
                 Connector.read(inputStream).getConnectionType());

    final byte[] text = "Hello".getBytes();

    localSocket.getOutputStream().write(text);

    for (int i=0; i<text.length; ++i) {
      assertEquals(text[i], inputStream.read());
    }

    socketAcceptor.close();

    try {
      connector.connect();
      fail("Expected CommunicationException");
    }
    catch (CommunicationException e) {
    }

    //For some reason, this connection is sometimes established.
    final Connector badConnector =
      new Connector("this is not a host name", 1234, ConnectionType.AGENT);

    try {
      badConnector.connect();
      fail("Expected CommunicationException");
    }
    catch (CommunicationException e) {
    }
  }

  public void testBadRead() throws Exception {
    final PipedOutputStream out = new PipedOutputStream();
    final PipedInputStream in = new PipedInputStream(out);

    out.write(99);

    try {
      Connector.read(in);
      fail("Expected CommunicationException");
    }
    catch (CommunicationException e) {
    }

    ConnectionType.WORKER.write(out);
    out.write(99);
    new ObjectOutputStream(out).writeObject(null);

    try {
      Connector.read(in);
      fail("Expected CommunicationException");
    }
    catch (CommunicationException e) {
    }

    while (in.available() > 0) {
      in.read();
    }

    ConnectionType.WORKER.write(out);
    new ObjectOutputStream(out).writeObject(
      IsolatedObjectFactory.getIsolatedObject());

    try {
      Connector.read(in);
      fail("Expected CommunicationException");
    }
    catch (CommunicationException e) {
    }
  }

  public void testEquality() throws Exception {
    final Connector connector =
      new Connector("a", 1234, ConnectionType.WORKER);

    assertEquals(connector.hashCode(), connector.hashCode());
    assertEquals(connector, connector);
    AssertUtilities.assertNotEquals(connector, null);
    AssertUtilities.assertNotEquals(connector, this);

    final Connector[] equal = {
      new Connector("a", 1234, ConnectionType.WORKER),
    };

    final Connector[] notEqual = {
      new Connector("a", 6423, ConnectionType.WORKER),
      new Connector("b", 1234, ConnectionType.WORKER),
      new Connector("a", 1234, ConnectionType.AGENT),
    };

    for (int i = 0; i < equal.length; ++i) {
      assertEquals(connector.hashCode(), equal[i].hashCode());
      assertEquals(connector, equal[i]);
    }

    for (int i = 0; i < notEqual.length; ++i) {
      AssertUtilities.assertNotEquals(connector, notEqual[i]);
    }
  }

  public void testGetEndpointAsString() throws Exception {
    assertEquals(
      "a:1234",
      new Connector("a", 1234, ConnectionType.WORKER).getEndpointAsString());

    final String description =
      new Connector("", 1234, ConnectionType.WORKER).getEndpointAsString();

    AssertUtilities.assertContains(description, "localhost");
    AssertUtilities.assertContains(description, "1234");

  }
}
