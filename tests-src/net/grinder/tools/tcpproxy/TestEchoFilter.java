// Copyright (C) 2005 Philip Aston
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

import net.grinder.common.LoggerStubFactory;
import net.grinder.testutility.AssertUtilities;

import junit.framework.TestCase;


/**
 * Unit test case for {@link EchoFilter}.
 *
 * @author Philip Aston
 * @version $Revision: 3762 $
 */
public class TestEchoFilter extends TestCase {

  private static final String LINE_SEPARATOR =
      System.getProperty("line.separator");

  private final EndPoint m_endPoint1 = new EndPoint("host1", 1234);
  private final EndPoint m_endPoint2 = new EndPoint("host2", 99);

  private final ConnectionDetails m_connectionDetails =
      new ConnectionDetails(m_endPoint1, m_endPoint2, false);

  private LoggerStubFactory m_loggerStubFactory = new LoggerStubFactory();

  public void testHandle() throws Exception {
    final EchoFilter echoFilter =
      new EchoFilter(m_loggerStubFactory.getLogger());

    m_loggerStubFactory.assertSuccess("getOutputLogWriter");
    m_loggerStubFactory.assertNoMoreCalls();

    echoFilter.handle(m_connectionDetails, "This is a campaign.".getBytes(), 5);

    final String output =
      m_loggerStubFactory.getOutputLogWriter().getOutputAndReset();
    AssertUtilities.assertContains(output, m_connectionDetails.toString());
    AssertUtilities.assertContains(output, "This " + LINE_SEPARATOR);
    m_loggerStubFactory.assertNoMoreCalls();

    final String lines = "Some\nlines\rblah";
    echoFilter.handle(m_connectionDetails, lines.getBytes(), lines.length());

    final String output2 =
      m_loggerStubFactory.getOutputLogWriter().getOutputAndReset();
    AssertUtilities.assertContains(output2, m_connectionDetails.toString());
    AssertUtilities.assertContains(
      output2, "Some\nlines\rblah" + LINE_SEPARATOR);
    m_loggerStubFactory.assertNoMoreCalls();

    final byte[] binary = { 0x01, (byte)0xE7, 'a', 'b', 'c', (byte)0x89,
                            'd', 'a', 'h', '\n', 'b', 'a', 'h' };
    echoFilter.handle(m_connectionDetails, binary, binary.length);
    final String output3 =
      m_loggerStubFactory.getOutputLogWriter().getOutputAndReset();
    AssertUtilities.assertContains(output3, m_connectionDetails.toString());
    AssertUtilities.assertContains(
      output3, "[01E7]abc[89]dah\nbah" + LINE_SEPARATOR);
    m_loggerStubFactory.assertNoMoreCalls();
  }

  public void testOtherMethods() throws Exception {
    final EchoFilter echoFilter =
      new EchoFilter(m_loggerStubFactory.getLogger());

    m_loggerStubFactory.assertSuccess("getOutputLogWriter");
    m_loggerStubFactory.assertNoMoreCalls();

    echoFilter.connectionOpened(m_connectionDetails);
    assertEquals(1, m_loggerStubFactory.getOutputLogWriter().getLineCount());
    final String output =
      m_loggerStubFactory.getOutputLogWriter().getOutputAndReset();
    AssertUtilities.assertContains(output, m_connectionDetails.toString());
    AssertUtilities.assertContains(output, "opened");

    echoFilter.connectionClosed(m_connectionDetails);
    final String output2 =
      m_loggerStubFactory.getOutputLogWriter().getOutputAndReset();
    AssertUtilities.assertContains(output2, m_connectionDetails.toString());
    AssertUtilities.assertContains(output2, "closed");

    m_loggerStubFactory.assertNoMoreCalls();
  }
}
