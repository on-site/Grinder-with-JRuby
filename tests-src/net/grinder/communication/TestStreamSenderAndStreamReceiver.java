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

import java.io.InputStream;
import java.io.PipedOutputStream;


/**
 *  Unit tests for <code>StreamSender</code> and
 *  <code>StreamReceiver</code>.
 *
 * @author Philip Aston
 * @version $Revision: 3762 $
 */
public class TestStreamSenderAndStreamReceiver
  extends AbstractSenderAndReceiverTests {

  public TestStreamSenderAndStreamReceiver(String name) throws Exception {
    super(name);
  }

  /**
   * Sigh, JUnit treats setUp and tearDown as non-virtual methods -
   * must define in concrete test case class.
   */
  protected void setUp() throws Exception {
    super.setUp();

    final PipedOutputStream outputStream = new PipedOutputStream();
    final InputStream inputStream = new BigBufferPipedInputStream(outputStream);

    m_receiver = new StreamReceiver(inputStream);
    m_sender = new StreamSender(outputStream);
  }

  protected void tearDown() throws Exception {
    super.tearDown();

    m_receiver.shutdown();
    m_sender.shutdown();
  }
}
