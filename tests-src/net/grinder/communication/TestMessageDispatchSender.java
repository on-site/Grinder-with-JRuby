// Copyright (C) 2005 - 2009 Philip Aston
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
import net.grinder.communication.MessageDispatchRegistry.AbstractBlockingHandler;


/**
 * Unit tests for {@link MessageDispatchSender}.
 *
 * @author Philip Aston
 * @version $Revision: 4006 $
 */
public class TestMessageDispatchSender extends TestCase {

  public void testSend() throws Exception {
    final MessageDispatchSender messageDispatchSender =
      new MessageDispatchSender();

    messageDispatchSender.send(new SimpleMessage());

    final RandomStubFactory<Sender> fallbackHandlerStubFactory =
      RandomStubFactory.create(Sender.class);
    messageDispatchSender.addFallback(fallbackHandlerStubFactory.getStub());

    final Message m1 = new SimpleMessage();
    final Message m2 = new SimpleMessage();

    messageDispatchSender.send(m1);
    messageDispatchSender.send(m2);

    fallbackHandlerStubFactory.assertSuccess("send", m1);
    fallbackHandlerStubFactory.assertSuccess("send", m2);
    fallbackHandlerStubFactory.assertNoMoreCalls();

    final HandlerSenderStubFactory handlerStubFactory =
      new HandlerSenderStubFactory();

    final Sender previousHandler = messageDispatchSender.set(
      SimpleMessage.class,
      handlerStubFactory.getStub());
    assertNull(previousHandler);

    final RandomStubFactory<Sender> otherMessagerHandlerStubFactory =
      RandomStubFactory.create(Sender.class);
    Sender previousHandler2 =
      messageDispatchSender.set(OtherMessage.class,
                                otherMessagerHandlerStubFactory.getStub());
    assertNull(previousHandler2);

    messageDispatchSender.send(m1);
    messageDispatchSender.send(m2);

    handlerStubFactory.assertSuccess("send", m1);
    handlerStubFactory.assertSuccess("send", m2);
    handlerStubFactory.assertNoMoreCalls();
    fallbackHandlerStubFactory.assertNoMoreCalls();
    otherMessagerHandlerStubFactory.assertNoMoreCalls();

    final OtherMessage m3 = new OtherMessage();
    messageDispatchSender.send(m3);

    otherMessagerHandlerStubFactory.assertSuccess("send", m3);
    otherMessagerHandlerStubFactory.assertNoMoreCalls();
    fallbackHandlerStubFactory.assertNoMoreCalls();
    handlerStubFactory.assertNoMoreCalls();

    handlerStubFactory.setShouldThrowException(true);

    try {
      messageDispatchSender.send(m1);
      fail("Expected CommunicationException");
    }
    catch (CommunicationException e) {
    }

    handlerStubFactory.assertException("send",
                                       CommunicationException.class,
                                       m1);

    handlerStubFactory.assertNoMoreCalls();
    fallbackHandlerStubFactory.assertNoMoreCalls();
    otherMessagerHandlerStubFactory.assertNoMoreCalls();
  }

  public void testWithMessageRequiringResponse() throws Exception {
    final MessageDispatchSender messageDispatchSender =
      new MessageDispatchSender();

    final Message message = new SimpleMessage();
    final MessageRequiringResponse messageRequiringResponse =
      new MessageRequiringResponse(message);

    try {
      messageDispatchSender.send(messageRequiringResponse);
      fail("Expected CommunicationException");
    }
    catch (CommunicationException e) {
    }

    final RandomStubFactory<Sender> senderStubFactory =
      RandomStubFactory.create(Sender.class);
    messageRequiringResponse.setResponder(senderStubFactory.getStub());

    messageDispatchSender.send(messageRequiringResponse);
    senderStubFactory.assertSuccess("send", NoResponseMessage.class);
    senderStubFactory.assertNoMoreCalls();

    // Now check a handler can send a response.
    final Message responseMessage = new SimpleMessage();

    messageDispatchSender.set(
      SimpleMessage.class,
      new MessageDispatchRegistry.AbstractBlockingHandler() {
        public Message blockingSend(Message message)  {
          return responseMessage;
        }
      });

    final MessageRequiringResponse messageRequiringResponse2 =
      new MessageRequiringResponse(message);
    messageRequiringResponse2.setResponder(senderStubFactory.getStub());

    messageDispatchSender.send(messageRequiringResponse2);
    senderStubFactory.assertSuccess("send", responseMessage);
    senderStubFactory.assertNoMoreCalls();

    // Finally, check that fallback handler can handle response.
    final Message responseMessage2 = new SimpleMessage();

    messageDispatchSender.addFallback(
      new MessageDispatchRegistry.AbstractHandler() {
        public void send(Message message) throws CommunicationException {
          if (message instanceof MessageRequiringResponse) {
            final MessageRequiringResponse m =
              (MessageRequiringResponse) message;
            m.sendResponse(responseMessage2);
          }
        }
      });

    final MessageRequiringResponse messageRequiringResponse3 =
      new MessageRequiringResponse(new OtherMessage());
    messageRequiringResponse3.setResponder(senderStubFactory.getStub());

    messageDispatchSender.send(messageRequiringResponse3);
    senderStubFactory.assertSuccess("send", responseMessage2);
    senderStubFactory.assertNoMoreCalls();
  }

