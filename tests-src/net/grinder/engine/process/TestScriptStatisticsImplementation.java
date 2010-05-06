// Copyright (C) 2006 - 2009 Philip Aston
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

import java.util.Arrays;

import junit.framework.TestCase;

import net.grinder.communication.QueuedSender;
import net.grinder.messages.console.RegisterExpressionViewMessage;
import net.grinder.script.InvalidContextException;
import net.grinder.script.Statistics;
import net.grinder.script.Statistics.StatisticsForTest;
import net.grinder.statistics.ExpressionView;
import net.grinder.statistics.StatisticsServices;
import net.grinder.statistics.StatisticsServicesTestFactory;
import net.grinder.statistics.StatisticsView;
import net.grinder.testutility.AssertUtilities;
import net.grinder.testutility.CallData;
import net.grinder.testutility.RandomStubFactory;


/**
 * Unit test case for {@link ScriptStatisticsImplementation}.
 *
 * @author Philip Aston
 * @version $Revision: 4006 $
 */
public class TestScriptStatisticsImplementation extends TestCase {

  private final RandomStubFactory<ThreadContext> m_threadContextStubFactory =
    RandomStubFactory.create(ThreadContext.class);
  private final ThreadContext m_threadContext =
    m_threadContextStubFactory.getStub();

  final RandomStubFactory<QueuedSender> m_queuedSenderStubFactory =
    RandomStubFactory.create(QueuedSender.class);
  final QueuedSender m_queuedSender =
    m_queuedSenderStubFactory.getStub();

  private final StubThreadContextLocator m_threadContextLocator =
    new StubThreadContextLocator();

  private final StatisticsServices m_statisticsServices =
    StatisticsServicesTestFactory.createTestInstance();


  public void testContextChecks() throws Exception {

    final ScriptStatisticsImplementation scriptStatistics =
      new ScriptStatisticsImplementation(
        m_threadContextLocator,
        m_statisticsServices,
        m_queuedSender);

    // 1. Null thread context.
    assertFalse(scriptStatistics.isTestInProgress());

    try {
      scriptStatistics.getForCurrentTest();
      fail("Expected InvalidContextException");
    }
    catch (InvalidContextException e) {
      AssertUtilities.assertContains(e.getMessage(), "worker threads");
    }

    try {
      scriptStatistics.getForLastTest();
      fail("Expected InvalidContextException");
    }
    catch (InvalidContextException e) {
      AssertUtilities.assertContains(e.getMessage(), "worker threads");
    }

    try {
      scriptStatistics.report();
      fail("Expected InvalidContextException");
    }
    catch (InvalidContextException e) {
      AssertUtilities.assertContains(e.getMessage(), "worker threads");
    }

    try {
      scriptStatistics.setDelayReports(false);
      fail("Expected InvalidContextException");
    }
    catch (InvalidContextException e) {
      AssertUtilities.assertContains(e.getMessage(), "worker threads");
    }

    // 2. No last statistics, no current statistics.
    m_threadContextLocator.set(m_threadContext);
    m_threadContextStubFactory.setResult("getStatisticsForCurrentTest", null);
    m_threadContextStubFactory.setResult("getStatisticsForLastTest", null);
    assertFalse(scriptStatistics.isTestInProgress());

    scriptStatistics.setDelayReports(false);

    try {
      scriptStatistics.getForCurrentTest();
      fail("Expected InvalidContextException");
    }
    catch (InvalidContextException e) {
      AssertUtilities.assertContains(e.getMessage(), "no test");
    }

    try {
      scriptStatistics.getForLastTest();
      fail("Expected InvalidContextException");
    }
    catch (InvalidContextException e) {
      AssertUtilities.assertContains(e.getMessage(), "No tests");
    }

    // 3. No last statistics, current statistics.
    final RandomStubFactory<StatisticsForTest> statisticsForTestStubFactory1 =
      RandomStubFactory.create(StatisticsForTest.class);
    final StatisticsForTest statisticsForTest1 =
      statisticsForTestStubFactory1.getStub();
    m_threadContextStubFactory.setResult(
      "getStatisticsForCurrentTest", statisticsForTest1);
    assertTrue(scriptStatistics.isTestInProgress());
    assertSame(statisticsForTest1, scriptStatistics.getForCurrentTest());

    try {
      scriptStatistics.getForLastTest();
      fail("Expected InvalidContextException");
    }
    catch (InvalidContextException e) {
      AssertUtilities.assertContains(e.getMessage(), "No tests");
    }

    // 4. Last statistics, current statistics.
    final RandomStubFactory<StatisticsForTest> statisticsForTestStubFactory2 =
      RandomStubFactory.create(StatisticsForTest.class);
    final StatisticsForTest statisticsForTest2 =
      statisticsForTestStubFactory2.getStub();
    m_threadContextStubFactory.setResult(
      "getStatisticsForLastTest", statisticsForTest2);

    assertTrue(scriptStatistics.isTestInProgress());
    assertSame(statisticsForTest1, scriptStatistics.getForCurrentTest());
    assertSame(statisticsForTest2, scriptStatistics.getForLastTest());

    m_queuedSenderStubFactory.assertNoMoreCalls();
  }

  public void testRegisterStatisticsViews() throws Exception {

    final ThreadContextLocator threadContextLocator =
      new StubThreadContextLocator();
    threadContextLocator.set(m_threadContext);

    final Statistics scriptStatistics =
      new ScriptStatisticsImplementation(
        threadContextLocator,
        m_statisticsServices,
        m_queuedSender);

    final ExpressionView expressionView =
      m_statisticsServices.getStatisticExpressionFactory()
      .createExpressionView("display", "errors", false);
    scriptStatistics.registerSummaryExpression("display", "errors");

    final CallData callData =
      m_queuedSenderStubFactory.assertSuccess(
        "queue", RegisterExpressionViewMessage.class);
    final RegisterExpressionViewMessage message =
      (RegisterExpressionViewMessage)callData.getParameters()[0];
    assertEquals("display", message.getExpressionView().getDisplayName());
    assertEquals("errors", message.getExpressionView().getExpressionString());
    m_queuedSenderStubFactory.assertNoMoreCalls();

    final StatisticsView summaryStatisticsView =
      m_statisticsServices.getSummaryStatisticsView();

    final ExpressionView[] summaryExpressionViews =
      summaryStatisticsView.getExpressionViews();
    assertTrue(Arrays.asList(summaryExpressionViews).contains(expressionView));

    try {
      scriptStatistics.registerDataLogExpression("display2", "untimedTests");
      fail("Expected InvalidContextException");
    }
    catch (InvalidContextException e) {
    }

    threadContextLocator.set(null);

    scriptStatistics.registerDataLogExpression("display2", "untimedTests");

    final StatisticsView detailStatisticsView =
      m_statisticsServices.getDetailStatisticsView();

    final ExpressionView[] detailExpressionViews =
      detailStatisticsView.getExpressionViews();
    assertTrue(Arrays.asList(detailExpressionViews).contains(
      m_statisticsServices.getStatisticExpressionFactory()
      .createExpressionView("display2", "untimedTests", false)));
  }
}
