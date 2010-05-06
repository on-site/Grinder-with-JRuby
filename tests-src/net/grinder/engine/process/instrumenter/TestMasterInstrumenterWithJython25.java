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

package net.grinder.engine.process.instrumenter;

import static net.grinder.testutility.AssertUtilities.assertContains;

import java.lang.instrument.Instrumentation;

import junit.framework.TestCase;
import junit.framework.TestSuite;
import net.grinder.common.LoggerStubFactory;
import net.grinder.common.StubTest;
import net.grinder.common.Test;
import net.grinder.engine.process.ScriptEngine.Recorder;
import net.grinder.script.NonInstrumentableTypeException;
import net.grinder.script.NotWrappableTypeException;
import net.grinder.testutility.BlockingClassLoader;
import net.grinder.testutility.RandomStubFactory;
import net.grinder.util.weave.agent.ExposeInstrumentation;

import org.python.core.PyObject;

import test.MyClass;


/**
 * Unit tests for {@link MasterInstrumenter}.
 *
 * @author Philip Aston
 * @version $Revision:$
 */
public class TestMasterInstrumenterWithJython25 extends TestCase {

  private final LoggerStubFactory m_loggerStubFactory = new LoggerStubFactory();

  public static TestSuite suite() throws Exception {
    return new TestSuite(
      BlockingClassLoader.createJython25ClassLoader().loadClass(
        TestMasterInstrumenterWithJython25.class.getName()));
  }

  private Instrumentation m_originalInstrumentation;

  @Override
  protected void setUp() throws Exception {
    m_originalInstrumentation = ExposeInstrumentation.getInstrumentation();
  }

  @Override
  protected void tearDown() throws Exception {
    ExposeInstrumentation.premain("", m_originalInstrumentation);
  }

  private final RandomStubFactory<Recorder> m_recorderStubFactory =
    RandomStubFactory.create(Recorder.class);
  private final Recorder m_recorder = m_recorderStubFactory.getStub();

  private final Test m_test = new StubTest(1, "foo");

  public void testVersion() throws Exception {
    AbstractJythonInstrumenterTestCase.assertVersion("2.5");
  }

  public void testCreateInstrumentedProxyWithInstrumentation()
    throws Exception {

    final MasterInstrumenter masterInstrumenter =
      new MasterInstrumenter(m_loggerStubFactory.getLogger(), false);

    assertEquals("byte code transforming instrumenter for Jython 2.5; " +
                 "byte code transforming instrumenter for Java",
                 masterInstrumenter.getDescription());

    m_loggerStubFactory.assertOutputMessageContains("byte code");
    m_loggerStubFactory.assertNoMoreCalls();

    try {
      masterInstrumenter.createInstrumentedProxy(null, null, null);
      fail("Expected NotWrappableTypeException");
    }
    catch (NotWrappableTypeException e) {
    }

    try {
      masterInstrumenter.createInstrumentedProxy(m_test,
                                                 m_recorder,
                                                 new PyObject());
      fail("Expected NotWrappableTypeException");
    }
    catch (NotWrappableTypeException e) {
    }

    masterInstrumenter.createInstrumentedProxy(m_test,
                                               m_recorder,
                                               MyClass.class);
  }

  public void testInstrumentWithInstrumentation() throws Exception {
    final MasterInstrumenter masterInstrumenter =
      new MasterInstrumenter(m_loggerStubFactory.getLogger(), false);

    assertEquals("byte code transforming instrumenter for Jython 2.5; " +
                 "byte code transforming instrumenter for Java",
                 masterInstrumenter.getDescription());

    m_loggerStubFactory.assertOutputMessageContains("byte code");
    m_loggerStubFactory.assertNoMoreCalls();

    try {
      masterInstrumenter.instrument(null, null, null);
      fail("Expected NonInstrumentableTypeException");
    }
    catch (NonInstrumentableTypeException e) {
    }

    try {
      masterInstrumenter.instrument(m_test, m_recorder, new PyObject());
      fail("Expected NotWrappableTypeException");
    }
    catch (NonInstrumentableTypeException e) {
    }

    masterInstrumenter.instrument(m_test, m_recorder, MyClass.class);
  }

  public void testWithCreateInstrumentedProxyWithNoInstrumentation()
    throws Exception {

    ExposeInstrumentation.premain("", null);

    final MasterInstrumenter masterInstrumenter =
      new MasterInstrumenter(m_loggerStubFactory.getLogger(), false);

    assertContains(masterInstrumenter.getDescription(), "NO INSTRUMENTER");

    m_loggerStubFactory.assertOutputMessageContains("does not support");
    m_loggerStubFactory.assertOutputMessageContains("NO INSTRUMENTER");
    m_loggerStubFactory.assertNoMoreCalls();

    try {
      masterInstrumenter.createInstrumentedProxy(null, null, null);
      fail("Expected NotWrappableTypeException");
    }
    catch (NotWrappableTypeException e) {
    }

    try {
      masterInstrumenter.createInstrumentedProxy(m_test,
                                                 m_recorder,
                                                 MyClass.class);
      fail("Expected NotWrappableTypeException");
    }
    catch (NotWrappableTypeException e) {
    }
  }

  public void testInstrumentWithNoInstrumentation() throws Exception {

    ExposeInstrumentation.premain("", null);

    final MasterInstrumenter masterInstrumenter =
      new MasterInstrumenter(m_loggerStubFactory.getLogger(), false);

    assertContains(masterInstrumenter.getDescription(), "NO INSTRUMENTER");

    m_loggerStubFactory.assertOutputMessageContains("does not support");
    m_loggerStubFactory.assertOutputMessageContains("NO INSTRUMENTER");
    m_loggerStubFactory.assertNoMoreCalls();

    try {
      masterInstrumenter.instrument(null, null, null);
      fail("Expected NonInstrumentableTypeException");
    }
    catch (NonInstrumentableTypeException e) {
    }

    try {
      masterInstrumenter.instrument(m_test, m_recorder, MyClass.class);
      fail("Expected NonInstrumentableTypeException");
    }
    catch (NonInstrumentableTypeException e) {
    }
  }
}
