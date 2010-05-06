// Copyright (C) 2000, 2001, 2002, 2003, 2004, 2005 Philip Aston
// Copyright (C) 2003 Bertrand Ave
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

package net.grinder.plugin.http.tcpproxyfilter;

import net.grinder.tools.tcpproxy.ConnectionDetails;
import net.grinder.tools.tcpproxy.TCPProxyFilter;


/**
 * {@link TCPProxyFilter} that collects data from server responses.
 * Should be installed as a response filter. Used by
 * HttpPluginTCPProxyFilter to determine things such as the basic
 * authentication realm.
 *
 * @author Philip Aston
 * @author Bertrand Ave
 * @version $Revision: 3762 $
 */
public class HTTPResponseFilter implements TCPProxyFilter {

  private final HTTPFilterEventListener m_eventListener;

  /**
   * Constructor.
   *
   * @param eventListener
   *         Connection handler.
   */
  public HTTPResponseFilter(HTTPFilterEventListener eventListener) {
    m_eventListener = eventListener;
  }

  /**
   * The main handler method called by the proxy engine.
   *
   * <p>NOTE, this is called for message fragments, don't assume
   * that its passed a complete HTTP message at a time.</p>
   *
   * @param connectionDetails The TCP connection.
   * @param buffer The message fragment buffer.
   * @param bytesRead The number of bytes of buffer to process.
   * @return Filters can optionally return a <code>byte[]</code>
   * which will be transmitted to the server instead of
   * <code>buffer</code>.
   * @exception FilterException If an error occurs.
   */
  public byte[] handle(ConnectionDetails connectionDetails, byte[] buffer,
                       int bytesRead)
    throws FilterException {

    m_eventListener.response(connectionDetails, buffer, bytesRead);

    return null;
  }

  /**
   * A connection has been opened.
   *
   * @param connectionDetails a <code>ConnectionDetails</code> value
   */
  public void connectionOpened(ConnectionDetails connectionDetails) {
  }

  /**
   * A connection has been closed.
   *
   * @param connectionDetails a <code>ConnectionDetails</code> value
   */
  public void connectionClosed(ConnectionDetails connectionDetails) {
  }
}
