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

package net.grinder.engine.process;

import net.grinder.common.GrinderProperties;
import net.grinder.common.ThreadLifeCycleListener;
import net.grinder.engine.common.EngineException;
import net.grinder.engine.process.ScriptEngine.WorkerRunnable;
import net.grinder.statistics.StatisticsServices;
import net.grinder.statistics.StatisticsServicesImplementation;
import net.grinder.testutility.AbstractFileTestCase;
import net.grinder.testutility.RandomStubFactory;
import net.grinder.util.Sleeper;


/**
 * Unit tests for {@link GrinderThread}.
 *
 * @author Philip Aston
 * @version $Revision: 4006 $
 */
public class TestGrinderThread extends AbstractFileTestCase {

  private final RandomStubFactory<WorkerThreadSynchronisation>
    m_workerThreadSynchronisationStubFactory =
      RandomStubFactory.create(WorkerThreadSynchronisation.class);
  private final WorkerThreadSynchronisation m_workerThreadSynchronisation =
    m_workerThreadSynchronisationStubFactory.getStub();

  private final RandomStubFactory<Sleeper> m_sleeperStubFactory =
    RandomStubFactory.create(Sleeper.class);
  private final Sleeper m_sleeper =
    m_sleeperStubFactory.getStub();

  private final GrinderProperties m_properties = new GrinderProperties();

  private final StatisticsServices m_statisticsServices =
    StatisticsServicesImplementation.getInstance();

  private final RandomStubFactory<ProcessContext> m_processContextStubFactory =
    RandomStubFactory.create(ProcessContext.class);
  private final ProcessContext m_processContext =
    m_processContextStubFactory.getStub();

  {
    m_processContextStubFactory.setResult("getProperties", m_properties);
    m_processContextStubFactory.setResult("getStatisticsServices",
                                          m_statisticsServices);
    m_processContextStubFactory.setResult("getSleeper", m_sleeper);
    m_processContextStubFactory.setIgnoreMethod("getProperties");
    m_processContextStubFactory.setIgnoreMethod("getStatisticsServices");
    m_processContextStubFactory.setIgnoreMethod("getSleeper");
  }

  private final RandomStubFactory<ScriptEngine> m_scriptEngineStubFactory =
    RandomStubFactory.create (ScriptEngine.class);
  private final ScriptEngine scriptEngine =
    m_scriptEngineStubFactory.getStub();

  final LoggerImplementation m_loggerImplementation;

  public TestGrinderThread() throws Exception {
    m_loggerImplementation =
      new LoggerImplementation("grinderid", getDirectory().getPath(), false, 0);
  }

  public void testConstruction() throws Exception {
    new GrinderThread(m_workerThreadSynchronisation,
                      m_processContext,
                      m_loggerImplementation,
                      scriptEngine,
                      3,
                      null);

    m_workerThreadSynchronisationStubFactory.assertSuccess("threadCreated");

    final ThreadContext threadContext =
      (ThreadContext)m_processContextStubFactory.assertSuccess(
        "fireThreadCreatedEvent", ThreadContext.class).getParameters()[0];
    m_processContextStubFactory.assertNoMoreCalls();

    assertEquals(3, threadContext.getThreadNumber());
  }

