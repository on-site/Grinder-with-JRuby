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

package net.grinder.engine.process.jython;

import java.lang.reflect.Field;

import net.grinder.engine.common.EngineException;
import net.grinder.engine.common.ScriptLocation;
import net.grinder.engine.process.ScriptEngine;

import org.python.core.Py;
import org.python.core.PyException;
import org.python.core.PyInstance;
import org.python.core.PyObject;
import org.python.core.PyString;
import org.python.core.PySystemState;
import org.python.util.PythonInterpreter;


/**
 * Wrap up the context information necessary to invoke a Jython script.
 *
 * Package scope.
 *
 * @author Philip Aston
 * @version $Revision: 4081 $
 */
public final class JythonScriptEngine implements ScriptEngine {
  private static final String TEST_RUNNER_CALLABLE_NAME = "TestRunner";

  private final PySystemState m_systemState;
  private final PythonInterpreter m_interpreter;
  private final JythonVersionAdapter m_versionAdapter;
  private PyObject m_testRunnerFactory;

  /**
   * Constructor for JythonScriptEngine.
   *
   * @throws EngineException If the script engine could not be created.
   */
  public JythonScriptEngine() throws EngineException {

    PySystemState.initialize();
    m_systemState = new PySystemState();
    m_interpreter = new PythonInterpreter(null, m_systemState);
    m_versionAdapter = new JythonVersionAdapter();
  }

  /**
   * Run any process initialisation required by the script. Called once
   * per ScriptEngine instance.
   *
   * @param script The script.
   * @throws EngineException If process initialisation failed.
   */
  public void initialise(ScriptLocation script) throws EngineException {

    m_systemState.path.insert(0,
      new PyString(script.getDirectory().getFile().getPath()));

    try {
      // Run the test script, script does global set up here.
      m_interpreter.execfile(script.getFile().getPath());
    }
    catch (PyException e) {
      throw new JythonScriptExecutionException("initialising test script", e);
    }

    // Find the callable that acts as a factory for test runner instances.
    m_testRunnerFactory = m_interpreter.get(TEST_RUNNER_CALLABLE_NAME);

    if (m_testRunnerFactory == null || !m_testRunnerFactory.isCallable()) {
      throw new JythonScriptExecutionException(
        "There is no callable (class or function) named '" +
        TEST_RUNNER_CALLABLE_NAME + "' in " + script);
    }
  }

  /**
   * {@inheritDoc}
   */
  public WorkerRunnable createWorkerRunnable() throws EngineException {
    return new JythonWorkerRunnable();
  }

  /**
   * {@inheritDoc}
   */
  public WorkerRunnable createWorkerRunnable(Object testRunner)
    throws EngineException {

    if (testRunner instanceof PyObject) {
      final PyObject pyTestRunner = (PyObject) testRunner;

      if (pyTestRunner.isCallable()) {
        return new JythonWorkerRunnable(pyTestRunner);
      }
    }

    throw new JythonScriptExecutionException(
      "testRunner object is not callable");
  }

  /**
   * Shut down the engine.
   *
   * <p>
   * We don't use m_interpreter.cleanup(), which delegates to
   * PySystemState.callExitFunc, as callExitFunc logs problems to stderr.
   * Instead we duplicate the callExitFunc behaviour raise our own exceptions.
   * </p>
   *
   * @throws EngineException
   *           If the engine could not be shut down.
   */
  public void shutdown() throws EngineException {

    final PyObject exitfunc = m_systemState.__findattr__("exitfunc");

    if (exitfunc != null) {
      try {
        exitfunc.__call__();
      }
      catch (PyException e) {
        throw new JythonScriptExecutionException(
          "calling script exit function", e);
      }
    }
  }

  /**
   * Returns a description of the script engine for the log.
   *
   * @return The description.
   */
  public String getDescription() {
    return "Jython " + m_versionAdapter.getVersion();
  }

  /**
   * Wrapper for script's TestRunner.
   */
  private final class JythonWorkerRunnable
    implements ScriptEngine.WorkerRunnable {

    private final PyObject m_testRunner;

    private JythonWorkerRunnable() throws EngineException {
      try {
        // Script does per-thread initialisation here and
        // returns a callable object.
        m_testRunner = m_testRunnerFactory.__call__();
      }
      catch (PyException e) {
        throw new JythonScriptExecutionException(
          "creating per-thread TestRunner object", e);
      }

      if (!m_testRunner.isCallable()) {
        throw new JythonScriptExecutionException(
          "The result of '" + TEST_RUNNER_CALLABLE_NAME +
          "()' is not callable");
      }
    }

    public JythonWorkerRunnable(PyObject testRunner) {
      m_testRunner = testRunner;
    }

    public void run() throws ScriptExecutionException {

      try {
        m_testRunner.__call__();
      }
      catch (PyException e) {
        throw new JythonScriptExecutionException("calling TestRunner", e);
      }
    }

    /**
     * <p>Ensure that if the test runner has a <code>__del__</code>
     * attribute, it is called when the thread is shutdown. Normally
     * Jython defers this to the Java garbage collector, so we might
     * have done something like
     *
     * <blockquote><pre>
     * m_testRunner = null; Runtime.getRuntime().gc();
     *</pre></blockquote>
     *
     * instead. However this would have a number of problems:
     *
     * <ol>
     * <li>Some JVM's may chose not to finalise the test runner in
     * response to <code>gc()</code>.</li>
     * <li><code>__del__</code> would be called by a GC thread.</li>
     * <li>The standard Jython finalizer wrapping around
     * <code>__del__</code> logs to <code>stderr</code>.</li>
     * </ol></p>
     *
     * <p>Instead, we call any <code>__del__</code> ourselves. After
     * calling this method, the <code>PyObject</code> that underlies
     * this class is made invalid.</p>
    */
    public void shutdown() throws ScriptExecutionException {

      final PyObject del = m_testRunner.__findattr__("__del__");

      if (del != null) {
        try {
          del.__call__();
        }
        catch (PyException e) {
          throw new JythonScriptExecutionException(
            "deleting TestRunner instance", e);
        }
        finally {
          // To avoid the (pretty small) chance of the test runner being
          // finalised and __del__ being run twice, we disable it.
          m_versionAdapter.disableDel(m_testRunner);
        }
      }
    }
  }

  /**
   * Work around different the Jython implementations.
   */
  private static class JythonVersionAdapter {

    // The softly spoken Welshman.
    private final PyObject m_dieQuietly = Py.java2py(Object.class);

    private final Field m_instanceClassField;
    private final String m_version;

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

      String version;

      try {

        version = PySystemState.class.getField("version").get(null).toString();
      }
      catch (Exception e) {
        version = "Unknown";
      }

      m_version = version;
    }

    public String getVersion() {
      return m_version;
    }

    public void disableDel(PyObject pyObject) {
      // Unfortunately, Jython caches the __del__ attribute and makes
      // it impossible to turn it off at a class level. Instead we do
      // this:
      try {
        m_instanceClassField.set(pyObject, m_dieQuietly);
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
