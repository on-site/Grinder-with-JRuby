// Copyright (C) 2003, 2004, 2005, 2006, 2007, 2008 Philip Aston
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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import net.grinder.common.UncheckedInterruptedException;


/**
 * Constants that are used to discriminate between different types of
 * connections.
 *
 * @author Philip Aston
 * @version $Revision: 3824 $
 */
public final class ConnectionType {

  /** Connection type constant. */
  public static final ConnectionType AGENT =
    new ConnectionType(0, "AGENT connection type");

  /** Connection type constant. */
  public static final ConnectionType WORKER =
    new ConnectionType(1, "WORKER connection type");

  /** Connection type constant. */
  public static final ConnectionType CONSOLE_CLIENT =
    new ConnectionType(2, "CONSOLE_CLIENT connection type");


  /**
   * Serialisation method that reads a ConnectionType from a stream.
   * Package scope.
   *
   * @param in The stream.
   * @return The ConnectionType.
   */
  static ConnectionType read(InputStream in) throws CommunicationException {

    final int i;

    try {
      i = in.read();
    }
    catch (IOException e) {
      UncheckedInterruptedException.ioException(e);
      throw new CommunicationException("Failed to read connection type", e);
    }

    switch (i) {
    case 0:
      return ConnectionType.AGENT;

    case 1:
      return ConnectionType.WORKER;

    case 2:
      return ConnectionType.CONSOLE_CLIENT;

    default:
      throw new CommunicationException("Unknown connection type (" + i + ")");
    }
  }

  private final int m_identity;
  private final String m_description;

  private ConnectionType(int identity, String description) {
    m_identity = identity;
    m_description = description;
  }

  /**
   * Serialisation method that writes a ConnectionType to a stream.
   * Package scope.
   *
   * @param out The stream.
   * @throws CommunicationException If write failed.
   */
  void write(OutputStream out) throws CommunicationException {
    try {
      out.write(m_identity);
      out.flush();
    }
    catch (IOException e) {
      UncheckedInterruptedException.ioException(e);
      throw new CommunicationException("Write failed", e);
    }
  }

  /**
   * Implement {@link Object#hashCode}.
   *
   * @return The hash code.
   */
  public int hashCode() {
    return m_identity;
  }

  /**
   * Equality.
   *
   * @param other An <code>Object</code> to compare.
   * @return <code>true</code> => <code>other</code> is equal to this object.
   */
  public boolean equals(Object other) {
    if (other == this) {
      return true;
    }

    if (other == null || other.getClass() != ConnectionType.class) {
      return false;
    }

    final ConnectionType otherConnectionType = (ConnectionType)other;
    return m_identity == otherConnectionType.m_identity;
  }

  /**
   * Describe ourself.
   *
   * @return The description.
   */
  public String toString() {
    return m_description;
  }
}
