// Copyright (C) 2009 - 2010 Philip Aston
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

package net.grinder.engine.process.instrumenter.dcr;

import java.lang.instrument.Instrumentation;
import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import net.grinder.common.LoggerStubFactory;
import net.grinder.engine.process.Instrumenter;
import net.grinder.testutility.RandomStubFactory;
import net.grinder.util.weave.agent.ExposeInstrumentation;


/**
 * Unit tests for {@link DCRInstrumenterFactory}.
 *
 * @author Philip Aston
 * @version $Revision:$
 */
public class TestDCRInstrumenterFactory extends TestCase {

  private Instrumentation m_originalInstrumentation;

  @Override
  protected void setUp() throws Exception {
    m_originalInstrumentation = ExposeInstrumentation.getInstrumentation();
  }

  @Override
  protected void tearDown() throws Exception {
    ExposeInstrumentation.premain("", m_originalInstrumentation);
  }

  private final RandomStubFactory<Instrumentation>
    m_instrumentationStubFactory =
      RandomStubFactory.create(Instrumentation.class);
  private final Instrumentation m_instrumentation =
    m_instrumentationStubFactory.getStub();

  private final LoggerStubFactory m_loggerStubFactory =
    new LoggerStubFactory();

  public void testCreateWithNoInstrumentation() throws Exception {
    ExposeInstrumentation.premain("", null);

    assertNull(DCRInstrumenterFactory.createFactory(
                 m_loggerStubFactory.getLogger()));

    m_loggerStubFactory.assertOutputMessageContains("does not support");
    m_loggerStubFactory.assertNoMoreCalls();
  }

  public void testCreateWithNoRetransformation() throws Exception {
    ExposeInstrumentation.premain("", m_instrumentation);

    m_instrumentationStubFactory.setResult("isRetransformClassesSupported",
                                           false);

    assertNull(
      DCRInstrumenterFactory.createFactory(m_loggerStubFactory.getLogger()));

    m_loggerStubFactory.assertOutputMessageContains("does not support");
    m_loggerStubFactory.assertNoMoreCalls();

    m_instrumentationStubFactory.setThrows("isRetransformClassesSupported",
                                           new NoSuchMethodError());

    assertNull(
      DCRInstrumenterFactory.createFactory(m_loggerStubFactory.getLogger()));

    m_loggerStubFactory.assertOutputMessageContains("does not support");
    m_loggerStubFactory.assertNoMoreCalls();
  }

  public void testAddJavaInstrumentation() throws Exception {

    final DCRInstrumenterFactory factory =
      DCRInstrumenterFactory.createFactory(m_loggerStubFactory.getLogger());

    final List<Instrumenter> instrumenters = new ArrayList<Instrumenter>();

    final boolean result = factory.addJavaInstrumenter(instrumenters);

    assertTrue(result);
    assertEquals(1, instrumenters.size());

    assertEquals("byte code transforming instrumenter for Java",
                 instrumenters.get(0).getDescription());

    m_loggerStubFactory.assertNoMoreCalls();
  }

  public void testAddJythonInstrumentation() throws Exception {

    final DCRInstrumenterFactory factory =
      DCRInstrumenterFactory.createFactory(null);

    final List<Instrumenter> instrumenters = new ArrayList<Instrumenter>();

    final boolean result = factory.addJythonInstrumenter(instrumenters);

    assertTrue(result);
    assertEquals(1, instrumenters.size());

    assertEquals("byte code transforming instrumenter for Jython 2.1/2.2",
                 instrumenters.get(0).getDescription());
  }

  public void testAddAllInstrumentation() throws Exception {

    final DCRInstrumenterFactory factory =
      DCRInstrumenterFactory.createFactory(null);

    final List<Instrumenter> instrumenters = new ArrayList<Instrumenter>();

    final boolean result = factory.addJythonInstrumenter(instrumenters);
    assertTrue(result);

    final boolean result2 = factory.addJavaInstrumenter(instrumenters);
    assertTrue(result2);

    assertEquals(2, instrumenters.size());
  }

  public void testWithBadAdvice() throws Exception {
    try {
      new DCRInstrumenterFactory(m_instrumentation,
                                 TestDCRInstrumenterFactory.class,
                                 RecorderLocator.getRecorderRegistry());
      fail("Expected AssertionError");
    }
    catch (AssertionError e) {
    }
  }
}
