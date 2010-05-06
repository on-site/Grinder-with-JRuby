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

package net.grinder.communication;

import junit.framework.TestCase;

import net.grinder.testutility.RandomStubFactory;


/**
 *  Unit test case for <code>TeeSender</code>.
 *
 * @author Philip Aston
 * @version $Revision: 4006 $
 */
public class TestTeeSender extends TestCase {

  public void testWithGoodSenders() throws Exception {

    final RandomStubFactory<Sender> sender1StubFactory =
      RandomStubFactory.create(Sender.class);

    final RandomStubFactory<Sender> sender2StubFactory =
      RandomStubFactory.create(Sender.class);
    final TeeSender teeSender = new TeeSender(sender1StubFactory.getStub(),
                                              sender2StubFactory.getStub());

    final Message m1 = new SimpleMessage();
    final Message m2 = new SimpleMessage();

    teeSender.send(m1);
    teeSender.send(m2);
    teeSender.send(m2);
    teeSender.shutdown();

    sender1StubFactory.assertSuccess("send", m1);
    sender1StubFactory.assertSuccess("send", m2);
    sender1StubFactory.assertSuccess("send", m2);
    sender1StubFactory.assertSuccess("shutdown");
    sender1StubFactory.assertNoMoreCalls();

    sender2StubFactory.assertSuccess("send", m1);
    sender2StubFactory.assertSuccess("send", m2);
    sender2StubFactory.assertSuccess("send", m2);
    sender2StubFactory.assertSuccess("shutdown");
    sender2StubFactory.assertNoMoreCalls();
  }

  final static class BadSender implements Sender {

    final CommunicationException m_exceptionToThrowFromSend;
    final RuntimeException m_exceptionToThrowFromShutdown;

    public BadSender(CommunicationException exceptionToThrowFromSend,
                     RuntimeException exceptionToThrowFromShutdown) {
      m_exceptionToThrowFromSend = exceptionToThrowFromSend;
      m_exceptionToThrowFromShutdown = exceptionToThrowFromShutdown;
    }

    public void send(Message message) throws CommunicationException {
      throw m_exceptionToThrowFromSend;
    }

    public void shutdown() {
      throw m_exceptionToThrowFromShutdown;
    }
  }

  public void testWithABadSender() throws Exception {

    final RandomStubFactory<Sender> goodSenderStubFactory =
      RandomStubFactory.create(Sender.class);
    final Sender goodSender = goodSenderStubFactory.getStub();

    final CommunicationException exceptionToThrowFromSend =
      new CommunicationException("Foo");

    final RuntimeException exceptionToThrowFromShutdown =
      new RuntimeException();

    final Sender badSender =
      new BadSender(exceptionToThrowFromSend, exceptionToThrowFromShutdown);

    // goodSender is first, so should be invoked before badSender fails.
    final TeeSender teeSender1 = new TeeSender(goodSender, badSender);

    final Message m = new SimpleMessage();

    try {
      teeSender1.send(m);
      fail("Expected CommunicationException");
    }
    catch (CommunicationException e) {
      assertSame(exceptionToThrowFromSend, e);
    }

    goodSenderStubFactory.assertSuccess("send", m);
    goodSenderStubFactory.assertNoMoreCalls();

    try {
      teeSender1.shutdown();
      fail("Expected RuntimeException");
    }
    catch (RuntimeException e) {
      assertSame(exceptionToThrowFromShutdown, e);
    }

    goodSenderStubFactory.assertSuccess("shutdown");
    goodSenderStubFactory.assertNoMoreCalls();

    // goodSender is second, so will never be invoked.
    final TeeSender teeSender2 = new TeeSender(badSender, goodSender);

    try {
      teeSender2.send(m);
      fail("Expected CommunicationException");
    }
    catch (CommunicationException e) {
      assertSame(exceptionToThrowFromSend, e);
    }

    goodSenderStubFactory.assertNoMoreCalls();

    try {
      teeSender2.shutdown();
      fail("Expected RuntimeException");
    }
    catch (RuntimeException e) {
      assertSame(exceptionToThrowFromShutdown, e);
    }

    goodSenderStubFactory.assertNoMoreCalls();
  }
}
