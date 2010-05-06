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

package net.grinder.console.client;

import java.io.InputStream;
import java.io.ObjectInputStream;

import junit.framework.TestCase;

import net.grinder.communication.SocketAcceptorThread;


/**
 * Unit tests for {@link ConsoleConnectionFactory}.
*
 * @author Philip Aston
 * @version $Revision: 3995 $
 */
public class TestConsoleConnectionFactory extends TestCase {

  public void testConnection() throws Exception {
    final ConsoleConnectionFactory consoleConnectionFactory =
      new ConsoleConnectionFactory();

    final SocketAcceptorThread socketAcceptor = new SocketAcceptorThread();

    final ConsoleConnection consoleConnection =
      consoleConnectionFactory.connect(
        socketAcceptor.getHostName(), socketAcceptor.getPort());

    assertNotNull(consoleConnection);

    socketAcceptor.join();

    final InputStream socketInput =
      socketAcceptor.getAcceptedSocket().getInputStream();

    assertEquals(2, socketInput.read()); // ConnectionType.CONSOLE_CLIENT

    assertNull(new ObjectInputStream(socketInput).readObject());

    assertEquals(0, socketInput.available());

    socketAcceptor.close();

    try {
      consoleConnectionFactory.connect(
        socketAcceptor.getHostName(), socketAcceptor.getPort());

      fail("Expected ConsoleConnectionException");
    }
    catch (ConsoleConnectionException e) {
    }
  }
}
