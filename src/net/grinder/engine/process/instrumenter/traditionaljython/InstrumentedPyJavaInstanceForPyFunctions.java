// Copyright (C) 2002 - 2009 Philip Aston
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

import org.python.core.PyFunction;
import org.python.core.PyObject;


/**
 * An instrumented <code>PyJavaInstance</code>, used to wrap PyFunctions.
 *
 * @author Philip Aston
 * @version $Revision: 4073 $
 */
class InstrumentedPyJavaInstanceForPyFunctions
  extends AbstractInstrumentedPyJavaInstance {

  private final PyFunction m_pyFunction;

  public InstrumentedPyJavaInstanceForPyFunctions(
    Test test,
    PyFunction pyFunction,
    PyDispatcher dispatcher) {
    super(test, pyFunction, dispatcher);
    m_pyFunction = pyFunction;
  }

  public final PyObject invoke(final String name) {

    if (name == InstrumentationHelper.TARGET_FIELD_NAME) {
      // Under Jython 2.1, wrapped.__target__() comes through this path. Under
      // Jython 2.2, it is dispatched via __find_attr__ and this code path is
      // unnecessary.
      return m_pyFunction.__call__();
    }

    return getInstrumentationHelper().dispatch(
      new PyDispatcher.Callable() {
        public PyObject call() {
          return InstrumentedPyJavaInstanceForPyFunctions
            .super.invoke(name);
        }
      }
    );
  }

  public final PyObject invoke(final String name, final PyObject arg1) {

    if (name == InstrumentationHelper.TARGET_FIELD_NAME) {
      return m_pyFunction.__call__(arg1);
    }

    return getInstrumentationHelper().dispatch(
      new PyDispatcher.Callable() {
        public PyObject call() {
          return InstrumentedPyJavaInstanceForPyFunctions
            .super.invoke(name, arg1);
        }
      }
    );
  }

  public final PyObject invoke(
    final String name, final PyObject arg1, final PyObject arg2) {

    if (name == InstrumentationHelper.TARGET_FIELD_NAME) {
      return m_pyFunction.__call__(arg1, arg2);
    }

    return getInstrumentationHelper().dispatch(
      new PyDispatcher.Callable() {
        public PyObject call() {
          return InstrumentedPyJavaInstanceForPyFunctions
            .super.invoke(name, arg1, arg2);
        }
      }
    );
  }

  public final PyObject invoke(final String name, final PyObject[] args) {

    if (name == InstrumentationHelper.TARGET_FIELD_NAME) {
      return m_pyFunction.__call__(args);
    }

    return getInstrumentationHelper().dispatch(
      new PyDispatcher.Callable() {
          public PyObject call() {
            return InstrumentedPyJavaInstanceForPyFunctions
              .super.invoke(name, args);
          }
      }
    );
  }

  public final PyObject invoke(
    final String name, final PyObject[] args, final String[] keywords) {

    /*
    // Neither Jython 2.1 or 2.2 take this path.
    if (name == InstrumentationHelper.TARGET_FIELD_NAME) {
      return m_pyFunction.__call__(args, keywords);
    }
    */

    return getInstrumentationHelper().dispatch(
      new PyDispatcher.Callable() {
        public PyObject call() {
          return InstrumentedPyJavaInstanceForPyFunctions
            .super.invoke(name, args, keywords);
        }
      }
    );
  }
}

