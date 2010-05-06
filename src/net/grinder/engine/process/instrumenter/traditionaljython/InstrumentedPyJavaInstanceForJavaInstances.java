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

import org.python.core.PyObject;


/**
 * An instrumented <code>PyJavaInstance</code> that is used to wrap Java
 * instances.
 *
 * <p>
 * Plain InstrumentedPyJavaInstanceForPyMethodsAndPyFunctions's work for Jython
 * 2.1. However, in Jython 2.2 the invoke methods use {@link #__findattr__} to
 * determine which method to call, which bypassed our instrumentation. This
 * special version ensures that the methods we hand back are also instrumented
 * correctly.
 * </p>
 *
 * @author Philip Aston
 * @version $Revision: 4073 $
 */
final class InstrumentedPyJavaInstanceForJavaInstances
  extends AbstractInstrumentedPyJavaInstance {

  public InstrumentedPyJavaInstanceForJavaInstances(Test test,
                                                    Object target,
                                                    PyDispatcher dispatcher) {
    super(test, target, dispatcher);
  }

  public PyObject __findattr__(String name) {
    return getInstrumentationHelper().findAttrInstrumentingMethods(name);
  }

  public PyObject invoke(final String name) {
    return getInstrumentationHelper().dispatch(
      new PyDispatcher.Callable() {
        public PyObject call() {
          return InstrumentedPyJavaInstanceForJavaInstances.super.invoke(name);
        }
      }
    );
  }

  public PyObject invoke(final String name, final PyObject arg1) {
    return getInstrumentationHelper().dispatch(
      new PyDispatcher.Callable() {
        public PyObject call() {
          return InstrumentedPyJavaInstanceForJavaInstances.super.invoke(
            name, arg1);
        }
      }
    );
  }

  public PyObject invoke(final String name, final PyObject arg1,
                         final PyObject arg2) {
    return getInstrumentationHelper().dispatch(
      new PyDispatcher.Callable() {
        public PyObject call() {
          return InstrumentedPyJavaInstanceForJavaInstances.super.invoke(
            name, arg1, arg2);
        }
      }
    );
  }
}

