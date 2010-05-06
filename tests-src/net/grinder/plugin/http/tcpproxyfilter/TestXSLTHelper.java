// Copyright (C) 2005 - 2008 Philip Aston
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

import java.text.ParseException;

import HTTPClient.Codecs;
import junit.framework.TestCase;


/**
 * Unit tests for {@link XSLTHelper}.
 *
 * @author Philip Aston
 * @version $Revision: 3880 $
 */
public class TestXSLTHelper extends TestCase {

  protected void setUp() throws Exception {
    XSLTHelper.resetIndent();
  }

  public void testBase64ToPython() throws Exception {
    final byte[] bytes0 = { 0, -42, 1, 22, };

    assertEquals("\"\\x00\\xD6\\x01\\x16\"",
                 XSLTHelper.base64ToPython(
                   new String(Codecs.base64Encode(bytes0))));

    XSLTHelper.changeIndent(2);

    assertEquals("\"\\x00\\xD6\\x01\\x16\"",
      XSLTHelper.base64ToPython(new String(Codecs.base64Encode(bytes0))));

    assertEquals("\"\"", XSLTHelper.base64ToPython(""));

    final byte[] bytes1 = new byte[300];

    for (int i = 0; i < bytes1.length; ++i) {
      bytes1[i] = (byte) i;
    }

    assertEquals(
      "\"\\x00\\x01\\x02\\x03\\x04\\x05\\x06\\x07\\x08\\x09\\x0A\\x0B\\x0C\\x0D\\x0E\\x0F\"\n" +
      "    \"\\x10\\x11\\x12\\x13\\x14\\x15\\x16\\x17\\x18\\x19\\x1A\\x1B\\x1C\\x1D\\x1E\\x1F\"\n" +
      "    \"\\x20\\x21\\x22\\x23\\x24\\x25\\x26\\x27\\x28\\x29\\x2A\\x2B\\x2C\\x2D\\x2E\\x2F\"\n" +
      "    \"\\x30\\x31\\x32\\x33\\x34\\x35\\x36\\x37\\x38\\x39\\x3A\\x3B\\x3C\\x3D\\x3E\\x3F\"\n" +
      "    \"\\x40\\x41\\x42\\x43\\x44\\x45\\x46\\x47\\x48\\x49\\x4A\\x4B\\x4C\\x4D\\x4E\\x4F\"\n" +
      "    \"\\x50\\x51\\x52\\x53\\x54\\x55\\x56\\x57\\x58\\x59\\x5A\\x5B\\x5C\\x5D\\x5E\\x5F\"\n" +
      "    \"\\x60\\x61\\x62\\x63\\x64\\x65\\x66\\x67\\x68\\x69\\x6A\\x6B\\x6C\\x6D\\x6E\\x6F\"\n" +
      "    \"\\x70\\x71\\x72\\x73\\x74\\x75\\x76\\x77\\x78\\x79\\x7A\\x7B\\x7C\\x7D\\x7E\\x7F\"\n" +
      "    \"\\x80\\x81\\x82\\x83\\x84\\x85\\x86\\x87\\x88\\x89\\x8A\\x8B\\x8C\\x8D\\x8E\\x8F\"\n" +
      "    \"\\x90\\x91\\x92\\x93\\x94\\x95\\x96\\x97\\x98\\x99\\x9A\\x9B\\x9C\\x9D\\x9E\\x9F\"\n" +
      "    \"\\xA0\\xA1\\xA2\\xA3\\xA4\\xA5\\xA6\\xA7\\xA8\\xA9\\xAA\\xAB\\xAC\\xAD\\xAE\\xAF\"\n" +
      "    \"\\xB0\\xB1\\xB2\\xB3\\xB4\\xB5\\xB6\\xB7\\xB8\\xB9\\xBA\\xBB\\xBC\\xBD\\xBE\\xBF\"\n" +
      "    \"\\xC0\\xC1\\xC2\\xC3\\xC4\\xC5\\xC6\\xC7\\xC8\\xC9\\xCA\\xCB\\xCC\\xCD\\xCE\\xCF\"\n" +
      "    \"\\xD0\\xD1\\xD2\\xD3\\xD4\\xD5\\xD6\\xD7\\xD8\\xD9\\xDA\\xDB\\xDC\\xDD\\xDE\\xDF\"\n" +
      "    \"\\xE0\\xE1\\xE2\\xE3\\xE4\\xE5\\xE6\\xE7\\xE8\\xE9\\xEA\\xEB\\xEC\\xED\\xEE\\xEF\"\n" +
      "    \"\\xF0\\xF1\\xF2\\xF3\\xF4\\xF5\\xF6\\xF7\\xF8\\xF9\\xFA\\xFB\\xFC\\xFD\\xFE\\xFF\"\n" +
      "    \"\\x00\\x01\\x02\\x03\\x04\\x05\\x06\\x07\\x08\\x09\\x0A\\x0B\\x0C\\x0D\\x0E\\x0F\"\n" +
      "    \"\\x10\\x11\\x12\\x13\\x14\\x15\\x16\\x17\\x18\\x19\\x1A\\x1B\\x1C\\x1D\\x1E\\x1F\"\n" +
      "    \"\\x20\\x21\\x22\\x23\\x24\\x25\\x26\\x27\\x28\\x29\\x2A\\x2B\"",
      XSLTHelper.base64ToPython(new String(Codecs.base64Encode(bytes1))));
  }

