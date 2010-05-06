// Copyright (C) 2000 Paco Gomez
// Copyright (C) 2000 - 2009 Philip Aston
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

import java.util.HashMap;
import java.util.Map;

import net.grinder.common.FilenameFactory;
import net.grinder.common.GrinderException;
import net.grinder.common.GrinderProperties;
import net.grinder.common.Logger;
import net.grinder.common.SkeletonThreadLifeCycleListener;
import net.grinder.common.processidentity.WorkerIdentity;
import net.grinder.communication.QueuedSender;
import net.grinder.messages.console.ReportStatisticsMessage;
import net.grinder.messages.console.WorkerProcessReportMessage;
import net.grinder.script.Grinder;
import net.grinder.script.InternalScriptContext;
import net.grinder.script.SSLControl;
import net.grinder.script.Statistics;
import net.grinder.statistics.StatisticsServices;
import net.grinder.statistics.TestStatisticsMap;
import net.grinder.util.ListenerSupport;
import net.grinder.util.Sleeper;
import net.grinder.util.SleeperImplementation;
import net.grinder.util.StandardTimeAuthority;
import net.grinder.util.TimeAuthority;
import net.grinder.util.ListenerSupport.Informer;


/**
 * Process wide state.
 *
 * @author Philip Aston
 * @version $Revision: 4147 $
 */
final class ProcessContextImplementation implements ProcessContext {
  private final ListenerSupport<ProcessLifeCycleListener>
    m_processLifeCycleListeners =
      new ListenerSupport<ProcessLifeCycleListener>();

  private final ThreadContexts m_threadContexts = new ThreadContexts();

  private final WorkerIdentity m_workerIdentity;
  private final GrinderProperties m_properties;
  private final Logger m_processLogger;
  private final ThreadContextLocator m_threadContextLocator;
  private final TestRegistryImplementation m_testRegistryImplementation;
  private final InternalScriptContext m_scriptContext;
  private final Sleeper m_sleeper;
  private final StatisticsServices m_statisticsServices;
  private final TestStatisticsHelper m_testStatisticsHelper;
  private final TimeAuthority m_timeAuthority;
  private final boolean m_reportTimesToConsole;

  private volatile long m_executionStartTime;

  ProcessContextImplementation(WorkerIdentity workerIdentity,
                               WorkerIdentity firstWorkerIdentity,
                               GrinderProperties properties,
                               Logger logger,
                               FilenameFactory filenameFactory,
                               QueuedSender consoleSender,
                               StatisticsServices statisticsServices,
                               ThreadStarter threadStarter)
    throws GrinderException {

    m_workerIdentity = workerIdentity;
    m_properties = properties;
    m_processLogger = logger;
    m_statisticsServices = statisticsServices;
    m_threadContextLocator = new ThreadContextLocatorImplementation();
    m_testStatisticsHelper =
      new TestStatisticsHelperImplementation(
        m_statisticsServices.getStatisticsIndexMap());

    m_timeAuthority = new StandardTimeAuthority();

    final Logger externalLogger =
      new ExternalLogger(m_processLogger, m_threadContextLocator);

    final FilenameFactory externalFilenameFactory =
      new ExternalFilenameFactory(filenameFactory, m_threadContextLocator);

    m_sleeper = new SleeperImplementation(
      m_timeAuthority,
      externalLogger,
      properties.getDouble("grinder.sleepTimeFactor", 1.0d),
      properties.getDouble("grinder.sleepTimeVariation", 0.2d));

    final SSLControl sslControl =
      new SSLControlImplementation(m_threadContextLocator);

    final Statistics scriptStatistics =
      new ScriptStatisticsImplementation(m_threadContextLocator,
                                         statisticsServices,
                                         consoleSender);

    m_testRegistryImplementation =
      new TestRegistryImplementation(m_threadContextLocator,
                       statisticsServices.getStatisticsSetFactory(),
                       m_testStatisticsHelper,
                       m_timeAuthority);

    final ThreadStopper threadStopper = new ThreadStopper() {
      public boolean stopThread(int threadNumber) {
        return m_threadContexts.shutdown(threadNumber);
      }
    };

    m_scriptContext = new ScriptContextImplementation(
      m_workerIdentity,
      firstWorkerIdentity,
      m_threadContextLocator,
      properties,
      externalLogger,
      externalFilenameFactory,
      m_sleeper,
      sslControl,
      scriptStatistics,
      m_testRegistryImplementation,
      threadStarter,
      threadStopper);

    final PluginRegistryImplementation pluginRegistry =
      new PluginRegistryImplementation(externalLogger, m_scriptContext,
                                       m_threadContextLocator,
                                       statisticsServices, m_timeAuthority);

    m_processLifeCycleListeners.add(pluginRegistry);

    m_processLifeCycleListeners.add(m_threadContexts);

    m_reportTimesToConsole =
      properties.getBoolean("grinder.reportTimesToConsole", true);

    Grinder.grinder = m_scriptContext;
  }

