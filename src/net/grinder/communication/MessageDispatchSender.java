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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import net.grinder.util.ListenerSupport;


/**
 * Passive {@link Sender}class that dispatches incoming messages to the
 * appropriate handler.
 *
 * @author Philip Aston
 * @version $Revision: 4003 $
 */
public final class MessageDispatchSender
  implements Sender, MessageDispatchRegistry {

  /* Guarded by m_handlers. */
  private final Map<Class<? extends Message>, Sender> m_handlers =
    Collections.synchronizedMap(
      new HashMap<Class<? extends Message>, Sender>());

  /* Guarded by m_responders. */
  private final Map<Class<? extends Message>, BlockingSender> m_responders =
    Collections.synchronizedMap(
      new HashMap<Class<? extends Message>, BlockingSender>());

  private final ListenerSupport<Sender> m_fallbackHandlers =
    new ListenerSupport<Sender>();

  /**
   * Register a message handler.
   * @param messageType
   *          Messages of this type will be routed to the handler.
   * @param messageHandler
   *          The message handler.
   *
   * @return The previous message handler registered for
   *         <code>messageType</code> or <code>null</code>.
   */
  public Sender set(Class<? extends Message> messageType,
                    Sender messageHandler) {
    return m_handlers.put(messageType, messageHandler);
  }

  /**
   * Register a message responder.
   * @param messageType
   *          Messages of this type will be routed to the handler.
   * @param messageResponder The message responder.
   *
   * @return The previous message handler registered for
   *         <code>messageType</code> or <code>null</code>.
   */
  public BlockingSender set(Class<? extends Message> messageType,
                            BlockingSender messageResponder) {
    return m_responders.put(messageType, messageResponder);
  }

  /**
   * Register a message handler that is called if no other handler or responder
   * is registered for the message type.
   *
   * @param messageHandler The sender.
   */
  public void addFallback(Sender messageHandler) {
    m_fallbackHandlers.add(messageHandler);
  }

  /**
   * Sends a message to each handler until one claims to have handled the
   * message.
   *
   * @param message The message.
   * @throws CommunicationException If one of the handlers failed.
   */
  public void send(final Message message) throws CommunicationException {

    if (message instanceof MessageRequiringResponse) {
      final MessageRequiringResponse messageRequringResponse =
        (MessageRequiringResponse)message;

      final Message requestMessage = messageRequringResponse.getMessage();

      final BlockingSender responder =
        m_responders.get(requestMessage.getClass());

      if (responder != null) {
        messageRequringResponse.sendResponse(
          responder.blockingSend(requestMessage));
        return;
      }
    }
    else {
      final Sender handler = m_handlers.get(message.getClass());

      if (handler != null) {
        handler.send(message);
        return;
      }
    }

    final CommunicationException[] exception = new CommunicationException[1];

    m_fallbackHandlers.apply(new ListenerSupport.Informer<Sender>() {
        public void inform(Sender sender) {
          try {
            sender.send(message);
          }
          catch (CommunicationException e) {
            exception[0] = e;
          }
        }
      });

    if (message instanceof MessageRequiringResponse) {
      final MessageRequiringResponse messageRequringResponse =
        (MessageRequiringResponse)message;

      if (!messageRequringResponse.isResponseSent()) {
        // No one responded.
        messageRequringResponse.sendResponse(new NoResponseMessage());
      }
    }

    if (exception[0] != null) {
      throw exception[0];
    }
  }

 /**
  * Shutdown all our handlers.
  */
  public void shutdown() {
    final Sender[] handlers;

    synchronized (m_handlers) {
      handlers = m_handlers.values().toArray(new Sender[m_handlers.size()]);
    }

    for (int i = 0; i < handlers.length; ++i) {
      handlers[i].shutdown();
    }

    final BlockingSender[] responders;

    synchronized (m_responders) {
      responders =
        m_responders.values().toArray(new BlockingSender[m_responders.size()]);
    }

    for (int i = 0; i < responders.length; ++i) {
      responders[i].shutdown();
    }

    m_fallbackHandlers.apply(new ListenerSupport.Informer<Sender>() {
      public void inform(Sender sender) { sender.shutdown(); }
    });
  }
}
