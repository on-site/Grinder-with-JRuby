// Copyright (C) 2008 - 2010 Philip Aston
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

package net.grinder.console.swingui;

import java.awt.Color;
import java.io.File;
import java.io.StringWriter;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Timer;

import net.grinder.common.StubTest;
import net.grinder.common.Test;
import net.grinder.console.common.StubResources;
import net.grinder.console.model.ConsoleProperties;
import net.grinder.console.model.ModelTestIndex;
import net.grinder.console.model.SampleModel;
import net.grinder.console.model.SampleModelImplementation;
import net.grinder.console.model.SampleModelViews;
import net.grinder.statistics.StatisticsServices;
import net.grinder.statistics.StatisticsServicesTestFactory;
import net.grinder.statistics.StatisticsSet;
import net.grinder.statistics.TestStatisticsMap;
import net.grinder.statistics.TestStatisticsQueries;
import net.grinder.testutility.AbstractFileTestCase;
import net.grinder.testutility.DelegatingStubFactory;
import net.grinder.testutility.RandomStubFactory;
import net.grinder.testutility.StubTimer;

/**
 * Unit tests for {@link CumulativeStatisticsTableModel}.
 *
 * @author Philip Aston
 * @version $Revision: 4224 $
 */
public class TestCumulativeStatisticsTableModel extends AbstractFileTestCase {

  private File m_file;

  protected void setUp() throws Exception {
    super.setUp();
    m_file = new File(getDirectory(), "properties");
  }

  public static class NullSwingDispatcherFactory
    implements SwingDispatcherFactory {

    public Object create(Object delegate) {
      return delegate;
    }
  }

  private final SwingDispatcherFactory m_swingDispatcherFactoryDelegate =
    new NullSwingDispatcherFactory();

  private final StubResources<String> m_resources =
    new StubResources<String>(
      new HashMap<String, String>() { {
        put("table.test.label", "t3st");
        put("table.testColumn.label", "Test Column");
        put("table.descriptionColumn.label", "Test Description Column");
        put("table.total.label", "Total Label");
      } }
    );

  private final DelegatingStubFactory<SwingDispatcherFactory>
    m_swingDispatcherFactoryStubFactory =
      DelegatingStubFactory.create(m_swingDispatcherFactoryDelegate);
  private final SwingDispatcherFactory m_swingDispatcherFactory =
    m_swingDispatcherFactoryStubFactory.getStub();

  private final RandomStubFactory<SampleModel> m_sampleModelStubFactory =
    RandomStubFactory.create(SampleModel.class);
  private final SampleModel m_sampleModel = m_sampleModelStubFactory.getStub();

  private final RandomStubFactory<SampleModelViews>
    m_sampleModelViewsStubFactory =
      RandomStubFactory.create(SampleModelViews.class);
  private final SampleModelViews m_sampleModelViews =
    m_sampleModelViewsStubFactory.getStub();

  private final StatisticsServices m_statisticsServices =
    StatisticsServicesTestFactory.createTestInstance();

  private final TestStatisticsQueries m_testStatisticsQueries =
    m_statisticsServices.getTestStatisticsQueries();

  {
    m_sampleModelViewsStubFactory.setResult("getCumulativeStatisticsView",
      m_statisticsServices.getSummaryStatisticsView());
    m_sampleModelViewsStubFactory.setResult("getTestStatisticsQueries",
                                            m_testStatisticsQueries);
    m_sampleModelViewsStubFactory.setResult("getNumberFormat",
      new DecimalFormat("0.0"));

    m_sampleModelStubFactory.setResult("getTotalCumulativeStatistics",
      m_statisticsServices.getStatisticsSetFactory().create());
  }

  public void testConstruction() throws Exception {
    final CumulativeStatisticsTableModel model =
      new CumulativeStatisticsTableModel(m_sampleModel,
                                         m_sampleModelViews,
                                         m_resources,
                                         m_swingDispatcherFactory);

    // The dispatcher factory is used a couple of times to wrap
    // listeners.
    m_swingDispatcherFactoryStubFactory.assertSuccess("create", Object.class);
    m_swingDispatcherFactoryStubFactory.assertSuccess("create", Object.class);
    m_swingDispatcherFactoryStubFactory.assertNoMoreCalls();

    assertSame(m_sampleModel, model.getModel());
    assertSame(m_sampleModelViews, model.getModelViews());

    assertEquals(7, model.getColumnCount());
    assertEquals(1, model.getRowCount());
    assertEquals(0, model.getLastModelTestIndex().getNumberOfTests());

    assertEquals("Test Column", model.getColumnName(0));
    assertEquals("Test Description Column", model.getColumnName(1));
    assertEquals("Errors", model.getColumnName(3));

    assertEquals("Total Label", model.getValueAt(0, 0));
    assertEquals("", model.getValueAt(0, 1));
    assertEquals("0 tests", "0", model.getValueAt(0, 2));
    assertEquals("Mean time NaN", "", model.getValueAt(0, 4));
    assertEquals("SD is 0", "0.0", model.getValueAt(0, 5));
    assertEquals("?", model.getValueAt(0, 10));

    assertTrue(model.isBold(0, 1));
    assertTrue(model.isBold(0, 2));
    assertTrue(model.isBold(0, 3));

    assertNull(model.getForeground(0, 0));
    assertNull(model.getForeground(0, 1));
    assertNull(model.getForeground(0, 3));
    assertNull(model.getBackground(0, 0));
    assertNull(model.getBackground(0, 1));
    assertNull(model.getBackground(0, 3));
  }