  public void testRun() throws Exception {
    final GrinderThread grinderThread =
      new GrinderThread(m_workerThreadSynchronisation,
                        m_processContext,
                        m_loggerImplementation,
                        scriptEngine,
                        3,
                        null);

    m_workerThreadSynchronisationStubFactory.assertSuccess("threadCreated");

    final ThreadContext threadContext =
      (ThreadContext)m_processContextStubFactory.assertSuccess(
        "fireThreadCreatedEvent", ThreadContext.class).getParameters()[0];
    m_processContextStubFactory.assertNoMoreCalls();


    final RandomStubFactory<ThreadLifeCycleListener>
      threadLifeCycleListenerStubFactory =
        RandomStubFactory.create(ThreadLifeCycleListener.class);
    threadContext.registerThreadLifeCycleListener(
      threadLifeCycleListenerStubFactory.getStub());

    grinderThread.run();

    m_processContextStubFactory.assertSuccess("getThreadContextLocator");
    m_processContextStubFactory.assertNoMoreCalls();

    threadLifeCycleListenerStubFactory.assertSuccess("beginThread");
    threadLifeCycleListenerStubFactory.assertSuccess("beginRun");
    threadLifeCycleListenerStubFactory.assertSuccess("endRun");
    threadLifeCycleListenerStubFactory.assertSuccess("beginShutdown");
    threadLifeCycleListenerStubFactory.assertSuccess("endThread");
    threadLifeCycleListenerStubFactory.assertNoMoreCalls();

    m_workerThreadSynchronisationStubFactory.assertSuccess("awaitStart");
    m_workerThreadSynchronisationStubFactory.assertSuccess("threadFinished");
    m_workerThreadSynchronisationStubFactory.assertNoMoreCalls();

    m_sleeperStubFactory.assertSuccess("sleepFlat", new Long(0));
    m_sleeperStubFactory.assertNoMoreCalls();


    m_properties.setInt("grinder.runs", 2);
    m_properties.setLong("grinder.initialSleepTime", 100);

    grinderThread.run();

    threadLifeCycleListenerStubFactory.assertSuccess("beginThread");
    threadLifeCycleListenerStubFactory.assertSuccess("beginRun");
    threadLifeCycleListenerStubFactory.assertSuccess("endRun");
    threadLifeCycleListenerStubFactory.assertSuccess("beginRun");
    threadLifeCycleListenerStubFactory.assertSuccess("endRun");
    threadLifeCycleListenerStubFactory.assertSuccess("beginShutdown");
    threadLifeCycleListenerStubFactory.assertSuccess("endThread");
    threadLifeCycleListenerStubFactory.assertNoMoreCalls();

    m_workerThreadSynchronisationStubFactory.assertSuccess("awaitStart");
    m_workerThreadSynchronisationStubFactory.assertSuccess("threadFinished");
    m_workerThreadSynchronisationStubFactory.assertNoMoreCalls();

    m_sleeperStubFactory.assertSuccess("sleepFlat", new Long(100));
    m_sleeperStubFactory.assertNoMoreCalls();



    final RandomStubFactory<WorkerRunnable> workerRunnableStubFactory =
      RandomStubFactory.create(ScriptEngine.WorkerRunnable.class);

    m_properties.setInt("grinder.runs", 0);
    m_scriptEngineStubFactory.setResult("createWorkerRunnable",
      workerRunnableStubFactory.getStub());
    workerRunnableStubFactory.setThrows("run",
      new MyScriptEngineException(new ShutdownException("bye")));

    grinderThread.run();

    threadLifeCycleListenerStubFactory.assertSuccess("beginThread");
    threadLifeCycleListenerStubFactory.assertSuccess("beginRun");
    threadLifeCycleListenerStubFactory.assertSuccess("beginShutdown");
    threadLifeCycleListenerStubFactory.assertSuccess("endThread");
    threadLifeCycleListenerStubFactory.assertNoMoreCalls();

    m_workerThreadSynchronisationStubFactory.assertSuccess("awaitStart");
    m_workerThreadSynchronisationStubFactory.assertSuccess("threadFinished");
    m_workerThreadSynchronisationStubFactory.assertNoMoreCalls();


    m_properties.setInt("grinder.runs", 1);
    workerRunnableStubFactory.setThrows("run",
      new MyScriptEngineException("whatever"));
    workerRunnableStubFactory.setThrows("shutdown",
      new MyScriptEngineException("whatever"));

    grinderThread.run();

    threadLifeCycleListenerStubFactory.assertSuccess("beginThread");
    threadLifeCycleListenerStubFactory.assertSuccess("beginRun");
    threadLifeCycleListenerStubFactory.assertSuccess("endRun");
    threadLifeCycleListenerStubFactory.assertSuccess("beginShutdown");
    threadLifeCycleListenerStubFactory.assertSuccess("endThread");
    threadLifeCycleListenerStubFactory.assertNoMoreCalls();

    m_workerThreadSynchronisationStubFactory.assertSuccess("awaitStart");
    m_workerThreadSynchronisationStubFactory.assertSuccess("threadFinished");
    m_workerThreadSynchronisationStubFactory.assertNoMoreCalls();


    m_scriptEngineStubFactory.setThrows("createWorkerRunnable",
      new MyScriptEngineException("blah"));
    grinderThread.run();

    threadLifeCycleListenerStubFactory.assertSuccess("beginThread");
    threadLifeCycleListenerStubFactory.assertNoMoreCalls();

    m_workerThreadSynchronisationStubFactory.assertSuccess("threadFinished");
    m_workerThreadSynchronisationStubFactory.assertNoMoreCalls();


    m_scriptEngineStubFactory.setThrows("createWorkerRunnable",
      new EngineException("blah"));
    grinderThread.run();

    threadLifeCycleListenerStubFactory.assertSuccess("beginThread");
    threadLifeCycleListenerStubFactory.assertNoMoreCalls();

    m_workerThreadSynchronisationStubFactory.assertSuccess("threadFinished");
    m_workerThreadSynchronisationStubFactory.assertNoMoreCalls();
  }

  public void testRunWithWorkerRunnable() throws Exception {
    final RandomStubFactory<WorkerRunnable> workerRunnableStubFactory =
      RandomStubFactory.create(WorkerRunnable.class);

    final GrinderThread grinderThread =
      new GrinderThread(m_workerThreadSynchronisation,
                        m_processContext,
                        m_loggerImplementation,
                        scriptEngine,
                        3,
                        workerRunnableStubFactory.getStub());

    m_workerThreadSynchronisationStubFactory.assertSuccess("threadCreated");

    final ThreadContext threadContext =
      (ThreadContext)m_processContextStubFactory.assertSuccess(
        "fireThreadCreatedEvent", ThreadContext.class).getParameters()[0];
    m_processContextStubFactory.assertNoMoreCalls();

    final RandomStubFactory<ThreadLifeCycleListener>
      threadLifeCycleListenerStubFactory =
        RandomStubFactory.create(ThreadLifeCycleListener.class);
    threadContext.registerThreadLifeCycleListener(
      threadLifeCycleListenerStubFactory.getStub());

    grinderThread.run();

    m_processContextStubFactory.assertSuccess("getThreadContextLocator");
    m_processContextStubFactory.assertNoMoreCalls();

    threadLifeCycleListenerStubFactory.assertSuccess("beginThread");
    threadLifeCycleListenerStubFactory.assertSuccess("beginRun");
    threadLifeCycleListenerStubFactory.assertSuccess("endRun");
    threadLifeCycleListenerStubFactory.assertSuccess("beginShutdown");
    threadLifeCycleListenerStubFactory.assertSuccess("endThread");
    threadLifeCycleListenerStubFactory.assertNoMoreCalls();

    workerRunnableStubFactory.assertSuccess("run");
    workerRunnableStubFactory.assertSuccess("shutdown");
    workerRunnableStubFactory.assertNoMoreCalls();
  }

  private static final class MyScriptEngineException
    extends ScriptEngine.ScriptExecutionException {
    public MyScriptEngineException(Throwable t) {
      super("whoops", t);
    }

    public MyScriptEngineException(String message) {
      super(message);
    }

    public String getShortMessage() {
      return "";
    }
  }
}
