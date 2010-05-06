// Copyright (C) 2006 - 2009 Philip Aston
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


/**
 * Register of message handlers, keyed by message type.
 *
 * <p>
 * The current implementation of this interface ({@link MessageDispatchSender})
 * does not interpret the message type polymorphically. That is, a message is
 * only passed to a handler if the handler was registered for the message's
 * class; messages registered for super classes are not invoked.
 * </p>
 *
 * @author Philip Aston
 * @version $Revision: 4003 $
 */
public interface MessageDispatchRegistry {

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
  Sender set(Class<? extends Message> messageType, Sender messageHandler);

  /**
   * Register a message responder.
   * @param messageType
   *          Messages of this type will be routed to the handler.
   * @param messageResponder The message responder.
   *
   * @return The previous message handler registered for
   *         <code>messageType</code> or <code>null</code>.
   */
  BlockingSender set(Class<? extends Message> messageType,
                     BlockingSender messageResponder);

  /**
   * Register a message handler that is called if no other handler or responder
   * is registered for the message type. There can be multiple such handlers.
   *
   * @param messageHandler The sender.
   */
  void addFallback(Sender messageHandler);

  /**
   * Most handlers ignore the shutdown event, so provide this as a convenient
   * base for anonymous classes.
   */
  public abstract static class AbstractHandler implements Sender {
    /**
     * Ignore shutdown events.
     */
    public void shutdown() {
    }
  }

  /**
   * Most handlers ignore the shutdown event, so provide this as a convenient
   * base for anonymous classes.
   */
  public abstract static class AbstractBlockingHandler
    implements BlockingSender {

    /**
     * Ignore shutdown events.
     */
    public void shutdown() {
    }
  }
}