  public void testDefaultWrite() throws Exception {
    final CumulativeStatisticsTableModel model =
      new CumulativeStatisticsTableModel(m_sampleModel,
                                         m_sampleModelViews,
                                         m_resources,
                                         m_swingDispatcherFactory);

    final StringWriter writer = new StringWriter();

    model.write(writer, "::", "**");

    assertEquals("Test Column::Test Description Column::Tests::Errors::Mean Test Time (ms)::Test Time Standard Deviation (ms)::TPS::**Total Label::::0::0::::0.0::::**",
                 writer.toString());
  }

  public void testWriteWithoutTotals() throws Exception {
    final CumulativeStatisticsTableModel model =
      new CumulativeStatisticsTableModel(m_sampleModel,
                                         m_sampleModelViews,
                                         m_resources,
                                         m_swingDispatcherFactory);

    final StringWriter writer = new StringWriter();

    model.writeWithoutTotals(writer, "::", "**");

    assertEquals("Test Column::Test Description Column::Tests::Errors::Mean Test Time (ms)::Test Time Standard Deviation (ms)::TPS::**",
                 writer.toString());
  }

  public void testAddColumns() throws Exception {
    final CumulativeStatisticsTableModel model =
      new CumulativeStatisticsTableModel(m_sampleModel,
                                         m_sampleModelViews,
                                         m_resources,
                                         m_swingDispatcherFactory);

    m_resources.put("statistic.Errors", "Blah");
    m_resources.put("statistic.Mean_Test_Time_(ms)", "meantime");

    assertEquals(7, model.getColumnCount());

    model.addColumns(m_statisticsServices.getSummaryStatisticsView());

    // Adding same columns again is a no-op.
    assertEquals(7, model.getColumnCount());
    assertEquals("Tests", model.getColumnName(2));
    assertEquals("Errors", model.getColumnName(3));
    assertEquals("Mean Test Time (ms)", model.getColumnName(4));

    model.addColumns(m_statisticsServices.getDetailStatisticsView());

    assertEquals(8, model.getColumnCount());
    assertEquals("Test time", model.getColumnName(2));
    assertEquals("Blah", model.getColumnName(4));
    assertEquals("meantime", model.getColumnName(5));
  }

  public void testWithData() throws Exception {
    final Timer timer = new StubTimer();

    final SampleModelImplementation sampleModelImplementation =
      new SampleModelImplementation(new ConsoleProperties(m_resources, m_file),
                                    m_statisticsServices,
                                    timer,
                                    m_resources,
                                    null);

    final CumulativeStatisticsTableModel model =
      new CumulativeStatisticsTableModel(sampleModelImplementation,
                                         m_sampleModelViews,
                                         m_resources,
                                         m_swingDispatcherFactory);

    model.newTests(null, new ModelTestIndex());

    assertEquals(1, model.getRowCount());
    assertNull(model.getForeground(0, 0));
    assertNull(model.getBackground(0, 0));

    final Test[] tests = {
        new StubTest(1, "test 1"),
        new StubTest(2, "test 2"),
    };

    sampleModelImplementation.registerTests(Arrays.asList(tests));

    assertEquals(3, model.getRowCount());
    assertNull(model.getForeground(0, 0));
    assertNull(model.getBackground(0, 0));
    assertEquals("t3st 1", model.getValueAt(0, 0));
    assertEquals("test 1", model.getValueAt(0, 1));
    assertEquals("0", model.getValueAt(0, 3));
    assertNull(model.getForeground(0, 3));
    assertNull(model.getForeground(2, 3));

    final StatisticsSet statistics =
      m_statisticsServices.getStatisticsSetFactory().create();
    statistics.addValue(
      m_statisticsServices.getStatisticsIndexMap().getLongIndex("errors"), 1);

    final TestStatisticsMap testStatisticsMap = new TestStatisticsMap();
    testStatisticsMap.put(tests[0], statistics);
    sampleModelImplementation.addTestReport(testStatisticsMap);

    assertEquals("1", model.getValueAt(0, 3));
    assertEquals(Color.RED, model.getForeground(0, 3));
    assertNull(model.getForeground(0, 2));
    assertEquals(Color.RED, model.getForeground(2, 3));
  }
}
