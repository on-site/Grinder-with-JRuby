// Copyright (C) 2000, 2001, 2002, 2003, 2004, 2005 Philip Aston
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

package net.grinder.statistics;

import junit.framework.TestCase;
import junit.swingui.TestRunner;

import net.grinder.statistics.StatisticsIndexMap;
import net.grinder.statistics.StatisticsSet;

/**
 * Unit test case for <code>StatisticsIndexMap</code>.
 *
 * @author Philip Aston
 * @version $Revision: 3762 $
 * @see StatisticsSet
 */
public class TestStatisticsIndexMap extends TestCase {

  public static void main(String[] args) {
    TestRunner.run(TestStatisticsIndexMap.class);
  }

  public TestStatisticsIndexMap(String name) {
    super(name);
  }

  private final StatisticsIndexMap m_indexMap =
    StatisticsServicesImplementation.getInstance().getStatisticsIndexMap();

  public void testLongs() throws Exception {
    final String[] data = {
        "userLong0", "userLong1", "userLong2", "userLong3", };

    final StatisticsIndexMap.LongIndex[] longResults =
      new StatisticsIndexMap.LongIndex[data.length];

    for (int i = 0; i < data.length; i++) {
      longResults[i] = m_indexMap.getLongIndex(data[i]);

      assertNotNull(m_indexMap.getLongIndex(data[i]));
      assertNull(m_indexMap.getDoubleIndex(data[i]));

      for (int j = 0; j < i; ++j) {
        assertTrue(longResults[i].getValue() != longResults[j].getValue());
      }
    }

    for (int i = 0; i < data.length; i++) {
      assertEquals(longResults[i].getValue(), m_indexMap.getLongIndex(
          data[i]).getValue());
    }
  }

  public void testDoubles() throws Exception {
    final String[] data = {
        "userDouble0", "userDouble1", "userDouble2", "userDouble3", };

    final StatisticsIndexMap.DoubleIndex[] doubleResults =
      new StatisticsIndexMap.DoubleIndex[data.length];

    for (int i = 0; i < data.length; i++) {
      doubleResults[i] = m_indexMap.getDoubleIndex(data[i]);

      assertNotNull(m_indexMap.getDoubleIndex(data[i]));
      assertNull(m_indexMap.getLongIndex(data[i]));

      for (int j = 0; j < i; ++j) {
        assertTrue(doubleResults[i].getValue() != doubleResults[j].getValue());
      }
    }

    for (int i = 0; i < data.length; i++) {
      assertEquals(doubleResults[i].getValue(), m_indexMap.getDoubleIndex(
          data[i]).getValue());
    }
  }
}