  public void testWithBadHandlers() throws Exception {
    final MessageDispatchSender messageDispatchSender =
      new MessageDispatchSender();

    final Message message = new SimpleMessage();

    final RandomStubFactory<Sender> senderStubFactory =
      RandomStubFactory.create(Sender.class);
    final CommunicationException communicationException =
      new CommunicationException("");
    senderStubFactory.setThrows("send", communicationException);

    messageDispatchSender.addFallback(senderStubFactory.getStub());

    try {
      messageDispatchSender.send(message);
    }
    catch (CommunicationException e) {
      assertSame(communicationException, e);
    }

    senderStubFactory.assertException("send",
                                      communicationException,
                                      Message.class);

    senderStubFactory.assertNoMoreCalls();

    messageDispatchSender.set(SimpleMessage.class, senderStubFactory.getStub());

    try {
      messageDispatchSender.send(message);
    }
    catch (CommunicationException e) {
      assertSame(communicationException, e);
    }

    senderStubFactory.assertException("send",
                                      communicationException,
                                      Message.class);

    senderStubFactory.assertNoMoreCalls();
  }

  public void testShutdown() throws Exception {
    final MessageDispatchSender messageDispatchSender =
      new MessageDispatchSender();

    messageDispatchSender.shutdown();

    final HandlerSenderStubFactory handlerStubFactory =
      new HandlerSenderStubFactory();

    messageDispatchSender.set(
      SimpleMessage.class,
      handlerStubFactory.getStub());

    messageDispatchSender.shutdown();

    handlerStubFactory.assertSuccess("shutdown");
    handlerStubFactory.assertNoMoreCalls();

    final RandomStubFactory<Sender> senderStubFactory =
      RandomStubFactory.create(Sender.class);
    messageDispatchSender.addFallback(senderStubFactory.getStub());
    messageDispatchSender.addFallback(senderStubFactory.getStub());

    final RandomStubFactory<BlockingSender> responderStubFactory =
      RandomStubFactory.create(BlockingSender.class);
    messageDispatchSender.set(OtherMessage.class,
                              responderStubFactory.getStub());

    final BlockingSender blockingSender2 =
      new AbstractBlockingHandler() {
        public Message blockingSend(Message message)
          throws CommunicationException {
            return null;
        }};
    messageDispatchSender.set(Message.class, blockingSender2);

    messageDispatchSender.shutdown();

    handlerStubFactory.assertSuccess("shutdown");
    handlerStubFactory.assertNoMoreCalls();
    senderStubFactory.assertSuccess("shutdown");
    senderStubFactory.assertSuccess("shutdown"); // Registered thrice.
    senderStubFactory.assertNoMoreCalls();
    responderStubFactory.assertSuccess("shutdown");
    responderStubFactory.assertNoMoreCalls();
  }

  public static final class HandlerSenderStubFactory
    extends RandomStubFactory<Sender> {

    private boolean m_shouldThrowException;

    public HandlerSenderStubFactory() {
      super(Sender.class);
    }

    public void setShouldThrowException(boolean b) {
      m_shouldThrowException = b;
    }

    public void override_send(Object proxy, Message message)
      throws CommunicationException {

      if (m_shouldThrowException) {
        throw new CommunicationException("");
      }
    }
  }

  public static class OtherMessage implements Message {
  }
}
