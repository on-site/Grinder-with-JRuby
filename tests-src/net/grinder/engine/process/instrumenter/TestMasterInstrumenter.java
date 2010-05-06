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

import junit.framework.TestCase;
import net.grinder.common.LoggerStubFactory;
import net.grinder.common.StubTest;
import net.grinder.common.Test;
import net.grinder.engine.process.ScriptEngine.Recorder;
import net.grinder.script.NonInstrumentableTypeException;
import net.grinder.script.NotWrappableTypeException;
import net.grinder.testutility.RandomStubFactory;

import org.python.core.PyObject;
import org.python.core.PySystemState;


/**
 * Unit tests for {@link MasterInstrumenter}.
 *
 * @author Philip Aston
 * @version $Revision:$
 */
public class TestMasterInstrumenter extends TestCase {

  {
    PySystemState.initialize();
  }

  private final RandomStubFactory<Recorder> m_recorderStubFactory =
    RandomStubFactory.create(Recorder.class);
  private final Recorder m_recorder = m_recorderStubFactory.getStub();

  private final LoggerStubFactory m_loggerStubFactory = new LoggerStubFactory();

  private final Test m_test = new StubTest(1, "foo");

  public void testCreateInstrumentedProxyWithDefaults() throws Exception {
    final MasterInstrumenter masterInstrumenter =
      new MasterInstrumenter(m_loggerStubFactory.getLogger(), false);

    assertEquals("traditional Jython instrumenter; " +
                 "byte code transforming instrumenter for Java",
                 masterInstrumenter.getDescription());

    m_loggerStubFactory.assertOutputMessageContains("traditional Jython");
    m_loggerStubFactory.assertNoMoreCalls();

    try {
      masterInstrumenter.createInstrumentedProxy(null, m_recorder, null);
      fail("Expected NotWrappableTypeException");
    }
    catch (NotWrappableTypeException e) {
    }

    final Object foo = new Object();

    final PyObject proxy =
      (PyObject)
      masterInstrumenter.createInstrumentedProxy(m_test, m_recorder, foo);

    assertSame(proxy.__getattr__("__target__").__tojava__(Object.class), foo);

    try {
      masterInstrumenter.createInstrumentedProxy(m_test,
                                                 m_recorder,
                                                 new PyObject());
      fail("Expected NotWrappableTypeException");
    }
    catch (NotWrappableTypeException e) {
    }
  }

  public void testInstrumentWithDefaults() throws Exception {
    final MasterInstrumenter masterInstrumenter =
      new MasterInstrumenter(m_loggerStubFactory.getLogger(), false);

    assertEquals("traditional Jython instrumenter; " +
                 "byte code transforming instrumenter for Java",
                 masterInstrumenter.getDescription());

    m_loggerStubFactory.assertOutputMessageContains("traditional Jython");
    m_loggerStubFactory.assertNoMoreCalls();

    try {
      masterInstrumenter.instrument(null, m_recorder, null);
      fail("Expected NonInstrumentableTypeException");
    }
    catch (NonInstrumentableTypeException e) {
    }

    final Object foo = new Object();

    try {
      masterInstrumenter.instrument(m_test, m_recorder, foo);
      fail("Expected NonInstrumentableTypeException");
    }
    catch (NonInstrumentableTypeException e) {
    }
  }

  public void testWithForcedDCRInsstrumentation() throws Exception {
    final MasterInstrumenter masterInstrumenter =
      new MasterInstrumenter(m_loggerStubFactory.getLogger(), true);

    assertEquals("byte code transforming instrumenter for Jython 2.1/2.2; " +
                 "byte code transforming instrumenter for Java",
                 masterInstrumenter.getDescription());

    m_loggerStubFactory.assertOutputMessageContains("byte code");
    m_loggerStubFactory.assertNoMoreCalls();
  }
}
