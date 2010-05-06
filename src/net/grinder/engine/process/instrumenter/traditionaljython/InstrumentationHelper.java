// Copyright (C) 2008 - 2009 Philip Aston
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

import org.python.core.Py;
import org.python.core.PyJavaInstance;
import org.python.core.PyMethod;
import org.python.core.PyObject;

import net.grinder.common.Test;


/**
 * Common instrumentation logic.
 *
 * @author Philip Aston
 * @version $Revision: 4073 $
 */
abstract class InstrumentationHelper {
  /** The field name that allows the test to be obtained from a proxy. */
  private static final String TEST_FIELD_NAME = "__test__";

  /** The field name that allows the target to be obtained from a proxy. */
  static final String TARGET_FIELD_NAME = "__target__";

  private final Test m_test;
  private final PyJavaInstance m_pyTest;
  private final PyObject m_target;
  private final PyDispatcher m_dispatcher;

  public InstrumentationHelper(Test test,
                               Object target,
                               PyDispatcher dispatcher) {
    m_test = test;
    m_pyTest = new PyJavaInstance(test);
    m_target = Py.java2py(target);
    m_dispatcher = dispatcher;
  }

  public PyObject findAttr(String name) {
    if (name == TEST_FIELD_NAME) { // Valid because name is interned.
      return m_pyTest;
    }

    if (name == TARGET_FIELD_NAME) {
      return m_target;
    }

    return doFindAttr(name);
  }

  public PyObject findAttrInstrumentingMethods(String name) {
    if (name == TEST_FIELD_NAME) {
      return m_pyTest;
    }

    if (name == TARGET_FIELD_NAME) {
      return m_target;
    }

    final PyObject unadorned = doFindAttr(name);

    if (unadorned instanceof PyMethod) {
      // We create new instrumentation every time.
      //
      // At one point, we cached the instrumented method. However, Jython
      // doesn't cache the underlying PyMethod (it creates a new PyMethod
      // whenever one is asked for), so its hard/too expensive to check the
      // cached value is correct.
      //
      // Another bad idea was to call __setattr__ with the instrumented
      // method, and use our dictionary as the cache. We share our
      // dictionary with our target, so invocations on the target ended up
      // being instrumented.
      return new InstrumentedPyJavaInstanceForPyMethods(
        m_test, (PyMethod)unadorned, m_dispatcher);
    }

    return unadorned;
  }

  public PyObject dispatch(PyDispatcher.Callable callable) {
    return m_dispatcher.dispatch(callable);
  }

  protected abstract PyObject doFindAttr(String name);
}
