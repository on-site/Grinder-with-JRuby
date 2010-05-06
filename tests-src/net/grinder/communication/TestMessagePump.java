// Copyright (C) 2003 - 2010 Philip Aston
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
import java.io.PipedOutputStream;


/**
 *  Unit tests for <code>MessagePump</code>.
 *
 * @author Philip Aston
 * @version $Revision: 4234 $
 */
public class TestMessagePump extends AbstractSenderAndReceiverTests {

  public TestMessagePump(String name) throws Exception {
    super(name);
  }

  private MessagePump m_messagePump;
  private Sender m_intermediateSender;
  private Receiver m_intermediateReceiver;


  /**
   * Sigh, JUnit treats setUp and tearDown as non-virtual methods -
   * must define in concrete test case class.
   */
  protected void setUp() throws Exception {
    super.setUp();

    // m_sender -> m_intermediateReceiver -> messagePump
    // -> m_intermediateSender -> m_receiver

    final PipedOutputStream intermediateSenderOutputStream =
      new PipedOutputStream();
    m_intermediateSender = new StreamSender(intermediateSenderOutputStream);

    final InputStream receiverInputStream =
      new BigBufferPipedInputStream(intermediateSenderOutputStream);
    m_receiver = new StreamReceiver(receiverInputStream);

    final PipedOutputStream senderOutputStream = new PipedOutputStream();
    m_sender = new StreamSender(senderOutputStream);

    final InputStream intermediateReceiverInputStream =
      new BigBufferPipedInputStream(senderOutputStream);
    m_intermediateReceiver =
      new StreamReceiver(intermediateReceiverInputStream);

    m_messagePump =
      new MessagePump(m_intermediateReceiver, m_intermediateSender, 1);

    m_messagePump.start();
  }

  protected void tearDown() throws Exception {
    super.tearDown();

    m_messagePump.shutdown();

    m_receiver.shutdown();
    m_sender.shutdown();
  }

  public void testShutdownOnNullMessage() throws Exception {
    m_sender.send(null);
    assertEquals(null, m_intermediateReceiver.waitForMessage());
  }

  public void testShutdownIfReceiverShutdown() throws Exception {
    m_sender.shutdown();
    assertEquals(null, m_intermediateReceiver.waitForMessage());
    assertEquals(null, m_receiver.waitForMessage());

  }
}

