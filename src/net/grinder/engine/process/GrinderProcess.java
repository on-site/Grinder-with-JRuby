// Copyright (C) 2000 Paco Gomez
// Copyright (C) 2000 - 2010 Philip Aston
// Copyright (C) 2003 Kalyanaraman Venkatasubramaniy
// Copyright (C) 2004 Slavik Gnatenko
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

import java.io.PrintWriter;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import net.grinder.common.GrinderBuild;
import net.grinder.common.GrinderException;
import net.grinder.common.GrinderProperties;
import net.grinder.common.Logger;
import net.grinder.common.Test;
import net.grinder.common.processidentity.WorkerProcessReport;
import net.grinder.communication.ClientSender;
import net.grinder.communication.CommunicationException;
import net.grinder.communication.ConnectionType;
import net.grinder.communication.Message;
import net.grinder.communication.MessageDispatchSender;
import net.grinder.communication.MessagePump;
import net.grinder.communication.QueuedSender;
import net.grinder.communication.QueuedSenderDecorator;
import net.grinder.communication.Receiver;
import net.grinder.engine.common.ConnectorFactory;
import net.grinder.engine.common.EngineException;
import net.grinder.engine.communication.ConsoleListener;
import net.grinder.engine.messages.InitialiseGrinderMessage;
import net.grinder.engine.process.instrumenter.MasterInstrumenter;
import net.grinder.engine.process.multilingual.MultilingualScriptEngine;
import net.grinder.messages.console.RegisterTestsMessage;
import net.grinder.script.InvalidContextException;
import net.grinder.statistics.ExpressionView;
import net.grinder.statistics.StatisticsServices;
import net.grinder.statistics.StatisticsServicesImplementation;
import net.grinder.statistics.StatisticsSet;
import net.grinder.statistics.StatisticsTable;
import net.grinder.statistics.TestStatisticsMap;
import net.grinder.statistics.StatisticsIndexMap.LongIndex;
import net.grinder.util.JVM;
import net.grinder.util.thread.BooleanCondition;
import net.grinder.util.thread.Condition;


/**
 * The controller for a worker process.
 *
 * <p>Package scope.</p>
 *
 * @author Paco Gomez
 * @author Philip Aston
 * @version $Revision: 4234 $
 * @see net.grinder.engine.process.GrinderThread
 */
final class GrinderProcess {

  private final ProcessContext m_context;
  private final QueuedSender m_consoleSender;
  private final LoggerImplementation m_loggerImplementation;
  private final InitialiseGrinderMessage m_initialisationMessage;
  private final ConsoleListener m_consoleListener;
  private final TestStatisticsMap m_accumulatedStatistics;
  private final Condition m_eventSynchronisation = new Condition();
  private final MessagePump m_messagePump;

  private final ThreadStarter m_invalidThreadStarter =
    new InvalidThreadStarter();

  // Guarded by m_eventSynchronisation.
  private ThreadStarter m_threadStarter = m_invalidThreadStarter;

  private boolean m_shutdownTriggered;
  private boolean m_communicationShutdown;

