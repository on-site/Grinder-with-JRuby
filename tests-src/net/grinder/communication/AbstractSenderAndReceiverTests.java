// Copyright (C) 2000, 2001, 2002, 2003, 2004, 2005 Philip Aston
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

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.net.ServerSocket;

import junit.framework.TestCase;


/**
 *  Abstract unit test cases for <code>Sender</code> and
 *  <code>Receiver</code> implementations..
 *
 * @author Philip Aston
 * @version $Revision: 3762 $
 */
public abstract class AbstractSenderAndReceiverTests extends TestCase {

  private ConnectionType m_connectionType;
  private Acceptor m_acceptor;
  private Connector m_connector;

  protected Receiver m_receiver;
  protected Sender m_sender;

  private ExecuteThread m_executeThread;

  public AbstractSenderAndReceiverTests(String name)
    throws Exception {
    super(name);
  }

  private final void initialiseSockets() throws Exception {

    if (m_connector == null) {
      // Find a free port.
      final ServerSocket socket = new ServerSocket(0);
      final int port = socket.getLocalPort();
      socket.close();

      m_connectionType = ConnectionType.AGENT;
      m_connector = new Connector("localhost", port, m_connectionType);
      m_acceptor = new Acceptor("localhost", port, 1);
    }
  }

  protected final Acceptor getAcceptor() throws Exception {
    initialiseSockets();
    return m_acceptor;
  }

  protected final ConnectionType getConnectionType() throws Exception {
    initialiseSockets();
    return m_connectionType;
  }

  protected final Connector getConnector() throws Exception {
    initialiseSockets();
    return m_connector;
  }

  protected void setUp() throws Exception {
    m_executeThread = new ExecuteThread();
  }

  protected void tearDown() throws Exception {
    m_executeThread.shutdown();

    if (m_acceptor != null) {
      m_acceptor.shutdown();
    }
  }


  public void testSendSimpleMessage() throws Exception {

    final SimpleMessage sentMessage = new SimpleMessage();
    m_sender.send(sentMessage);

    final Message receivedMessage = m_executeThread.waitForMessage();
    assertEquals(sentMessage, receivedMessage);
    assertTrue(sentMessage != receivedMessage);
  }

  public void testSendManyMessages() throws Exception {

    for (int i=1; i<=10; ++i) {
      final SimpleMessage[] sentMessages = new SimpleMessage[i];

      for (int j=0; j<i; ++j) {
        sentMessages[j] = new SimpleMessage(i);
        m_sender.send(sentMessages[j]);
      }

      for (int j=0; j<i; ++j) {
        final SimpleMessage receivedMessage =
          (SimpleMessage) m_executeThread.waitForMessage();

        assertEquals(sentMessages[j], receivedMessage);
        assertTrue(sentMessages[j] != receivedMessage);
      }
    }
  }

  public void testSendLargeMessage() throws Exception {
    // This causes a message size of about 38K. Should be limited by
    // the buffer size in Receiver.
    final SimpleMessage sentMessage = new SimpleMessage(8000);
    m_sender.send(sentMessage);

    final SimpleMessage receivedMessage =
      (SimpleMessage) m_executeThread.waitForMessage();

    assertEquals(sentMessage, receivedMessage);
    assertTrue(sentMessage != receivedMessage);
  }

  public void testShutdownReceiver() throws Exception {
    m_receiver.shutdown();
    assertNull(m_executeThread.waitForMessage());
  }

  public void testQueueAndFlush() throws Exception {

    final QueuedSender sender = new QueuedSenderDecorator(m_sender);

    final SimpleMessage[] messages = new SimpleMessage[25];

    for (int i=0; i<messages.length; ++i) {
      messages[i] = new SimpleMessage();
      sender.queue(messages[i]);
    }

    sender.flush();

    for (int i=0; i<messages.length; ++i) {
      final Message receivedMessage = m_executeThread.waitForMessage();

      assertEquals(messages[i], receivedMessage);
      assertTrue(messages[i] != receivedMessage);
    }
  }

  public void testQueueAndSend() throws Exception {

    final QueuedSender sender = new QueuedSenderDecorator(m_sender);

    final SimpleMessage[] messages = new SimpleMessage[25];

    for (int i=0; i<messages.length; ++i) {
      messages[i] = new SimpleMessage();
      sender.queue(messages[i]);
    }

    final SimpleMessage finalMessage = new SimpleMessage();
    sender.send(finalMessage);

    for (int i=0; i<messages.length; ++i) {
      final Message receivedMessage = m_executeThread.waitForMessage();

      assertEquals(messages[i], receivedMessage);
      assertTrue(messages[i] != receivedMessage);
    }

    final Message receivedFinalMessage = m_executeThread.waitForMessage();

    assertEquals(finalMessage, receivedFinalMessage);
    assertTrue(finalMessage != receivedFinalMessage);
  }

  /**
   * Pico-kernel! Need a long running thread because of the half-baked
   * PipedInputStream/PipedOutputStream thread checking.
   */
  private final class ExecuteThread extends Thread {

    private Action m_action;

    public ExecuteThread() {
      super("ExecuteThread");
      start();
    }

    public synchronized void run() {

      try {
        while (true) {
          while (m_action == null) {
            wait();
          }

          m_action.run();
          m_action = null;

          notifyAll();
        }
      }
      catch (InterruptedException e) {
      }
    }

    private synchronized Object execute(Action action) throws Exception {

      m_action = action;
      notifyAll();

      while (!action.getHasRun()) {
        wait();
      }

      return action.getResult();
    }

    public Message waitForMessage() throws Exception {
      return (Message) execute(
        new Action() {
          public Object doAction() throws Exception {
            return m_receiver.waitForMessage();
          }
        }
        );
    }

    public void shutdown() throws Exception {
      execute(
        new Action() {
          public Object doAction() throws Exception {
            throw new InterruptedException();
          }
        }
        );
    }

    private abstract class Action {

      private Object m_result;
      private Exception m_exception;
      private boolean m_hasRun = false;

      public void run() throws InterruptedException {
        try {
          m_result = doAction();
        }
        catch (InterruptedException e) {
          throw e;
        }
        catch (Exception e) {
          m_exception = e;
        }
        finally {
          m_hasRun = true;
        }
      }

      public Object getResult() throws Exception {
        if (m_exception != null) {
          throw m_exception;
        }

        return m_result;
      }

      public boolean getHasRun() {
        return m_hasRun;
      }

      protected abstract Object doAction() throws Exception;
    }
  }

  static final class BigBufferPipedInputStream extends PipedInputStream {
    public BigBufferPipedInputStream(PipedOutputStream src)
      throws IOException {
      super(src);
      // JDK, I laugh at your puny buffer.
      buffer = new byte[32768];
    }
  }
}
