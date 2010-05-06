// Copyright (C) 2001 - 2009 Philip Aston
// Copyright (C) 2005 Martin Wagner
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

import java.lang.reflect.Field;

import net.grinder.common.Test;
import net.grinder.engine.common.EngineException;
import net.grinder.engine.process.Instrumenter;
import net.grinder.engine.process.ScriptEngine.Recorder;
import net.grinder.script.NonInstrumentableTypeException;
import net.grinder.script.NotWrappableTypeException;

import org.python.core.PyClass;
import org.python.core.PyFunction;
import org.python.core.PyInstance;
import org.python.core.PyMethod;
import org.python.core.PyObject;
import org.python.core.PyProxy;
import org.python.core.PyReflectedFunction;


/**
 * Wrap up the context information necessary to invoke a Jython script.
 *
 * Package scope.
 *
 * @author Philip Aston
 * @version $Revision: 4057 $
 */
final class TraditionalJythonInstrumenter implements Instrumenter {

  private final JythonVersionAdapter m_versionAdapter;

  /**
   * Constructor for JythonScriptEngine.
   *
   * @throws EngineException If the script engine could not be created.
   */
  public TraditionalJythonInstrumenter() throws EngineException {
    m_versionAdapter = new JythonVersionAdapter();
  }

  public String getDescription() {
    return "traditional Jython instrumenter";
  }

  /**
   * {@inheritDoc}
   */
  public Object createInstrumentedProxy(Test test,
                                        Recorder recorder,
                                        Object o)
    throws NotWrappableTypeException {

    return instrumentObject(test, new PyDispatcher(recorder), o);
  }

  /**
   * {@inheritDoc}
   */
  public boolean instrument(Test test, Recorder recorder, Object target)
    throws NonInstrumentableTypeException {

    throw new NonInstrumentableTypeException(
      "record() is not supported by the Traditional Jython instrumentor");
  }

  /**
   * Create a proxy PyObject that wraps an target object for a test.
   *
   * <p>
   * We could have defined overloaded createProxy methods that take a
   * PyInstance, PyFunction etc., and return decorator PyObjects. There's no
   * obvious way of doing this in a polymorphic way, so we would be forced to
   * have n factories, n types of decorator, and probably run into identity
   * issues. Instead we lean on Jython and force it to give us Java proxy which
   * we then dynamically subclass with our own wrappers.
   * </p>
   *
   * <p>
   * Of course we're only really interested in the things we can invoke in some
   * way. We throw NotWrappableTypeException for the things we don't want to
   * handle.
   * </p>
   *
   * <p>
   * The specialised PyJavaInstance works surprisingly well for everything bar
   * PyInstances. It can't work for PyInstances, because invoking on the
   * PyJavaInstance calls the PyInstance which in turn attempts to call back on
   * the PyJavaInstance. Use specialised PyInstance clone objects to handle this
   * case. We also need to handle PyReflectedFunctions as an exception.
   * </p>
   *
   * <p>
   * Jython 2.2 requires special handling for Java instances, as method
   * invocations are now dispatched by first looking up the method using
   * __findattr__. See {@link InstrumentedPyJavaInstanceForJavaInstances}.
   * </p>
   *
   * <p>
   * There's a subtle difference in the equality semantics of
   * InstrumentedPyInstances and InstrumentedPyJavaInstances.
   * InstrumentedPyInstances do not compare equal to the wrapped objects,
   * whereas due to <code>PyJavaInstance._is()</code> semantics,
   * InstrumentedPyJavaInstances <em>do</em> compare equal to the wrapped
   * objects. We can only influence one side of the comparison (we can't easily
   * alter the <code>_is</code> implementation of wrapped objects) so we can't
   * do anything nice about this.
   * </p>
   *
   * @param test
   *          The test.
   * @param pyDispatcher
   *          The proxy should use this to dispatch the work.
   * @param o
   *          Object to wrap.
   * @return The instrumented proxy.
   * @throws NotWrappableTypeException
   *           If the target cannot be wrapped.
   */
  private PyObject instrumentObject(Test test,
                                    PyDispatcher pyDispatcher,
                                    Object o)
    throws NotWrappableTypeException {

    if (o instanceof PyObject) {
      // Jython object.
      if (o instanceof PyInstance) {
        final PyInstance pyInstance = (PyInstance)o;
        final PyClass pyClass =
          m_versionAdapter.getClassForInstance(pyInstance);
        return new InstrumentedPyInstance(
          test, pyClass, pyInstance, pyDispatcher);
      }
      else if (o instanceof PyFunction) {
        return new InstrumentedPyJavaInstanceForPyFunctions(
          test, (PyFunction)o, pyDispatcher);
      }
      else if (o instanceof PyMethod) {
        return new InstrumentedPyJavaInstanceForPyMethods(
          test, (PyMethod)o, pyDispatcher);
      }
      else if (o instanceof PyReflectedFunction) {
        return new InstrumentedPyReflectedFunction(
          test, (PyReflectedFunction)o, pyDispatcher);
      }
      else {
        // Fail, rather than guess a generic approach.
        throw new NotWrappableTypeException("Unknown PyObject");
      }
    }
    else if (o instanceof PyProxy) {
      // Jython object that extends a Java class.
      final PyInstance pyInstance = ((PyProxy)o)._getPyInstance();
      final PyClass pyClass =
        m_versionAdapter.getClassForInstance(pyInstance);
      return new InstrumentedPyInstance(
        test, pyClass, pyInstance, pyDispatcher);
    }
    else if (o instanceof Class<?>) {
      return new InstrumentedPyJavaClass(test, (Class<?>)o, pyDispatcher);
    }
    else if (o != null) {
      // Java object.

      // NB Jython uses Java types for some primitives and strings.
      if (!o.getClass().isArray() &&
          !(o instanceof Number) &&
          !(o instanceof String)) {
        return new InstrumentedPyJavaInstanceForJavaInstances(
          test, o, pyDispatcher);
      }
    }

    return null;
  }

  /**
   * Work around different the Jython implementations.
   */
  private static class JythonVersionAdapter {
    private final Field m_instanceClassField;

    public JythonVersionAdapter() throws EngineException {
      Field f;

      try {
        // Jython 2.1
        f = PyObject.class.getField("__class__");
      }
      catch (NoSuchFieldException e) {
        // Jython 2.2a1+
        try {
          f = PyInstance.class.getField("instclass");
        }
        catch (NoSuchFieldException e2) {
          throw new EngineException("Incompatible Jython release in classpath");
        }
      }

      m_instanceClassField = f;
    }

    public PyClass getClassForInstance(PyInstance target) {
      try {
        return (PyClass)m_instanceClassField.get(target);
      }
      catch (IllegalArgumentException e) {
        throw new AssertionError(e);
      }
      catch (IllegalAccessException e) {
        throw new AssertionError(e);
      }
    }
  }
}
