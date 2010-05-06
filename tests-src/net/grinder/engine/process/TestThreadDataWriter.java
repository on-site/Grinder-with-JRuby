// Copyright (C) 2005, 2006, 2007 Philip Aston
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

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;

import net.grinder.common.StubTest;
import net.grinder.common.Test;
import net.grinder.statistics.StatisticsIndexMap;
import net.grinder.statistics.StatisticsServices;
import net.grinder.statistics.StatisticsServicesImplementation;
import net.grinder.statistics.StatisticsServicesTestFactory;
import net.grinder.statistics.StatisticsSet;
import junit.framework.TestCase;


public class TestThreadDataWriter extends TestCase {

  private static final StatisticsIndexMap.LongIndex s_errorsIndex;
  private static final StatisticsIndexMap.LongSampleIndex s_timedTestsIndex;
  private static final StatisticsIndexMap.DoubleIndex s_userDouble0Index;

  static {
    final StatisticsIndexMap indexMap =
      StatisticsServicesImplementation.getInstance().getStatisticsIndexMap();

    s_errorsIndex = indexMap.getLongIndex("errors");
    s_timedTestsIndex = indexMap.getLongSampleIndex("timedTests");
    s_userDouble0Index = indexMap.getDoubleIndex("userDouble0");
  }

  private final ByteArrayOutputStream m_dataOutput =
    new ByteArrayOutputStream();

  private final PrintWriter m_dataFilePrintWriter =
    new PrintWriter(m_dataOutput, true);

  public void testReport() throws Exception {

    final StatisticsServices statisticsServices =
      StatisticsServicesTestFactory.createTestInstance();

    final ThreadDataWriter threadDataWriter =
      new ThreadDataWriter(
          m_dataFilePrintWriter,
          statisticsServices.getDetailStatisticsView().getExpressionViews(),
          33);

    final Test test1 = new StubTest(1, "A description");
    final Test test3 = new StubTest(3, "Another test");

    final StatisticsSet statistics =
      statisticsServices.getStatisticsSetFactory().create();

    statistics.addSample(s_timedTestsIndex, 99);

    threadDataWriter.report(10, test1, 123L, statistics);

    assertEquals("33, 10, 1, 123, 99, 0", m_dataOutput.toString().trim());
    m_dataOutput.reset();

    threadDataWriter.report(10, test1, 125L, statistics);

    assertEquals("33, 10, 1, 125, 99, 0", m_dataOutput.toString().trim());
    m_dataOutput.reset();

    threadDataWriter.report(11, test3, 300L, statistics);

    assertEquals("33, 11, 3, 300, 99, 0", m_dataOutput.toString().trim());
    m_dataOutput.reset();

    statistics.reset();
    statistics.setValue(s_errorsIndex, 1);

    threadDataWriter.report(11, test3, 301L, statistics);

    assertEquals("33, 11, 3, 301, 0, 1", m_dataOutput.toString().trim());
    m_dataOutput.reset();

    statisticsServices.getDetailStatisticsView().add(
      statisticsServices.getStatisticExpressionFactory()
      .createExpressionView("foo", "userDouble0", false));
    statistics.reset();
    statistics.addSample(s_timedTestsIndex, 5);
    statistics.addValue(s_userDouble0Index, 1.5);

    final ThreadDataWriter threadDataWriter2 =
      new ThreadDataWriter(
          m_dataFilePrintWriter,
          statisticsServices.getDetailStatisticsView().getExpressionViews(),
          33);

    threadDataWriter2.report(11, test3, 530L, statistics);

    assertEquals("33, 11, 3, 530, 5, 0, 1.5", m_dataOutput.toString().trim());
    m_dataOutput.reset();
  }
}