  /**
   * Creates a new <code>GrinderProcess</code> instance.
   *
   * @param agentReceiver
   *          Receiver used to listen to the agent.
   * @exception GrinderException
   *          If the process could not be created.
   */
  public GrinderProcess(Receiver agentReceiver) throws GrinderException {

    m_initialisationMessage =
      (InitialiseGrinderMessage)agentReceiver.waitForMessage();

    if (m_initialisationMessage == null) {
      throw new EngineException("No control stream from agent");
    }

    final GrinderProperties properties =
      m_initialisationMessage.getProperties();

    m_loggerImplementation = new LoggerImplementation(
      m_initialisationMessage.getWorkerIdentity().getName(),
      properties.getProperty(GrinderProperties.LOG_DIRECTORY, "."),
      properties.getBoolean("grinder.logProcessStreams", true),
      properties.getInt("grinder.numberOfOldLogs", 1));

    final Logger processLogger = m_loggerImplementation.getProcessLogger();
    processLogger.output("The Grinder version " +
                         GrinderBuild.getVersionString());
    processLogger.output(JVM.getInstance().toString());
    processLogger.output("time zone is " +
                         new SimpleDateFormat("z (Z)").format(new Date()));

    if (m_initialisationMessage.getReportToConsole()) {
      m_consoleSender =
        new QueuedSenderDecorator(
          ClientSender.connect(
            new ConnectorFactory(ConnectionType.WORKER).create(properties)));
    }
    else {
      // Null Sender implementation.
      m_consoleSender = new QueuedSender() {
          public void send(Message message) { }
          public void flush() { }
          public void queue(Message message) { }
          public void shutdown() { }
        };
    }

    final ThreadStarter delegatingThreadStarter = new ThreadStarter() {
      public int startThread(Object testRunner)
        throws EngineException, InvalidContextException {

        final ThreadStarter threadStarter;

        synchronized (m_eventSynchronisation) {
          threadStarter = m_threadStarter;
        }

        return threadStarter.startThread(testRunner);
      }
    };

    m_context =
      new ProcessContextImplementation(
        m_initialisationMessage.getWorkerIdentity(),
        m_initialisationMessage.getFirstWorkerIdentity(),
        properties,
        processLogger,
        m_loggerImplementation.getFilenameFactory(),
        m_consoleSender,
        StatisticsServicesImplementation.getInstance(),
        delegatingThreadStarter);

    // If we don't call getLocalHost() before spawning our
    // ConsoleListener thread, any attempt to call it afterwards will
    // silently crash the JVM. Reproduced with both J2SE 1.3.1-b02 and
    // J2SE 1.4.1_03-b02 on W2K. Do not ask me why, I've stopped
    // caring.
    try { java.net.InetAddress.getLocalHost(); }
    catch (UnknownHostException e) { /* Ignore */ }

    m_consoleListener =
      new ConsoleListener(m_eventSynchronisation, processLogger);
    m_accumulatedStatistics =
      new TestStatisticsMap(
        m_context.getStatisticsServices().getStatisticsSetFactory());

    final MessageDispatchSender messageDispatcher = new MessageDispatchSender();
    m_consoleListener.registerMessageHandlers(messageDispatcher);
    m_messagePump = new MessagePump(agentReceiver, messageDispatcher, 1);
  }

