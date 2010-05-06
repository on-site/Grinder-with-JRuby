// Copyright (C) 2008 Pawel Lacinski
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

package net.grinder.engine.agent;

import java.io.File;

import net.grinder.common.GrinderException;
import net.grinder.common.Logger;
import net.grinder.common.LoggerStubFactory;
import net.grinder.engine.agent.TestAgentDaemon.ActionListSleeperStubFactory.SleepAction;
import net.grinder.testutility.AbstractFileTestCase;
import net.grinder.testutility.RandomStubFactory;
import net.grinder.util.Sleeper;
import net.grinder.util.Sleeper.ShutdownException;


/**
 * Unit tests for {@link AgentDaemon}
 * TestAgentDaemon.
 *
 * @author
 * @author Philip Aston
 * @version $Revision: 4151 $
 */
public class TestAgentDaemon extends AbstractFileTestCase {

  private final LoggerStubFactory m_loggerStubFactory = new LoggerStubFactory();
  private final Logger m_logger = m_loggerStubFactory.getLogger();

  private final RandomStubFactory<Agent> m_agentStubFactory =
    RandomStubFactory.create(Agent.class);
  private final Agent m_agent = m_agentStubFactory.getStub();

  public void testConstruction() throws Exception {
    final File propertyFile = new File(getDirectory(), "properties");
    final Agent agent = new AgentImplementation(m_logger, propertyFile, false);
    final AgentDaemon daemon = new AgentDaemon(m_logger, 1000, agent);
    daemon.shutdown();

    m_loggerStubFactory.assertOutputMessageContains("finished");
    m_loggerStubFactory.assertNoMoreCalls();
  }

  public void testRun() throws Exception {
    final ActionListSleeperStubFactory sleeperStubFactory =
      new ActionListSleeperStubFactory(
        new SleepAction[] {
          new SleepAction() {
            public void sleep(long time) throws ShutdownException {
              assertEquals(1000, time);
            }
          },
          new SleepAction() {
            public void sleep(long time) throws ShutdownException {
              assertEquals(1000, time);
              throw new ShutdownException("");
            }
          }
      } );

    final AgentDaemon agentDaemon =
      new AgentDaemon(m_logger, 1000, m_agent, sleeperStubFactory.getStub());

    agentDaemon.run();

    sleeperStubFactory.assertFinished();
  }

  public void testShutdownHook() throws Exception {
    final AgentDaemon agentDaemon = new AgentDaemon(m_logger, 0, m_agent);
    m_loggerStubFactory.assertNoMoreCalls();

    final Thread shutdownHook = agentDaemon.getShutdownHook();

    assertFalse(Runtime.getRuntime().removeShutdownHook(shutdownHook));
    m_loggerStubFactory.assertNoMoreCalls();
    m_agentStubFactory.assertNoMoreCalls();

    final GrinderException runException = new GrinderException("") {};
    m_agentStubFactory.setThrows("run", runException);

    try {
      agentDaemon.run();
      fail("Expected GrinderException");
    }
    catch (GrinderException e) {
      assertSame(runException, e);
    }

    m_agentStubFactory.assertException("run", runException);

    assertTrue(Runtime.getRuntime().removeShutdownHook(shutdownHook));

    shutdownHook.run();
    m_loggerStubFactory.assertNoMoreCalls();
    m_agentStubFactory.assertSuccess("shutdown");
    m_agentStubFactory.assertNoMoreCalls();
  }

  public static class ActionListSleeperStubFactory
    // Good grief. Some horrible javac issue means we need to fully qualify
    // this.
    extends net.grinder.testutility.RandomStubFactory<
            net.grinder.util.Sleeper> {

    public interface SleepAction {
      void sleep(long time) throws ShutdownException;
    }

    private final SleepAction[] m_actions;
    private int m_nextRunnable = 0;

    public ActionListSleeperStubFactory(SleepAction[] actions) {
      super(Sleeper.class);
      m_actions = actions;
    }

    public void override_sleepNormal(Object proxy, long time)
      throws ShutdownException {
      m_actions[m_nextRunnable++].sleep(time);
    }

    public void assertFinished() {
      assertTrue("All actions complete", m_nextRunnable == m_actions.length);
    }
  }
}
