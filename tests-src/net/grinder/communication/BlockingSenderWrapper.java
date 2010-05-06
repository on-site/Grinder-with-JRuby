// Copyright (C) 2006 Philip Aston
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

import junit.framework.Assert;


/**
 * BlockingSender that wraps a Sender - used by unit tests.
 *
 * @author Philip Aston
 * @version $Revision: 3995 $
 */
public class BlockingSenderWrapper implements BlockingSender {

  private final Sender m_delegate;

  public BlockingSenderWrapper(Sender sender) {
    m_delegate = sender;
  }

  public Message blockingSend(Message message) throws CommunicationException {

    final MessageRequiringResponse messageRequringResponse =
      new MessageRequiringResponse(message);

    final Message[] response = new Message[1];

    final Sender captureResponse =
      new Sender() {

        public void send(Message message) throws CommunicationException {
          response[0] = message;
        }

        public void shutdown() {
          Assert.fail("Should not be called");
        }
    };


    messageRequringResponse.setResponder(captureResponse);

    m_delegate.send(messageRequringResponse);

    return response[0];
  }

  public void shutdown() {
    m_delegate.shutdown();
  }
}
