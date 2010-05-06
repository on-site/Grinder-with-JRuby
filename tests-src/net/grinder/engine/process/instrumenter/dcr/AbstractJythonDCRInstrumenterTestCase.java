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

package net.grinder.engine.process.instrumenter.dcr;

import org.python.core.PyObject;
import org.python.util.PythonInterpreter;

import test.MyClass;

import net.grinder.common.Test;
import net.grinder.engine.process.Instrumenter;
import net.grinder.engine.process.instrumenter.AbstractJythonInstrumenterTestCase;
import net.grinder.util.weave.Weaver;
import net.grinder.util.weave.WeavingException;
import net.grinder.util.weave.agent.ExposeInstrumentation;
import net.grinder.util.weave.j2se6.ASMTransformerFactory;
import net.grinder.util.weave.j2se6.DCRWeaver;


/**
 * Common stuff for Jython DCR instrumenters.
 *
 * @author Philip Aston
 * @version $Revision:$
 */
public abstract class AbstractJythonDCRInstrumenterTestCase
  extends AbstractJythonInstrumenterTestCase {

  protected static Weaver createWeaver() {
    try {
      return
        new DCRWeaver(new ASMTransformerFactory(RecorderLocator.class),
                      ExposeInstrumentation.getInstrumentation());
    }
    catch (WeavingException e) {
      throw new ExceptionInInitializerError(e);
    }
  }

  public AbstractJythonDCRInstrumenterTestCase(Instrumenter instrumenter) {
    super(instrumenter);
  }

  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
    RecorderLocator.clearRecorders();
  }

  @Override
  protected void assertTestReference(PyObject pyObject, Test test) {
    // No-op, DCRInstrumenter doesn't support __test__.
  }

  @Override
  protected void assertTargetReference(PyObject proxy,
                                       Object original,
                                       boolean unwrapTarget) {
    // DCRInstrumenter doesn't support __target__.
  }

  public void testInstrumentationWithNonWrappableParameters() throws Exception {

    // The types that can be wrapped depend on the Instrumenter.

    final PythonInterpreter interpreter = getInterpretter();

    // Can't wrap PyInteger.
    interpreter.exec("x=1");
    assertNotWrappable(interpreter.get("x"));

    assertNotWrappableByThisInstrumenter(null);

    assertNotWrappableByThisInstrumenter(MyClass.class);
  }

  public void testInstrumentationWithPyClass() throws Exception {
    m_interpreter.exec("class Foo:\n" +
                       " def __init__(self, a, b, c):\n" +
                       "  self.a = a\n" +
                       "  self.b = b\n" +
                       "  self.c = c\n" +
                       " def six(self): return 6\n");

    final PyObject pyType = m_interpreter.get("Foo");
    m_instrumenter.createInstrumentedProxy(m_test, m_recorder, pyType);
    final PyObject result = pyType.__call__(m_two, m_three, m_one);
    assertEquals(m_two, result.__getattr__("a"));
    m_recorderStubFactory.assertSuccess("start");
    m_recorderStubFactory.assertSuccess("end", true);
    m_recorderStubFactory.assertNoMoreCalls();

    // From Jython.
    m_interpreter.set("proxy", pyType);

    m_interpreter.exec("result2 = Foo(1, 2, 3)");
    final PyObject result2 = m_interpreter.get("result2");
    assertEquals(m_two, result2.__getattr__("b"));
    m_recorderStubFactory.assertSuccess("start");
    m_recorderStubFactory.assertSuccess("end", true);
    m_recorderStubFactory.assertNoMoreCalls();

    m_interpreter.exec("result3 = proxy(0, 0, 0)");
    final PyObject result3 = m_interpreter.get("result3");
    assertEquals(m_zero, result3.__getattr__("b"));
    m_recorderStubFactory.assertSuccess("start");
    m_recorderStubFactory.assertSuccess("end", true);
    m_recorderStubFactory.assertNoMoreCalls();

    // Instrumenting a class doesn't instrument methods.
    m_interpreter.exec("result4 = result3.six()");
    assertEquals(m_six, m_interpreter.get("result4"));
    m_recorderStubFactory.assertNoMoreCalls();
  }

  public void testInstrumentationWithPyDerivedClass() throws Exception {
    m_interpreter.exec("from test import MyClass\n" +
                       "class Foo(MyClass):\n" +
                       " def six(self): return 6\n" +
                       "x=Foo()");

    final PyObject pyType = m_interpreter.get("Foo");
    m_instrumenter.createInstrumentedProxy(m_test, m_recorder, pyType);
    final PyObject result = pyType.__call__();
    assertEquals(m_zero, result.invoke("getA"));
    m_recorderStubFactory.assertSuccess("start");
    m_recorderStubFactory.assertSuccess("end", true);
    m_recorderStubFactory.assertNoMoreCalls();

    // From Jython.
    m_interpreter.set("proxy", pyType);

    m_interpreter.exec("result2 = Foo()");
    final PyObject result2 = m_interpreter.get("result2");
    assertEquals(m_zero, result2.invoke("getB"));
    m_recorderStubFactory.assertSuccess("start");
    m_recorderStubFactory.assertSuccess("end", true);
    m_recorderStubFactory.assertNoMoreCalls();

    m_interpreter.exec("result3 = proxy(0, 0, 0)");
    final PyObject result3 = m_interpreter.get("result3");
    assertEquals(m_zero, result3.invoke("getB"));
    m_recorderStubFactory.assertSuccess("start");
    m_recorderStubFactory.assertSuccess("end", true);
    m_recorderStubFactory.assertNoMoreCalls();

    // Instrumenting a class doesn't instrument methods.
    m_interpreter.exec("result4 = result3.six()");
    assertEquals(m_six, m_interpreter.get("result4"));
    m_recorderStubFactory.assertNoMoreCalls();
  }

  public void testInstrumentationWithStaticMethod() throws Exception {
    m_interpreter.exec("from test import MyClass\n" +
                       "x=MyClass.staticSix");

    final PyObject pyType = m_interpreter.get("x");
    m_instrumenter.createInstrumentedProxy(m_test, m_recorder, pyType);
    final PyObject result = pyType.__call__();
    assertEquals(m_six, result);
    m_recorderStubFactory.assertSuccess("start");
    m_recorderStubFactory.assertSuccess("end", true);
    m_recorderStubFactory.assertNoMoreCalls();

    // From Jython.
    m_interpreter.set("proxy", pyType);

    m_interpreter.exec("result2 = MyClass.staticSix()");
    final PyObject result2 = m_interpreter.get("result2");
    assertEquals(m_six, result2);
    m_recorderStubFactory.assertSuccess("start");
    m_recorderStubFactory.assertSuccess("end", true);
    m_recorderStubFactory.assertNoMoreCalls();
  }

  public void testInstrumentationWithReflectedConstructor() throws Exception {
    m_interpreter.exec("from test import MyClass\n" +
                       "x=MyClass.__init__");

    final PyObject myClass = m_interpreter.get("MyClass");
    final PyObject py = m_interpreter.get("x");
    m_instrumenter.createInstrumentedProxy(m_test, m_recorder, py);
    myClass.__call__();
    m_recorderStubFactory.assertSuccess("start");
    m_recorderStubFactory.assertSuccess("end", true);
    m_recorderStubFactory.assertNoMoreCalls();

    // From Jython.
    m_interpreter.exec("MyClass()");
    m_recorderStubFactory.assertSuccess("start");
    m_recorderStubFactory.assertSuccess("end", true);
    m_recorderStubFactory.assertNoMoreCalls();
  }

  public void testInstrumentationWithPyLambda() throws Exception {
    m_interpreter.exec("f=lambda x:x+1");

    final PyObject pyType = m_interpreter.get("f");
    m_instrumenter.createInstrumentedProxy(m_test, m_recorder, pyType);
    final PyObject result = pyType.__call__(m_two);
    assertEquals(m_three, result);
    m_recorderStubFactory.assertSuccess("start");
    m_recorderStubFactory.assertSuccess("end", true);
    m_recorderStubFactory.assertNoMoreCalls();

    // From Jython.
    m_interpreter.set("proxy", pyType);

    m_interpreter.exec("result2 = f(0)");
    final PyObject result2 = m_interpreter.get("result2");
    assertEquals(m_one, result2);
    m_recorderStubFactory.assertSuccess("start");
    m_recorderStubFactory.assertSuccess("end", true);
    m_recorderStubFactory.assertNoMoreCalls();
  }
}