  /**
   * The application's main loop. This is split from the constructor as
   * theoretically it might be called multiple times. The constructor sets up
   * the static configuration, this does a single execution.
   *
   * <p>
   * This method is interruptible, in the same sense as
   * {@link net.grinder.util.thread.InterruptibleRunnable#interruptibleRun()}.
   * We don't implement that method because we want to be able to throw
   * exceptions.
   * </p>
   *
   * @throws GrinderException
   *           If something went wrong.
   */
  public void run() throws GrinderException {
    final Logger logger = m_context.getProcessLogger();

    final Timer timer = new Timer(true);
    timer.schedule(new TickLoggerTimerTask(), 0, 1000);

    final ScriptEngine scriptEngine = new MultilingualScriptEngine();

    // Don't start the message pump until we've initialised Jython. Jython 2.5+
    // tests to see whether the stdin stream is a tty, and on some versions of
    // Windows, this synchronises on the stream object's monitor. This clashes
    // with the message pump which starts a thread to call
    // StreamRecevier.waitForMessage(), and so also synchronises on that
    // monitor. See bug 2936167.
    m_messagePump.start();

    final StringBuffer numbers = new StringBuffer("worker process ");
    numbers.append(m_initialisationMessage.getWorkerIdentity().getNumber());

    final int agentNumber =
      m_initialisationMessage
      .getWorkerIdentity().getAgentIdentity().getNumber();

    if (agentNumber >= 0) {
      numbers.append(" of agent number ");
      numbers.append(agentNumber);
    }

    logger.output(numbers.toString());

    final GrinderProperties properties = m_context.getProperties();
    final short numberOfThreads =
      properties.getShort("grinder.threads", (short)1);
    final int reportToConsoleInterval =
      properties.getInt("grinder.reportToConsole.interval", 500);
    final int duration = properties.getInt("grinder.duration", 0);

    final MasterInstrumenter instrumenter =
      new MasterInstrumenter(
        logger,
        // This property name is poor, since it really means "If DCR
        // instrumentation is available, use it for Jython". I'm not
        // renaming it, since I expect it only to last a few releases,
        // until DCR becomes the default.
        properties.getBoolean("grinder.dcrinstrumentation", false));

    m_context.getTestRegistry().setInstrumenter(instrumenter);

    logger.output("executing \"" + m_initialisationMessage.getScript() +
      "\" using " + scriptEngine.getDescription());

    scriptEngine.initialise(m_initialisationMessage.getScript());

    // Don't initialise the data writer until now as the script may
    // declare new statistics.
    final PrintWriter dataWriter = m_loggerImplementation.getDataWriter();

    dataWriter.print("Thread, Run, Test, Start time (ms since Epoch)");

    final StatisticsServices statisticsServices =
      m_context.getStatisticsServices();

    final ExpressionView[] detailExpressionViews =
      statisticsServices.getDetailStatisticsView().getExpressionViews();

    for (int i = 0; i < detailExpressionViews.length; ++i) {
      dataWriter.print(", " + detailExpressionViews[i].getDisplayName());
    }

    dataWriter.println();

    m_consoleSender.send(
      m_context.createStatusMessage(
        WorkerProcessReport.STATE_STARTED, (short)0, numberOfThreads));

    final ThreadSynchronisation threadSynchronisation =
      new ThreadSynchronisation(m_eventSynchronisation);

    logger.output("starting threads", Logger.LOG | Logger.TERMINAL);

    synchronized (m_eventSynchronisation) {
      m_threadStarter =
        new ThreadStarterImplementation(threadSynchronisation, scriptEngine);

      for (int i = 0; i < numberOfThreads; i++) {
        m_threadStarter.startThread(null);
      }
    }

    threadSynchronisation.startThreads();

    m_context.setExecutionStartTime();
    logger.output("start time is " + m_context.getExecutionStartTime() +
                  " ms since Epoch");

    final TimerTask reportTimerTask =
      new ReportToConsoleTimerTask(threadSynchronisation);
    final TimerTask shutdownTimerTask = new ShutdownTimerTask();

    // Schedule a regular statistics report to the console. We don't
    // need to schedule this at a fixed rate. Each report contains the
    // work done since the last report.

    // First (empty) report to console to start it recording if its
    // not already.
    reportTimerTask.run();

    timer.schedule(reportTimerTask, reportToConsoleInterval,
                   reportToConsoleInterval);

    try {
      if (duration > 0) {
        logger.output("will shut down after " + duration + " ms",
                      Logger.LOG | Logger.TERMINAL);

        timer.schedule(shutdownTimerTask, duration);
      }

      // Wait for a termination event.
      synchronized (m_eventSynchronisation) {
        while (!threadSynchronisation.isFinished()) {

          if (m_consoleListener.checkForMessage(ConsoleListener.ANY ^
                                                ConsoleListener.START)) {
            break;
          }

          if (m_shutdownTriggered) {
            logger.output("specified duration exceeded, shutting down",
                          Logger.LOG | Logger.TERMINAL);
            break;
          }

          m_eventSynchronisation.waitNoInterrruptException();
        }
      }

      synchronized (m_eventSynchronisation) {
        if (!threadSynchronisation.isFinished()) {

          logger.output("waiting for threads to terminate",
                        Logger.LOG | Logger.TERMINAL);

          m_threadStarter = m_invalidThreadStarter;
          m_context.shutdown();

          final long time = System.currentTimeMillis();
          final long maximumShutdownTime = 10000;

          while (!threadSynchronisation.isFinished()) {
            if (System.currentTimeMillis() - time > maximumShutdownTime) {
              logger.output("ignoring unresponsive threads",
                            Logger.LOG | Logger.TERMINAL);
              break;
            }

            m_eventSynchronisation.waitNoInterrruptException(
              maximumShutdownTime);
          }
        }
      }
    }
    finally {
      reportTimerTask.cancel();
      shutdownTimerTask.cancel();
    }

    scriptEngine.shutdown();

    // Final report to the console.
    reportTimerTask.run();

    m_loggerImplementation.getDataWriter().close();

    if (!m_communicationShutdown) {
      m_consoleSender.send(
        m_context.createStatusMessage(
          WorkerProcessReport.STATE_FINISHED, (short)0, (short)0));
    }

    m_consoleSender.shutdown();

    final long elapsedTime = m_context.getElapsedTime();
    logger.output("elapsed time is " + elapsedTime + " ms");

    logger.output("Final statistics for this process:");

    final LongIndex periodIndex =
      statisticsServices.getStatisticsIndexMap().getLongIndex("period");

    m_accumulatedStatistics.new ForEach() {
      protected void next(Test test, StatisticsSet statistics) {
        statistics.setValue(periodIndex, elapsedTime);
      }
    }
    .iterate();

    final StatisticsTable statisticsTable =
      new StatisticsTable(statisticsServices.getSummaryStatisticsView(),
                          m_accumulatedStatistics);

    statisticsTable.print(logger.getOutputLogWriter());

    timer.cancel();

    logger.output("finished", Logger.LOG | Logger.TERMINAL);
  }

