// Copyright (C) 2000 - 2006 Philip Aston
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

import java.util.Random;


/**
 *  Unit tests for <code>ClientSender</code> and
 *  <code>ServerReceiver</code>.
 *
 * @author Philip Aston
 * @version $Revision: 3762 $
 */
public class TestClientSenderAndServerReceiver
  extends AbstractSenderAndReceiverTests {

  private final static Random s_random = new Random();

  public TestClientSenderAndServerReceiver(String name) throws Exception {
    super(name);
  }

  private Receiver createReceiver() throws Exception {
    final ServerReceiver receiver = new ServerReceiver();
    receiver.receiveFrom(
      getAcceptor(), new ConnectionType[] { getConnectionType() }, 3, 10);
    return receiver;
  }

  private Sender createSender() throws Exception {
    return ClientSender.connect(getConnector());
  }

  /**
   * Sigh, JUnit treats setUp and tearDown as non-virtual methods -
   * must define in concrete test case class.
   */
  protected void setUp() throws Exception {
    super.setUp();

    m_receiver = createReceiver();
    m_sender = createSender();
  }

  protected void tearDown() throws Exception {
    super.tearDown();

    m_receiver.shutdown();
    m_sender.shutdown();
  }

  static int s_numberOfMessages = 0;

  private class SenderThread extends Thread {
    public void run() {
      try {
        final Sender m_sender = createSender();

        final int n = s_random.nextInt(10);

        for (int i=0; i<n; ++i) {
          m_sender.send(new SimpleMessage(1));
          sleep(s_random.nextInt(30));
        }

        synchronized(Sender.class) {
          s_numberOfMessages += n;
        }

        m_sender.shutdown();
      }
      catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  public void testManySenders() throws Exception {
    s_numberOfMessages = 0;

    final Thread[] senderThreads = new Thread[5];

    for (int i=0; i<senderThreads.length; ++i) {
      senderThreads[i] = new SenderThread();
      senderThreads[i].start();
    }

    for (int i=0; i<senderThreads.length; ++i) {
      senderThreads[i].join();
    }

    for (int i=0; i<s_numberOfMessages; ++i) {
      m_receiver.waitForMessage();
    }
  }
}
