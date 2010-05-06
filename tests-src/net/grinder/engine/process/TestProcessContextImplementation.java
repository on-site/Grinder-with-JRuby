// Copyright (C) 2007 - 2009 Philip Aston
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

import junit.framework.TestCase;
import net.grinder.common.GrinderProperties;
import net.grinder.common.Logger;
import net.grinder.common.StubTest;
import net.grinder.common.ThreadLifeCycleListener;
import net.grinder.common.processidentity.WorkerIdentity;
import net.grinder.common.processidentity.WorkerProcessReport;
import net.grinder.communication.QueuedSender;
import net.grinder.messages.console.ReportStatisticsMessage;
import net.grinder.messages.console.WorkerProcessReportMessage;
import net.grinder.script.Grinder;
import net.grinder.script.Grinder.ScriptContext;
import net.grinder.statistics.StatisticsServices;
import net.grinder.statistics.StatisticsServicesTestFactory;
import net.grinder.statistics.StatisticsSet;
import net.grinder.statistics.TestStatisticsMap;
import net.grinder.testutility.RandomStubFactory;


/**
 * Unit tests for {@link TestProcessContextImplementation}.
 *
 * @author Philip Aston
 * @version $Revision: 4006 $
 */
public class TestProcessContextImplementation extends TestCase {

  private final RandomStubFactory<ThreadContext> m_threadContextStubFactory =
    RandomStubFactory.create(ThreadContext.class);
  private final ThreadContext m_threadContext =
    m_threadContextStubFactory.getStub();

  private final RandomStubFactory<Logger> m_loggerStubFactory =
    RandomStubFactory.create(Logger.class);
  private final Logger m_logger =
    m_loggerStubFactory.getStub();

  private final RandomStubFactory<QueuedSender> m_queuedSenderStubFactory =
    RandomStubFactory.create(QueuedSender.class);
  private final QueuedSender m_queuedSender =
    m_queuedSenderStubFactory.getStub();

  private final RandomStubFactory<WorkerIdentity> m_workerIdentityStubFactory =
    RandomStubFactory.create(WorkerIdentity.class);
  private final WorkerIdentity m_workerIdentity =
    m_workerIdentityStubFactory.getStub();
  private final WorkerIdentity m_firstWorkerIdentity =
    m_workerIdentityStubFactory.getStub();

  public void testAccessors() throws Exception {
    final GrinderProperties properties = new GrinderProperties();

    final StatisticsServices statisticsServices =
      StatisticsServicesTestFactory.createTestInstance();

    final ProcessContext processContext =
      new ProcessContextImplementation(
        m_workerIdentity,
        m_firstWorkerIdentity,
        properties,
        m_logger,
        null,
        m_queuedSender,
        statisticsServices,
        null);

    assertSame(statisticsServices, processContext.getStatisticsServices());
    assertSame(m_logger, processContext.getProcessLogger());
    assertSame(properties, processContext.getProperties());

    assertNotNull(processContext.getSleeper());
    assertNotNull(processContext.getTestRegistry());

    assertEquals(0, processContext.getExecutionStartTime());
    final long t1 = System.currentTimeMillis();
    processContext.setExecutionStartTime();
    final long t2 = System.currentTimeMillis();

    // Fudge required since nanoTime() is more precise that currentTimeMillis().
    final long FUDGE = 10;

    assertTrue(t1 - FUDGE <= processContext.getExecutionStartTime());
    assertTrue(processContext.getExecutionStartTime() <= t2 + FUDGE);
    final long elapsedTime = processContext.getElapsedTime();
    assertTrue(elapsedTime <= (System.currentTimeMillis() - t1));
    assertTrue(elapsedTime >= 0);
  }

  public void testThreadContextLocator() throws Exception {
    final ProcessContext processContext =
      new ProcessContextImplementation(
        null,
        null,
        new GrinderProperties(),
        null,
        null,
        null,
        StatisticsServicesTestFactory.createTestInstance(),
        null);

    final ThreadContextLocator threadContextLocator =
      processContext.getThreadContextLocator();
    assertNotNull(threadContextLocator);

    threadContextLocator.set(m_threadContext);
    assertSame(m_threadContext, threadContextLocator.get());

    final Thread t1 = new Thread() {
      public void run() {
        assertNull(threadContextLocator.get());
        threadContextLocator.set(m_threadContext);
        assertSame(m_threadContext, threadContextLocator.get());
        threadContextLocator.set(null);
        assertNull(threadContextLocator.get());
      }
    };
    t1.start();
    t1.join();

    assertSame(m_threadContext, threadContextLocator.get());
  }

