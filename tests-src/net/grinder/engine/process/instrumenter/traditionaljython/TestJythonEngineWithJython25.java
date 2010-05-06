// Copyright (C) 2009 Philip Aston
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

package net.grinder.engine.process.instrumenter.traditionaljython;

import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;
import junit.framework.TestSuite;
import net.grinder.engine.process.Instrumenter;
import net.grinder.engine.process.instrumenter.AbstractJythonInstrumenterTestCase;
import net.grinder.testutility.BlockingClassLoader;


/**
 * Miscellaneous unit tests for Jython instrumentation.
 *
 * @author Philip Aston
 * @version $Revision:$
 */
public class TestJythonEngineWithJython25 extends TestCase {

  public static TestSuite suite() throws Exception {
    return new TestSuite(
      BlockingClassLoader.createJython25ClassLoader().loadClass(
        TestJythonEngineWithJython25.class.getName()));
  }

  public void testJythonInstrumenterFactory() throws Exception {
    AbstractJythonInstrumenterTestCase.assertVersion("2.5");

    final List<Instrumenter> instrumenters = new ArrayList<Instrumenter>();

    final boolean result =
      JythonInstrumenterFactory.addJythonInstrumenter(instrumenters);

    assertFalse(result);
    assertEquals(0, instrumenters.size());
  }
}
