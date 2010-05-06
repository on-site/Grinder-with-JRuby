// Copyright (C) 2004 Philip Aston
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

package net.grinder.util;

import junit.framework.TestCase;

import java.io.PrintWriter;
import java.io.StringWriter;

import net.grinder.common.Logger;


/**
 *  Unit tests for <code>SimpleLogger</code>.
 *
 * @author Philip Aston
 * @version $Revision: 3762 $
 */
public class TestSimpleLogger extends TestCase {

  public void testLogging() throws Exception {

    final StringWriter outStringWriter = new StringWriter();
    final PrintWriter outWriter = new PrintWriter(outStringWriter);

    final StringWriter errorStringWriter = new StringWriter();
    final PrintWriter errorWriter = new PrintWriter(errorStringWriter);

    final Logger logger = new SimpleLogger("testme", outWriter, errorWriter);

    assertSame(outWriter, logger.getOutputLogWriter());
    assertSame(errorWriter, logger.getErrorLogWriter());
    
    logger.output("Hello");
    final String outString1 = outStringWriter.toString();
    assertTrue(outString1.indexOf("testme") >= 0);
    assertTrue(outString1.indexOf("Hello") >= 0);
    assertEquals("", errorStringWriter.toString());

    logger.output("Hello", Logger.LOG);
    assertEquals(outString1, outStringWriter.toString());

    logger.error("meanwhile back", Logger.LOG | Logger.TERMINAL);
    final String errorString1 = errorStringWriter.toString();
    assertTrue(errorString1.indexOf("testme") >= 0);
    assertTrue(errorString1.indexOf("meanwhile back") >= 0);
    assertEquals(outString1, outStringWriter.toString());

    logger.error("1234");
    final String errorString2 =
      errorStringWriter.toString().substring(errorString1.length());
    assertTrue(errorString2.indexOf("testme") >= 0);
    assertTrue(errorString2.indexOf("1234") >= 0);
    assertEquals(outString1, outStringWriter.toString());

    logger.error("No culture icons", Logger.LOG);
    assertEquals(errorString1 + errorString2, errorStringWriter.toString());

    logger.error("No culture icons", 0);
    assertEquals(errorString1 + errorString2, errorStringWriter.toString());
  }
}