  public void testProcessEvents() throws Exception {
    final ProcessContext processContext =
      new ProcessContextImplementation(
        m_workerIdentity,
        m_firstWorkerIdentity,
        new GrinderProperties(),
        m_logger,
        null,
        m_queuedSender,
        StatisticsServicesTestFactory.createTestInstance(),
        null);

    processContext.fireThreadCreatedEvent(m_threadContext);

    m_threadContextStubFactory.assertSuccess(
      "registerThreadLifeCycleListener", ThreadLifeCycleListener.class);
    m_threadContextStubFactory.assertSuccess("getThreadNumber");
    m_threadContextStubFactory.assertSuccess(
      "registerThreadLifeCycleListener", ThreadLifeCycleListener.class);
    m_threadContextStubFactory.assertNoMoreCalls();
  }

  public void testMessageFactories() throws Exception {
    final StatisticsServices statisticsServices =
      StatisticsServicesTestFactory.createTestInstance();

    final GrinderProperties grinderProperties = new GrinderProperties();

    final ProcessContext processContext =
      new ProcessContextImplementation(
        m_workerIdentity,
        m_firstWorkerIdentity,
        grinderProperties,
        m_logger,
        null,
        m_queuedSender,
        statisticsServices,
        null);

    final WorkerProcessReportMessage message1 =
      processContext.createStatusMessage(
        WorkerProcessReport.STATE_RUNNING, (short)5, (short)10);

    final WorkerProcessReportMessage message2 =
      processContext.createStatusMessage(
        WorkerProcessReport.STATE_RUNNING, (short)5, (short)10);

    assertNotSame(message1, message2);

    assertEquals(WorkerProcessReport.STATE_RUNNING, message1.getState());
    assertEquals(5, message1.getNumberOfRunningThreads());
    assertEquals(10, message1.getMaximumNumberOfThreads());

    assertSame(m_workerIdentity, message1.getWorkerIdentity());

    final TestStatisticsMap testStatisticsMap = new TestStatisticsMap();
    final StatisticsSet statistics =
      statisticsServices.getStatisticsSetFactory().create();
    final TestStatisticsHelper testStatisticsHelper =
      new TestStatisticsHelperImplementation(
        statisticsServices.getStatisticsIndexMap());

    testStatisticsHelper.recordTest(statistics, 123);
    testStatisticsMap.put(new StubTest(1, ""), statistics);
    final ReportStatisticsMessage message3 =
      processContext.createReportStatisticsMessage(testStatisticsMap);

    final TestStatisticsMap statisticsDelta = message3.getStatisticsDelta();
    assertSame(testStatisticsMap, statisticsDelta);
    assertEquals(123, testStatisticsHelper.getTestTime(
      statisticsDelta.nonCompositeStatisticsTotals()));

    grinderProperties.setBoolean("grinder.reportTimesToConsole", false);

    final ProcessContext processContext2 =
      new ProcessContextImplementation(
        m_workerIdentity,
        m_firstWorkerIdentity,
        grinderProperties,
        m_logger,
        null,
        m_queuedSender,
        statisticsServices,
        null);

    final ReportStatisticsMessage message4 =
      processContext2.createReportStatisticsMessage(testStatisticsMap);

    final TestStatisticsMap statisticsDelta2 = message4.getStatisticsDelta();
    assertEquals(0, testStatisticsHelper.getTestTime(
      statisticsDelta2.nonCompositeStatisticsTotals()));
  }

