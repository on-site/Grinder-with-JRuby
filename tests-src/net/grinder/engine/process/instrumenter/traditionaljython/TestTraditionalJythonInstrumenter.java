// Copyright (C) 2005 - 2009 Philip Aston
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

import net.grinder.common.Test;
import net.grinder.engine.process.instrumenter.AbstractJythonInstrumenterTestCase;

import org.python.core.Py;
import org.python.core.PyInstance;
import org.python.core.PyObject;
import org.python.util.PythonInterpreter;

import test.MyClass;
import test.MyExtendedClass;



/**
 * Unit tests for {@link JythonInstrumenter}.
 *
 * @author Philip Aston
 * @version $Revision: 4057 $
 */
public class TestTraditionalJythonInstrumenter
  extends AbstractJythonInstrumenterTestCase {

  public TestTraditionalJythonInstrumenter() throws Exception {
    super(new TraditionalJythonInstrumenter());
  }

  @Override protected void assertTestReference(PyObject pyObject, Test test) {
    assertSame(test, pyObject.__getattr__("__test__").__tojava__(Test.class));
  }

  @Override
  protected void assertTargetReference(PyObject proxy,
                                       Object original,
                                       boolean unwrapTarget) {
    final PyObject targetReference = proxy.__getattr__("__target__");

    final Object target =
      unwrapTarget ? targetReference.__tojava__(Object.class) : targetReference;

    assertSame(original, target);
    assertNotSame(proxy, target);
  }

  public void testCreateProxyWithJavaInstance() throws Exception {
    final Object java = new MyClass();
    final Object extendedJava = new MyExtendedClass();

    final PyObject javaProxy =
      (PyObject)m_instrumenter.createInstrumentedProxy(m_test,
                                                       m_recorder,
                                                       java);
    final PyObject result =
      javaProxy.invoke("addOne", Py.java2py(new Integer(10)));
    assertEquals(new Integer(11), result.__tojava__(Integer.class));
    m_recorderStubFactory.assertSuccess("start");
    m_recorderStubFactory.assertSuccess("end", true);
    m_recorderStubFactory.assertNoMoreCalls();
    assertTestReference(javaProxy, m_test);
    assertTargetReference(javaProxy, java, true);

    final PyObject result2 = javaProxy.invoke("sum", m_one, m_two);
    assertEquals(m_three, result2);
    m_recorderStubFactory.assertSuccess("start");
    m_recorderStubFactory.assertSuccess("end", true);
    m_recorderStubFactory.assertNoMoreCalls();

    final PyObject result3 = javaProxy.invoke("sum3", new PyObject[] { m_one,
        m_two, m_three });
    assertEquals(m_six, result3);
    m_recorderStubFactory.assertSuccess("start");
    m_recorderStubFactory.assertSuccess("end", true);
    m_recorderStubFactory.assertNoMoreCalls();

    final PyObject result4 = javaProxy.invoke("sum", new PyObject[] { m_one,
        m_two }, Py.NoKeywords);
    assertEquals(m_three, result4);
    m_recorderStubFactory.assertSuccess("start");
    m_recorderStubFactory.assertSuccess("end", true);
    m_recorderStubFactory.assertNoMoreCalls();

    // From Jython.
    m_interpreter.set("proxy", javaProxy);

    m_interpreter.exec("result5 = proxy.sum3(0, -29, 30)");
    final PyObject result5 = m_interpreter.get("result5");
    assertEquals(m_one, result5);
    m_recorderStubFactory.assertSuccess("start");
    m_recorderStubFactory.assertSuccess("end", true);
    m_recorderStubFactory.assertNoMoreCalls();

    m_interpreter.exec("result5Cached = proxy.sum3(0, -29, 30)");
    final PyObject result5Cached = m_interpreter.get("result5Cached");
    assertEquals(m_one, result5Cached);
    m_recorderStubFactory.assertSuccess("start");
    m_recorderStubFactory.assertSuccess("end", true);
    m_recorderStubFactory.assertNoMoreCalls();

    m_interpreter.exec("result6 = proxy.sum(1, 1)");
    final PyObject result6 = m_interpreter.get("result6");
    assertEquals(m_two, result6);
    m_recorderStubFactory.assertSuccess("start");
    m_recorderStubFactory.assertSuccess("end", true);
    m_recorderStubFactory.assertNoMoreCalls();

    final PyObject extendedJavaProxy =
      (PyObject)m_instrumenter.createInstrumentedProxy(m_test,
                                                       m_recorder,
                                                       extendedJava);
    final PyObject result7 =
      extendedJavaProxy.invoke("addOne", Py.java2py(new Integer(10)));
    assertEquals(new Integer(11), result7.__tojava__(Integer.class));
    m_recorderStubFactory.assertSuccess("start");
    m_recorderStubFactory.assertSuccess("end", true);
    m_recorderStubFactory.assertNoMoreCalls();
    assertTestReference(extendedJavaProxy, m_test);
    assertTargetReference(extendedJavaProxy, extendedJava, true);
  }

  public void testCreateProxyWithJavaClass() throws Exception {
    final Class<?> javaClass = MyClass.class;
    final PyObject javaProxy =
      (PyObject)m_instrumenter.createInstrumentedProxy(m_test,
                                                       m_recorder,
                                                       javaClass);
    final PyObject result =
      javaProxy.invoke("addTwo", Py.java2py(new Integer(10)));
    assertEquals(new Integer(12), result.__tojava__(Integer.class));
    m_recorderStubFactory.assertSuccess("start");
    m_recorderStubFactory.assertSuccess("end", true);
    m_recorderStubFactory.assertNoMoreCalls();
    assertTestReference(javaProxy, m_test);
    assertTargetReference(javaProxy, javaClass, true);

    final PyObject result1 = javaProxy.invoke("staticSum", m_one, m_two);
    assertEquals(m_three, result1);
    m_recorderStubFactory.assertSuccess("start");
    m_recorderStubFactory.assertSuccess("end", true);
    m_recorderStubFactory.assertNoMoreCalls();

    final PyObject result2 = javaProxy.invoke("staticSum3",
      new PyObject[] { m_one,  m_two, m_three });
    assertEquals(m_six, result2);
    m_recorderStubFactory.assertSuccess("start");
    m_recorderStubFactory.assertSuccess("end", true);
    m_recorderStubFactory.assertNoMoreCalls();

    final PyObject result3 = javaProxy.invoke("staticSum",
      new PyObject[] { m_one, m_two }, Py.NoKeywords);
    assertEquals(m_three, result3);
    m_recorderStubFactory.assertSuccess("start");
    m_recorderStubFactory.assertSuccess("end", true);
    m_recorderStubFactory.assertNoMoreCalls();

    final PyObject result4 = javaProxy.invoke("staticSix");
    assertEquals(m_six, result4);
    m_recorderStubFactory.assertSuccess("start");
    m_recorderStubFactory.assertSuccess("end", true);
    m_recorderStubFactory.assertNoMoreCalls();

    final PyObject instance = javaProxy.__call__(); // Constructor.

    assertEquals(MyClass.class, getClassForInstance((PyInstance) instance));

    m_recorderStubFactory.assertSuccess("start");
    m_recorderStubFactory.assertSuccess("end", true);
    m_recorderStubFactory.assertNoMoreCalls();

    final PyObject instance2 = javaProxy.__call__(
      new PyObject[] { m_one, m_two, m_three, },
      new String[] { "c", "b", "a" }); // Keywords.

    m_recorderStubFactory.assertSuccess("start");
    m_recorderStubFactory.assertSuccess("end", true);

    final MyClass javaInstance2 = (MyClass) instance2.__tojava__(MyClass.class);
    assertEquals(3, javaInstance2.getA());
    assertEquals(2, javaInstance2.getB());
    assertEquals(1, javaInstance2.getC());
    m_recorderStubFactory.assertNoMoreCalls();

    final PyObject instance3 = javaProxy.__call__(m_one, m_two, m_three);
    m_recorderStubFactory.assertSuccess("start");
    m_recorderStubFactory.assertSuccess("end", true);
    final MyClass javaInstance3 = (MyClass) instance3.__tojava__(MyClass.class);
    assertEquals(1, javaInstance3.getA());
    assertEquals(2, javaInstance3.getB());
    assertEquals(3, javaInstance3.getC());
    m_recorderStubFactory.assertNoMoreCalls();

    // From Jython.
    m_interpreter.set("proxy", javaProxy);

    m_interpreter.exec("result5 = proxy.staticSum3(0, -29, 30)");
    final PyObject result5 = m_interpreter.get("result5");
    assertEquals(m_one, result5);
    m_recorderStubFactory.assertSuccess("start");
    m_recorderStubFactory.assertSuccess("end", true);
    m_recorderStubFactory.assertNoMoreCalls();

    m_interpreter.exec("result5Cached = proxy.staticSum3(0, -29, 30)");
    final PyObject result5Cached = m_interpreter.get("result5Cached");
    assertEquals(m_one, result5Cached);
    m_recorderStubFactory.assertSuccess("start");
    m_recorderStubFactory.assertSuccess("end", true);
    m_recorderStubFactory.assertNoMoreCalls();

    m_interpreter.exec("result6 = proxy.staticSum(1, 1)");
    final PyObject result6 = m_interpreter.get("result6");
    assertEquals(m_two, result6);
    m_recorderStubFactory.assertSuccess("start");
    m_recorderStubFactory.assertSuccess("end", true);
    m_recorderStubFactory.assertNoMoreCalls();

    m_interpreter.exec("result7 = proxy.staticSix()");
    final PyObject result7 = m_interpreter.get("result7");
    assertEquals(m_six, result7);
    m_recorderStubFactory.assertSuccess("start");
    m_recorderStubFactory.assertSuccess("end", true);
    m_recorderStubFactory.assertNoMoreCalls();

    m_interpreter.exec("instance = proxy(a=1, c=2, b=3)\nb=instance.b");
    final PyObject result8 = m_interpreter.get("b");
    assertEquals(m_three, result8);

    m_recorderStubFactory.assertSuccess("start");
    m_recorderStubFactory.assertSuccess("end", true);
    m_recorderStubFactory.assertNoMoreCalls();

    m_interpreter.exec("instance = proxy()\n");
    final PyObject result9 = m_interpreter.get("instance");

    assertEquals(MyClass.class, getClassForInstance((PyInstance) result9));

    m_recorderStubFactory.assertSuccess("start");
    m_recorderStubFactory.assertSuccess("end", true);
    m_recorderStubFactory.assertNoMoreCalls();
  }

  public void testCreateProxyWithNonWrappableParameters() throws Exception {

    // The types that can be wrapped depend on the Instrumenter.

    // Can't wrap arrays.
    assertNotWrappableByThisInstrumenter(new int[] { 1, 2, 3 });
    assertNotWrappableByThisInstrumenter(new Object[] { "foo", new Object() });

    // Can't wrap strings.
    assertNotWrappableByThisInstrumenter("foo bah");

    // Can't wrap numbers.
    assertNotWrappableByThisInstrumenter(new Long(56));
    assertNotWrappableByThisInstrumenter(new Integer(56));
    assertNotWrappableByThisInstrumenter(new Short((short) 56));
    assertNotWrappableByThisInstrumenter(new Byte((byte) 56));

    final PythonInterpreter interpreter = getInterpretter();

    // Can't wrap PyInteger.
    interpreter.exec("x=1");
    assertNotWrappable(interpreter.get("x"));

    // Can't wrap PyClass.
    interpreter.exec("class Foo: pass");
    assertNotWrappable(interpreter.get("Foo"));

    // Can't wrap None.
    assertNotWrappableByThisInstrumenter(null);
  }
}
