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

package net.grinder.engine.process.jython;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.Writer;

import net.grinder.engine.common.EngineException;
import net.grinder.engine.common.ScriptLocation;
import net.grinder.engine.process.ScriptEngine;
import net.grinder.engine.process.ScriptEngine.WorkerRunnable;
import net.grinder.testutility.AbstractFileTestCase;
import net.grinder.testutility.AssertUtilities;
import net.grinder.util.Directory;

import org.python.core.PyObject;
import org.python.core.PySystemState;
import org.python.util.PythonInterpreter;


/**
 * Unit tests for {@link JythonScriptEngine}.
 *
 * @author Philip Aston
 * @version $Revision: 4060 $
 */
public class TestJythonScriptEngine extends AbstractFileTestCase {

  private static Object s_lastCallbackObject;

  {
    PySystemState.initialize();
  }

  private final PythonInterpreter m_interpreter =
    new PythonInterpreter(null, new PySystemState());

  public void testInitialise() throws Exception {
    final JythonScriptEngine scriptEngine = new JythonScriptEngine();

    AssertUtilities.assertContains(scriptEngine.getDescription(), "Jython");

    final File scriptFile = new File(getDirectory(), "script");

    // ScriptLocation with incorrect root directory, so import fails below.
    final ScriptLocation scriptWithIncorrectRoot =
      new ScriptLocation(new Directory(new File("")), scriptFile);

    try {
      scriptEngine.initialise(scriptWithIncorrectRoot);
      fail("Expected JythonScriptExecutionException");
    }
    catch (JythonScriptExecutionException e) {
      AssertUtilities.assertContains(e.getShortMessage(), "IOError");
    }

    assertTrue(scriptFile.createNewFile());

    try {
      scriptEngine.initialise(scriptWithIncorrectRoot);
      fail("Expected EngineException");
    }
    catch (EngineException e) {
      AssertUtilities.assertContains(e.getMessage(), "no callable");
    }

    final Writer w1 = new FileWriter(scriptFile);
    w1.write("TestRunner = 1");
    w1.close();

    try {
      scriptEngine.initialise(scriptWithIncorrectRoot);
      fail("Expected EngineException");
    }
    catch (EngineException e) {
      AssertUtilities.assertContains(e.getMessage(), "no callable");
    }

    final PrintWriter w2 = new PrintWriter(new FileWriter(scriptFile));
    w2.println("class TestRunner:pass");
    w2.close();

    scriptEngine.initialise(scriptWithIncorrectRoot);
    scriptEngine.shutdown();

    final File directory = new File(getDirectory(), "foo");
    assertTrue(directory.mkdirs());
    // new File(directory, "__init__.py").createNewFile();

    final PrintWriter w3 = new PrintWriter(new FileWriter(scriptFile, true));
    w3.println("import foo");
    w3.close();

    try {
      scriptEngine.initialise(scriptWithIncorrectRoot);
      fail("Expected JythonScriptExecutionException");
    }
    catch (JythonScriptExecutionException e) {
      AssertUtilities.assertContains(e.getShortMessage(), "ImportError");
    }

    // Script with correct root directory.
    final ScriptLocation script2 =
      new ScriptLocation(new Directory(getDirectory()), scriptFile);

    // Jython caches modules, so we need to use a fresh interpreter to
    // avoid a repeated import error.
    final ScriptEngine scriptEngine2 = new JythonScriptEngine();
    scriptEngine2.initialise(script2);
    scriptEngine2.shutdown();
  }

  public void testShutdown() throws Exception {
    final JythonScriptEngine scriptEngine = new JythonScriptEngine();

    final ScriptLocation script =
      new ScriptLocation(new Directory(getDirectory()),
                         new File(getDirectory(), "script"));

    final PrintWriter w1 = new PrintWriter(new FileWriter(script.getFile()));
    w1.println("class TestRunner:pass");
    w1.close();

    scriptEngine.initialise(script);
    scriptEngine.shutdown();

    callback(null);

    final PrintWriter w2 = new PrintWriter(new FileWriter(script.getFile()));
    w2.println("from net.grinder.engine.process.jython import TestJythonScriptEngine");
    w2.println("import sys");

    w2.println("def f():");
    w2.println(" TestJythonScriptEngine.callback(TestJythonScriptEngine)");
    w2.println("sys.exitfunc = f");

    w2.println("class TestRunner:pass");
    w2.close();

    scriptEngine.initialise(script);
    scriptEngine.shutdown();

    assertSame(TestJythonScriptEngine.class, s_lastCallbackObject);

    s_lastCallbackObject = null;

    final PrintWriter w3 = new PrintWriter(new FileWriter(script.getFile()));
    w3.println("import sys");

    w3.println("def f(): raise 'a problem'");
    w3.println("sys.exitfunc = f");

    w3.println("class TestRunner:pass");
    w3.close();

    scriptEngine.initialise(script);

    try {
      scriptEngine.shutdown();
      fail("Expected JythonScriptExecutionException");
    }
    catch (JythonScriptExecutionException e) {
      AssertUtilities.assertContains(e.getShortMessage(), "a problem");
    }
  }

