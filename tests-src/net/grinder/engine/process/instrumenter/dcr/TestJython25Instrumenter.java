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

package net.grinder.engine.process.instrumenter.dcr;

import org.python.core.PyObject;
import org.python.core.PyProxy;

import junit.framework.TestSuite;
import net.grinder.testutility.BlockingClassLoader;
import net.grinder.testutility.RandomStubFactory;
import net.grinder.util.weave.Weaver;


/**
 * Unit tests for {@link JythonInstrumenter}.
 *
 * @author Philip Aston
 * @version $Revision: 4057 $
 */
public class TestJython25Instrumenter
  extends AbstractJythonDCRInstrumenterTestCase {

  private static final Weaver s_weaver = createWeaver();

  public TestJython25Instrumenter() throws Exception {
    super(new Jython25Instrumenter(s_weaver,
                                   RecorderLocator.getRecorderRegistry()));
  }

  public static TestSuite suite() throws Exception {

    final String oldPythonHome = System.getProperty("python.home");

    System.setProperty("python.home",
                       System.getProperty("jython25.dir"));

    try {
      return new TestSuite(
        BlockingClassLoader.createJython25ClassLoader().loadClass(
          TestJython25Instrumenter.class.getName()));
    }
    finally {
      if (oldPythonHome != null) {
        System.setProperty("python.home", oldPythonHome);
      }
    }
  }

  public void testVersion() throws Exception {
    assertVersion("2.5");
  }

  public void testBrokenPyProxy() throws Exception {
    final RandomStubFactory<PyProxy> pyProxyStubFactory =
      RandomStubFactory.create(PyProxy.class);
    final PyProxy pyProxy = pyProxyStubFactory.getStub();

    assertNotWrappable(pyProxy);
  }


  public void testCreateProxyWithJavaClassAnd__call__() throws Exception {
    m_interpreter.exec("from test import MyClass");
    final PyObject pyJavaType = m_interpreter.get("MyClass");
    m_instrumenter.createInstrumentedProxy(m_test, m_recorder, pyJavaType);

    m_interpreter.exec("result2 = MyClass.__call__()");
    final PyObject result2 = m_interpreter.get("result2");
    assertEquals(m_zero, result2.invoke("getA"));
    m_recorderStubFactory.assertSuccess("start");
    m_recorderStubFactory.assertSuccess("end", true);
    m_recorderStubFactory.assertNoMoreCalls();
  }
}
