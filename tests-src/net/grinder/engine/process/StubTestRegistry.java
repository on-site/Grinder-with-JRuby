// Copyright (C) 2004 - 2009 Philip Aston
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

import net.grinder.script.Grinder;
import net.grinder.script.InternalScriptContext;
import net.grinder.statistics.StatisticsServicesImplementation;
import net.grinder.statistics.StatisticsSetFactory;
import net.grinder.testutility.RandomStubFactory;


/**
 * Test utility that allows TestRegistryImplementation to be set from outside
 * package.
 *
 * @author Philip Aston
 * @version $Revision: 4143 $
 */
public class StubTestRegistry {

  private static RandomStubFactory<Instrumenter> s_instrumenterStubFactory;

  public static void stubTestRegistry() {
    final StatisticsSetFactory statisticsSetFactory =
      StatisticsServicesImplementation.getInstance().getStatisticsSetFactory();

    final RandomStubFactory<TestStatisticsHelper>
      testStatisticsHelperStubFactory =
        RandomStubFactory.create(TestStatisticsHelper.class);

    final TestRegistryImplementation testRegistry =
      new TestRegistryImplementation(null,
                                     statisticsSetFactory,
                                     testStatisticsHelperStubFactory.getStub(),
                                     null);

    s_instrumenterStubFactory = RandomStubFactory.create(Instrumenter.class);

    testRegistry.setInstrumenter(s_instrumenterStubFactory.getStub());

    final RandomStubFactory<InternalScriptContext> scriptContextStubFactory =
      RandomStubFactory.create(InternalScriptContext.class);
    scriptContextStubFactory.setResult("getTestRegistry", testRegistry);

    Grinder.grinder = scriptContextStubFactory.getStub();
  }

  public static RandomStubFactory<Instrumenter> getInstrumenterStubFactory() {
    return s_instrumenterStubFactory;
  }
}
