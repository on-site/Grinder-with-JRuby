// Copyright (C) 2003, 2004, 2005, 2006 Philip Aston
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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import junit.framework.TestCase;

import net.grinder.testutility.AssertUtilities;


/**
 *  Unit test case for <code>ConnectionType</code>.
 *
 * @author Philip Aston
 * @version $Revision: 3762 $
 */
public class TestConnectionType extends TestCase {

  public TestConnectionType(String name) {
    super(name);
  }

  public void testToString() {
    assertNotNull(ConnectionType.AGENT.toString());
    AssertUtilities.assertNotEquals(ConnectionType.AGENT.toString(),
                                    ConnectionType.CONSOLE_CLIENT.toString());
    AssertUtilities.assertNotEquals(ConnectionType.CONSOLE_CLIENT.toString(),
                                    ConnectionType.WORKER.toString());
    AssertUtilities.assertNotEquals(ConnectionType.WORKER.toString(),
                                    ConnectionType.AGENT.toString());  }

  public void testEquality() throws Exception {
    assertEquals(ConnectionType.AGENT.hashCode(),
                 ConnectionType.AGENT.hashCode());

    assertEquals(ConnectionType.AGENT, ConnectionType.AGENT);

    assertEquals(ConnectionType.WORKER.hashCode(),
                 ConnectionType.WORKER.hashCode());

    assertEquals(ConnectionType.WORKER, ConnectionType.WORKER);

    assertTrue(!ConnectionType.AGENT.equals(ConnectionType.WORKER));
    assertTrue(!ConnectionType.WORKER.equals(ConnectionType.AGENT));
    assertTrue(!ConnectionType.CONSOLE_CLIENT.equals(ConnectionType.AGENT));

    assertTrue(!ConnectionType.WORKER.equals(new Object()));
  }

  public void testSerialisation() throws Exception {
    final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    ConnectionType.AGENT.write(outputStream);
    ConnectionType.CONSOLE_CLIENT.write(outputStream);
    ConnectionType.WORKER.write(outputStream);

    final InputStream inputSteam =
      new ByteArrayInputStream(outputStream.toByteArray());

    assertEquals(ConnectionType.AGENT, ConnectionType.read(inputSteam));
    assertEquals(ConnectionType.CONSOLE_CLIENT,
                 ConnectionType.read(inputSteam));
    assertEquals(ConnectionType.WORKER, ConnectionType.read(inputSteam));

    try {
      ConnectionType.read(inputSteam);
      fail("Expected CommunicationException");
    }
    catch (CommunicationException e) {
    }

    // ByteArrayInputStream isn't bad enough.

    final InputStream badInputStream =
      new InputStream() {
        public int read() throws IOException {
          throw new IOException("Eat me");
        }
    };

    try {
      ConnectionType.read(badInputStream);
      fail("Exception CommunicationException");
    }
    catch (CommunicationException e) {
    }

    final OutputStream badOutputStream =
      new OutputStream() {
        public void write(int b) throws IOException {
          throw new IOException("Eat me");
        }
    };

    try {
      ConnectionType.AGENT.write(badOutputStream);
      fail("Exception CommunicationException");
    }
    catch (CommunicationException e) {
    }
  }
}
