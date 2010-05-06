// Copyright (C) 2003, 2004, 2005, 2006, 2007 Philip Aston
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

import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;


/**
 * Active acceptor that accepts a single connection.
 */
public final class SocketAcceptorThread extends Thread {

  private final ServerSocket m_serverSocket;
  private final int m_numberOfAccepts;
  private Exception m_exception;
  private Socket m_acceptedSocket;

  public SocketAcceptorThread() throws Exception {
    this(1);
  }

  public SocketAcceptorThread(int numberOfAccepts) throws Exception {
    m_serverSocket = new ServerSocket(0);
    m_numberOfAccepts = numberOfAccepts;
    start();
  }

  public void run() {
    try {
      for (int i=0; i<m_numberOfAccepts; ++i) {
        m_acceptedSocket = m_serverSocket.accept();
      }
    }
    catch (Exception e) {
      m_exception = e;
    }
  }

  public String getHostName() throws UnknownHostException {
    return InetAddress.getByName(null).getHostName();
  }

  public int getPort() {
    return m_serverSocket.getLocalPort();
  }

  public Socket getAcceptedSocket() {
    return m_acceptedSocket;
  }

  public final void close() throws Exception {

    join();

    if (m_exception != null) {
      throw m_exception;
    }

    m_serverSocket.close();
  }
}