  public void testWorkerRunnable() throws Exception {
    final JythonScriptEngine scriptEngine = new JythonScriptEngine();

    final ScriptLocation script =
      new ScriptLocation(new Directory(getDirectory()),
                         new File(getDirectory(), "script"));

    final PrintWriter w1 = new PrintWriter(new FileWriter(script.getFile()));
    w1.println("class TestRunner:pass");
    w1.close();

    scriptEngine.initialise(script);

    try {
      scriptEngine.createWorkerRunnable();
      fail("Expected EngineException");
    }
    catch (EngineException e) {
      AssertUtilities.assertContains(e.getMessage(), "is not callable");
    }

    final PrintWriter w2 = new PrintWriter(new FileWriter(script.getFile()));
    w2.println("class TestRunner:");
    w2.println(" def __init__(self): raise 'a problem'");
    w2.close();

    scriptEngine.initialise(script);

    try {
      scriptEngine.createWorkerRunnable();
      fail("Expected JythonScriptExecutionException");
    }
    catch (JythonScriptExecutionException e) {
      AssertUtilities.assertContains(e.getShortMessage(), "a problem");
    }

    final PrintWriter w3 = new PrintWriter(new FileWriter(script.getFile()));
    w3.println("class TestRunner:");
    w3.println(" def __call__(self): pass");
    w3.close();

    scriptEngine.initialise(script);
    final WorkerRunnable runnable3a = scriptEngine.createWorkerRunnable();
    final WorkerRunnable runnable3b = scriptEngine.createWorkerRunnable();
    assertNotSame(runnable3a, runnable3b);
    runnable3a.run();
    runnable3b.run();

    runnable3a.shutdown();

    final PrintWriter w4 = new PrintWriter(new FileWriter(script.getFile()));
    w4.println("class TestRunner:");
    w4.println(" def __call__(self): raise 'a problem'");
    w4.close();

    scriptEngine.initialise(script);
    final WorkerRunnable runnable4 = scriptEngine.createWorkerRunnable();

    try {
      runnable4.run();
      fail("Expected JythonScriptExecutionException");
    }
    catch (JythonScriptExecutionException e) {
      AssertUtilities.assertContains(e.getShortMessage(), "a problem");
    }

    final PrintWriter w5 = new PrintWriter(new FileWriter(script.getFile()));
    w5.println("class TestRunner:");
    w5.println(" def __call__(self): pass");
    w5.println(" def __del__(self): raise 'a problem'");
    w5.close();

    scriptEngine.initialise(script);
    final WorkerRunnable runnable5 = scriptEngine.createWorkerRunnable();

    try {
      runnable5.shutdown();
      fail("Expected JythonScriptExecutionException");
    }
    catch (JythonScriptExecutionException e) {
      AssertUtilities.assertContains(e.getShortMessage(), "a problem");
    }

    // Try it again, __del__ should now be disabled.
    runnable5.shutdown();
  }


  public void testNewWorkerRunnableWithTestRunner() throws Exception {
    final JythonScriptEngine scriptEngine = new JythonScriptEngine();

    final ScriptLocation script =
      new ScriptLocation(new Directory(getDirectory()),
                         new File(getDirectory(), "script"));

    final PrintWriter w1 = new PrintWriter(new FileWriter(script.getFile()));
    w1.println("class TestRunner: pass");
    w1.close();

    scriptEngine.initialise(script);

    try {
      scriptEngine.createWorkerRunnable(null);
      fail("Expected JythonScriptExecutionException");
    }
    catch (JythonScriptExecutionException e) {
      AssertUtilities.assertContains(e.getMessage(), "is not callable");
    }

    final Object badRunner = new Object();

    try {
      scriptEngine.createWorkerRunnable(badRunner);
      fail("Expected JythonScriptExecutionException");
    }
    catch (JythonScriptExecutionException e) {
      AssertUtilities.assertContains(e.getMessage(), "is not callable");
    }

    m_interpreter.exec("result=1");
    m_interpreter.exec("def myRunner():\n global result\n result=99");
    final PyObject goodRunner = m_interpreter.get("myRunner");

    final WorkerRunnable workerRunnable =
      scriptEngine.createWorkerRunnable(goodRunner);

    assertEquals("1", m_interpreter.get("result").toString());

    workerRunnable.run();
    assertEquals("99", m_interpreter.get("result").toString());

    final PyObject badRunner2 =  m_interpreter.get("result");

    try {
      scriptEngine.createWorkerRunnable(badRunner2);
      fail("Expected JythonScriptExecutionException");
    }
    catch (JythonScriptExecutionException e) {
      AssertUtilities.assertContains(e.getMessage(), "is not callable");
    }
  }

  public static void callback(Object o) {
    s_lastCallbackObject = o;
  }
}