  public WorkerProcessReportMessage createStatusMessage(
    short state, short numberOfThreads, short totalNumberOfThreads) {

    return new WorkerProcessReportMessage(m_workerIdentity,
                                          state,
                                          numberOfThreads,
                                          totalNumberOfThreads);
  }

  public ReportStatisticsMessage createReportStatisticsMessage(
    TestStatisticsMap sample) {

    if (!m_reportTimesToConsole) {
      m_testStatisticsHelper.removeTestTimeFromSample(sample);
    }

    return new ReportStatisticsMessage(sample);
  }

  public Logger getProcessLogger() {
    return m_processLogger;
  }

  public TestRegistryImplementation getTestRegistry() {
    return m_testRegistryImplementation;
  }

  public GrinderProperties getProperties() {
    return m_properties;
  }

  public ThreadContextLocator getThreadContextLocator() {
    return m_threadContextLocator;
  }

  public void setExecutionStartTime() {
    m_executionStartTime = m_timeAuthority.getTimeInMilliseconds();
  }

  public long getExecutionStartTime() {
    return m_executionStartTime;
  }

  public long getElapsedTime() {
    return m_timeAuthority.getTimeInMilliseconds() - getExecutionStartTime();
  }

  public void shutdown() {
    // Interrupt any sleepers.
    SleeperImplementation.shutdownAllCurrentSleepers();

    m_threadContexts.shutdownAll();
  }

  public Sleeper getSleeper() {
    return m_sleeper;
  }

  public StatisticsServices getStatisticsServices() {
    return m_statisticsServices;
  }

  public void fireThreadCreatedEvent(final ThreadContext threadContext) {
    m_processLifeCycleListeners.apply(new Informer<ProcessLifeCycleListener>() {
      public void inform(ProcessLifeCycleListener listener) {
        listener.threadCreated(threadContext);
      }
    });
  }

  private static final class ThreadContextLocatorImplementation
    implements ThreadContextLocator  {

    private final ThreadLocal<ThreadContext> m_threadContextThreadLocal =
      new ThreadLocal<ThreadContext>();

    public ThreadContext get() {
      return m_threadContextThreadLocal.get();
    }

    public void set(ThreadContext threadContext) {
      m_threadContextThreadLocal.set(threadContext);
    }
  }

  private static final class ThreadContexts
    implements ProcessLifeCycleListener {

    // Guarded by self.
    private final Map<Integer, ThreadContext> m_threadContexts =
      new HashMap<Integer, ThreadContext>();

    // Guarded by m_threadContexts.
    private boolean m_allShutdown;

    public void threadCreated(ThreadContext threadContext) {
      final Integer threadNumber = threadContext.getThreadNumber();

      final boolean shutdown;

      synchronized (m_threadContexts) {
        shutdown = m_allShutdown;

        if (!shutdown) {
          threadContext.registerThreadLifeCycleListener(
            new SkeletonThreadLifeCycleListener() {
              public void endThread() {
                m_threadContexts.remove(threadNumber);
              }
            });

          // Very unlikely, harmless race here - we could store a reference to
          // a thread context that is in the process of shutting down.
          m_threadContexts.put(threadNumber, threadContext);
        }
      }

      if (shutdown) {
        // Stop new threads in their tracks.
        threadContext.shutdown();
      }
    }

    public boolean shutdown(int threadNumber) {
      final ThreadContext threadContext;

      synchronized (m_threadContexts) {
        threadContext = m_threadContexts.get(threadNumber);
      }

      if (threadContext != null) {
        threadContext.shutdown();
        return true;
      }

      return false;
    }

    public void shutdownAll() {
      final ThreadContext[] threadContexts;

      synchronized (m_threadContexts) {
        m_allShutdown = true;

        threadContexts =
          m_threadContexts.values().toArray(
            new ThreadContext[m_threadContexts.size()]);
      }

      for (int i = 0; i < threadContexts.length; ++i) {
        threadContexts[i].shutdown();
      }
    }
  }
}