  public void shutdown(boolean inputStreamIsStdin) {
    if (!inputStreamIsStdin) {
      // Sadly it appears its impossible to interrupt a read() on a process
      // input stream (at least under W2K), so we can't shut down the message
      // pump cleanly. It runs in a daemon thread, so this isn't a big
      // deal.
      m_messagePump.shutdown();
    }

    m_loggerImplementation.close();
  }

  public Logger getLogger() {
    return m_context.getProcessLogger();
  }

  private class ReportToConsoleTimerTask extends TimerTask {
    private final ThreadSynchronisation m_threads;

    public ReportToConsoleTimerTask(ThreadSynchronisation threads) {
      m_threads = threads;
    }

    public void run() {
      m_loggerImplementation.getDataWriter().flush();

      if (!m_communicationShutdown) {
        try {
          final TestStatisticsMap sample =
            m_context.getTestRegistry().getTestStatisticsMap().reset();
          m_accumulatedStatistics.add(sample);

          // We look up the new tests after we've taken the sample to
          // avoid a race condition when new tests are being added.
          final Collection<Test> newTests =
            m_context.getTestRegistry().getNewTests();

          if (newTests != null) {
            m_consoleSender.queue(new RegisterTestsMessage(newTests));
          }

          if (sample.size() > 0) {
            m_consoleSender.queue(
              m_context.createReportStatisticsMessage(sample));
          }

          m_consoleSender.send(
            m_context.createStatusMessage(WorkerProcessReport.STATE_RUNNING,
                                          m_threads.getNumberOfRunningThreads(),
                                          m_threads.getTotalNumberOfThreads()));
        }
        catch (CommunicationException e) {
          final Logger logger = m_context.getProcessLogger();

          logger.output("Report to console failed: " + e.getMessage(),
                        Logger.LOG | Logger.TERMINAL);

          e.printStackTrace(logger.getErrorLogWriter());

          m_communicationShutdown = true;
        }
      }
    }
  }

  private class ShutdownTimerTask extends TimerTask {
    public void run() {
      synchronized (m_eventSynchronisation) {
        m_shutdownTriggered = true;
        m_eventSynchronisation.notifyAll();
      }
    }
  }

  private static class TickLoggerTimerTask extends TimerTask {
    public void run() {
      LoggerImplementation.tick();
    }
  }