  public void testFormatTime() throws Exception {
    try {
      XSLTHelper.formatTime("abc");
      fail("Expected ParseException");
    }
    catch (ParseException e) {
    }

    final String s = XSLTHelper.formatTime("2005-01-04T18:30:00");
    assertNotNull(s);
  }

  public void testQuoteForPython() throws Exception {
    assertEquals("None", XSLTHelper.quoteForPython(null));
    assertEquals("''", XSLTHelper.quoteForPython(""));
    assertEquals("\'\\\"\'", XSLTHelper.quoteForPython("\""));
    assertEquals("'foo'", XSLTHelper.quoteForPython("foo"));
    assertEquals("'foo\\''", XSLTHelper.quoteForPython("foo'"));
    assertEquals("' \\\\ '", XSLTHelper.quoteForPython(" \\ "));
    assertEquals("'''foo \n bah'''", XSLTHelper.quoteForPython("foo \n bah"));
    assertEquals("'''foo \\r bah'''", XSLTHelper.quoteForPython("foo \r bah"));
    assertEquals("'foo \\\\n bah'", XSLTHelper.quoteForPython("foo \\n bah"));
  }

  public void testQuoteEOLEscapedStringForPython() throws Exception {
    assertEquals("None", XSLTHelper.quoteEOLEscapedStringForPython(null));
    assertEquals("''", XSLTHelper.quoteEOLEscapedStringForPython(""));
    assertEquals("\'\\\"\'", XSLTHelper.quoteEOLEscapedStringForPython("\""));
    assertEquals("'foo'", XSLTHelper.quoteEOLEscapedStringForPython("foo"));
    assertEquals("'foo\\''", XSLTHelper.quoteEOLEscapedStringForPython("foo'"));
    assertEquals("' \\\\ '", XSLTHelper.quoteEOLEscapedStringForPython(" \\ "));
    assertEquals("'''foo   bah'''",
      XSLTHelper.quoteEOLEscapedStringForPython("foo \n \r bah"));
    assertEquals("'foo \\n bah\\\\'",
      XSLTHelper.quoteEOLEscapedStringForPython("foo \\n bah\\"));

  }

  public void testEscape() throws Exception {
    assertEquals("", XSLTHelper.escape(""));
    assertEquals("\\'", XSLTHelper.escape("'"));
    assertEquals("\\\"", XSLTHelper.escape("\""));
    assertEquals("\\\\", XSLTHelper.escape("\\"));
    assertEquals("Hello \\'quoted\\\" \\\\world",
                 XSLTHelper.escape("Hello 'quoted\" \\world"));
  }

  public void testSummariseAsLine() throws Exception {
    assertEquals("blah, blah", XSLTHelper.summariseAsLine("blah, blah", 20));
    assertEquals("blah,...", XSLTHelper.summariseAsLine("blah, blah", 5));
    assertEquals("blah,\\nblah", XSLTHelper.summariseAsLine("blah,\nblah", 20));
    assertEquals("\\r blah,\\t", XSLTHelper.summariseAsLine("\r blah,\t", 20));
    assertEquals("..bla...", XSLTHelper.summariseAsLine("\0\0blah", 5));
  }
}