  public void testShutdownAll() throws Exception {
    final ProcessContext processContext =
      new ProcessContextImplementation(
        m_workerIdentity,
        m_firstWorkerIdentity,
        new GrinderProperties(),
        m_logger,
        null,
        m_queuedSender,
        StatisticsServicesTestFactory.createTestInstance(),
        null);

    final RandomStubFactory<ThreadContext> threadContext1StubFactory =
      RandomStubFactory.create(ThreadContext.class);
    threadContext1StubFactory.setResult("getThreadNumber", new Integer(1));

    final RandomStubFactory<ThreadContext> threadContext2StubFactory =
      RandomStubFactory.create(ThreadContext.class);
    threadContext2StubFactory.setResult("getThreadNumber", new Integer(2));

    processContext.fireThreadCreatedEvent(threadContext1StubFactory.getStub());

    threadContext1StubFactory.assertSuccess(
      "registerThreadLifeCycleListener", ThreadLifeCycleListener.class);
    threadContext1StubFactory.assertSuccess("getThreadNumber");
    final ThreadLifeCycleListener threadListener1 = (ThreadLifeCycleListener)
    threadContext1StubFactory.assertSuccess(
      "registerThreadLifeCycleListener",
      ThreadLifeCycleListener.class).getParameters()[0];
    threadContext1StubFactory.assertNoMoreCalls();

    processContext.fireThreadCreatedEvent(threadContext2StubFactory.getStub());

    threadContext2StubFactory.assertSuccess(
      "registerThreadLifeCycleListener", ThreadLifeCycleListener.class);
    threadContext2StubFactory.assertSuccess("getThreadNumber");
    threadContext2StubFactory.assertSuccess(
      "registerThreadLifeCycleListener", ThreadLifeCycleListener.class);
    threadContext2StubFactory.assertNoMoreCalls();

    processContext.shutdown();

    threadContext1StubFactory.assertSuccess("shutdown");
    threadContext1StubFactory.assertNoMoreCalls();
    threadContext2StubFactory.assertSuccess("shutdown");
    threadContext2StubFactory.assertNoMoreCalls();

    final RandomStubFactory<ThreadContext> threadContext3StubFactory =
      RandomStubFactory.create(ThreadContext.class);
    threadContext3StubFactory.setResult("getThreadNumber", new Integer(3));

    processContext.fireThreadCreatedEvent(threadContext3StubFactory.getStub());

    threadContext3StubFactory.assertSuccess(
      "registerThreadLifeCycleListener", ThreadLifeCycleListener.class);
    threadContext3StubFactory.assertSuccess("getThreadNumber");
    threadContext3StubFactory.assertSuccess("shutdown");
    threadContext3StubFactory.assertNoMoreCalls();

    threadListener1.endThread();

    processContext.shutdown();

    threadContext1StubFactory.assertNoMoreCalls();
    threadContext2StubFactory.assertSuccess("shutdown");
    threadContext2StubFactory.assertNoMoreCalls();
    threadContext3StubFactory.assertNoMoreCalls();
  }

  public void testStopThread() throws Exception {
    final ProcessContext processContext =
      new ProcessContextImplementation(
        m_workerIdentity,
        m_firstWorkerIdentity,
        new GrinderProperties(),
        m_logger,
        null,
        m_queuedSender,
        StatisticsServicesTestFactory.createTestInstance(),
        null);

    final ScriptContext scriptContext = Grinder.grinder;

    final RandomStubFactory<ThreadContext> threadContext1StubFactory =
      RandomStubFactory.create(ThreadContext.class);
    threadContext1StubFactory.setResult("getThreadNumber", new Integer(1));

    final RandomStubFactory<ThreadContext> threadContext2StubFactory =
      RandomStubFactory.create(ThreadContext.class);
    threadContext2StubFactory.setResult("getThreadNumber", new Integer(2));

    processContext.fireThreadCreatedEvent(threadContext1StubFactory.getStub());

    threadContext1StubFactory.assertSuccess(
      "registerThreadLifeCycleListener", ThreadLifeCycleListener.class);
    threadContext1StubFactory.assertSuccess("getThreadNumber");
    final ThreadLifeCycleListener threadListener1 = (ThreadLifeCycleListener)
    threadContext1StubFactory.assertSuccess(
      "registerThreadLifeCycleListener",
      ThreadLifeCycleListener.class).getParameters()[0];
    threadContext1StubFactory.assertNoMoreCalls();

    processContext.fireThreadCreatedEvent(threadContext2StubFactory.getStub());

    threadContext2StubFactory.assertSuccess(
      "registerThreadLifeCycleListener", ThreadLifeCycleListener.class);
    threadContext2StubFactory.assertSuccess("getThreadNumber");
    threadContext2StubFactory.assertSuccess(
      "registerThreadLifeCycleListener",
      ThreadLifeCycleListener.class);
    threadContext2StubFactory.assertNoMoreCalls();

    final boolean result1 = scriptContext.stopWorkerThread(3);
    assertFalse(result1);
    threadContext1StubFactory.assertNoMoreCalls();
    threadContext2StubFactory.assertNoMoreCalls();

    final boolean result2 = scriptContext.stopWorkerThread(2);
    assertTrue(result2);
    threadContext1StubFactory.assertNoMoreCalls();
    threadContext2StubFactory.assertSuccess("shutdown");
    threadContext2StubFactory.assertNoMoreCalls();

    threadListener1.endThread();

    final boolean result3 = scriptContext.stopWorkerThread(1);
    assertFalse(result3);
    threadContext1StubFactory.assertNoMoreCalls();
    threadContext2StubFactory.assertNoMoreCalls();
  }
}