  /**
   * Implement {@link WorkerThreadSynchronisation}. I looked hard at JSR 166's
   * <code>CountDownLatch</code> and <code>CyclicBarrier</code>, but neither
   * of them allow for the waiting thread to be interrupted by other events.
   *
   * <p>Package scope for unit tests.</p>
   *
   * @author Philip Aston
   * @version $Revision: 4234 $
   */
  static class ThreadSynchronisation implements WorkerThreadSynchronisation {
    private final BooleanCondition m_started = new BooleanCondition();
    private final Condition m_threadEventCondition;

    private short m_numberCreated = 0;
    private short m_numberAwaitingStart = 0;
    private short m_numberFinished = 0;

    ThreadSynchronisation(Condition condition) {
      m_threadEventCondition = condition;
    }

    /**
     * The number of worker threads that have been created but not run to
     * completion.
     */
    public short getNumberOfRunningThreads() {
      synchronized (m_threadEventCondition) {
        return (short)(m_numberCreated - m_numberFinished);
      }
    }

    public boolean isReadyToStart() {
      synchronized (m_threadEventCondition) {
        return m_numberAwaitingStart >= getNumberOfRunningThreads();
      }
    }

    public boolean isFinished() {
      return getNumberOfRunningThreads() <= 0;
    }

    /**
     * The number of worker threads that have been created.
     */
    public short getTotalNumberOfThreads() {
      synchronized (m_threadEventCondition) {
        return m_numberCreated;
      }
    }

    public void threadCreated() {
      synchronized (m_threadEventCondition) {
        ++m_numberCreated;
      }
    }

    public void startThreads() {
      synchronized (m_threadEventCondition) {
        while (!isReadyToStart()) {
          m_threadEventCondition.waitNoInterrruptException();
        }

        m_numberAwaitingStart = 0;
      }

      m_started.set(true);
    }

    public void awaitStart() {
      synchronized (m_threadEventCondition) {
        ++m_numberAwaitingStart;

        if (isReadyToStart()) {
          m_threadEventCondition.notifyAll();
        }
      }

      m_started.await(true);
    }

    public void threadFinished() {
      synchronized (m_threadEventCondition) {
        ++m_numberFinished;

        if (isReadyToStart() || isFinished()) {
          m_threadEventCondition.notifyAll();
        }
      }
    }
  }

  private final class ThreadStarterImplementation implements ThreadStarter {
    private final ThreadSynchronisation m_threadSynchronisation;
    private final ScriptEngine m_scriptEngine;

    private int m_i = -1;

    private ThreadStarterImplementation(
      ThreadSynchronisation threadSynchronisation,
      ScriptEngine scriptEngine) {
      m_threadSynchronisation = threadSynchronisation;
      m_scriptEngine = scriptEngine;
    }

    public int startThread(Object testRunner) throws EngineException {
      final int threadNumber;
      synchronized (this) {
        threadNumber = ++m_i;
      }

      final GrinderThread runnable;

      if (testRunner != null) {
        runnable =
          new GrinderThread(m_threadSynchronisation,
                            m_context,
                            m_loggerImplementation,
                            m_scriptEngine,
                            threadNumber,
                            m_scriptEngine.createWorkerRunnable(testRunner));
      }
      else {
        runnable =
          new GrinderThread(m_threadSynchronisation,
                            m_context,
                            m_loggerImplementation,
                            m_scriptEngine,
                            threadNumber,
                            null);
      }

      final Thread t = new Thread(runnable, "Grinder thread " + threadNumber);
      t.setDaemon(true);
      t.start();

      return threadNumber;
    }
  }

  private static final class InvalidThreadStarter implements ThreadStarter {
    public int startThread(Object testRunner) throws InvalidContextException {
      throw new InvalidContextException(
        "You should not start worker threads until the main thread has " +
        "initialised the script engine, or after all other threads have " +
        "shut down. Typically, you should only call startWorkerThread() from " +
        "another worker thread.");
    }
  }
}
