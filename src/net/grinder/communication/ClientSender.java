// Copyright (C) 2000 - 2008 Philip Aston
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
 * Class that manages the sending of messages to a server.
 *
 * @author Philip Aston
 * @version $Revision: 3862 $
 **/
public final class ClientSender extends StreamSender implements BlockingSender {

  /**
   * Factory method that makes a TCP connection and returns a
   * corresponding <code>Sender</code>.
   *
   * @param connector Connector to use to make the connection to the
   * server.
   * @return The ClientSender.
   * @throws CommunicationException If failed to connect.
   */
  public static ClientSender connect(Connector connector)
    throws CommunicationException {

    return new ClientSender(new SocketWrapper(connector.connect()));
  }

  /**
   * Factory method that makes <code>Sender</code> around the existing TCP
   * connection owned by the supplied <code>ClientReceiver</code>.
   *
   * @param clientReceiver We create a paired <code>Sender</code> for this
   * <code>Receiver</code>.
   * @return The ClientSender.
   * @throws CommunicationException If failed to connect.
   */
  public static ClientSender connect(ClientReceiver clientReceiver)
    throws CommunicationException {

    return new ClientSender(clientReceiver.getSocketWrapper());
  }

  private final SocketWrapper m_socketWrapper;

  private ClientSender(SocketWrapper socketWrapper)
    throws CommunicationException {

    super(socketWrapper.getOutputStream());
    m_socketWrapper = socketWrapper;
  }

  /**
   * Cleanly shutdown the <code>Sender</code>.
   */
  public void shutdown() {
    // Close the socket wrapper first as that needs to use the socket.
    m_socketWrapper.close();

    super.shutdown();
  }

  /**
   * Send the given message and await a response.
   *
   * <p>
   * The input stream is that of our socket. This method should only be used
   * where the sender can guarantee that the input stream will be free for
   * exclusive use - we don't lock out external processes from interrupting the
   * stream.
   * </p>
   *
   * @param message
   *          A {@link Message}.
   * @return The response message.
   * @throws CommunicationException
   *           If an error occurs.
   */
  public Message blockingSend(Message message) throws CommunicationException {
    final MessageRequiringResponse messageRequiringResponse =
      new MessageRequiringResponse(message);

    final Message result;

    synchronized (m_socketWrapper) {
      send(messageRequiringResponse);

      final Receiver receiver =
        new StreamReceiver(m_socketWrapper.getInputStream());

      result = receiver.waitForMessage();
    }

    if (result == null) {
      throw new CommunicationException("Shut down");
    }
    else if (result instanceof NoResponseMessage) {
      throw new NoResponseException("Server did not respond");
    }

    return result;
  }
}

