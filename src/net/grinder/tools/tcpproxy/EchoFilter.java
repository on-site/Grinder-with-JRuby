// Copyright (C) 2000 Phil Dawes
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

package net.grinder.tools.tcpproxy;

import java.io.PrintWriter;

import net.grinder.common.Logger;


/**
 * Filter that echos to the output stream of a logger.
 *
 * @author Philip Aston
 * @author Bertrand Ave
 * @version $Revision: 3762 $
 */
public class EchoFilter implements TCPProxyFilter {

  private final PrintWriter m_out;

  /**
   * Constructor.
   *
   * @param logger Logger to use.
   */
  public EchoFilter(Logger logger) {
    m_out = logger.getOutputLogWriter();
  }

  /**
   * Handle a message fragment from the stream.
   *
   * @param connectionDetails Describes the connection.
   * @param buffer Contains the data.
   * @param bytesRead How many bytes of data in <code>buffer</code>.
   * @return Filters can optionally return a <code>byte[]</code>
   * which will be transmitted to the server instead of
   * <code>buffer</code>.
   * @throws FilterException If an error occurs.
   */
  public byte[] handle(ConnectionDetails connectionDetails,
                       byte[] buffer, int bytesRead)
    throws FilterException {

    final StringBuffer stringBuffer = new StringBuffer();

    boolean inHex = false;

    for (int i = 0; i < bytesRead; i++) {
      final int value = buffer[i] & 0xFF;

      // If it's ASCII, print it as a char.
      if (value == '\r' || value == '\n' || (value >= ' ' && value <= '~')) {

        if (inHex) {
          stringBuffer.append(']');
          inHex = false;
        }

        stringBuffer.append((char)value);
      }
      // Else print the value.
      else {
        if (!inHex) {
          stringBuffer.append('[');
          inHex = true;
        }

        if (value <= 0xf) { // Where's "HexNumberFormatter?"
          stringBuffer.append("0");
        }

        stringBuffer.append(Integer.toHexString(value).toUpperCase());
      }
    }

    m_out.println("------ " + connectionDetails + " ------");
    m_out.println(stringBuffer);

    return null;
  }

  /**
   * A new connection has been opened.
   *
   * @param connectionDetails Describes the connection.
   */
  public void connectionOpened(ConnectionDetails connectionDetails) {

    m_out.println("--- " +  connectionDetails + " opened --");
  }

  /**
   * A connection has been closed.
   *
   * @param connectionDetails Describes the connection.
   */
  public void connectionClosed(ConnectionDetails connectionDetails) {

    m_out.println("--- " +  connectionDetails + " closed --");
  }
}



