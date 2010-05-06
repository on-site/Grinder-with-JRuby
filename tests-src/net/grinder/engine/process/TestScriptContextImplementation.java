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

import junit.framework.TestCase;

import net.grinder.common.FilenameFactory;
import net.grinder.common.GrinderProperties;
import net.grinder.common.Logger;
import net.grinder.common.processidentity.WorkerIdentity;
import net.grinder.engine.agent.StubAgentIdentity;
import net.grinder.script.InvalidContextException;
import net.grinder.script.SSLControl;
import net.grinder.script.Statistics;
import net.grinder.script.TestRegistry;
import net.grinder.testutility.RandomStubFactory;
import net.grinder.testutility.Time;
import net.grinder.util.Sleeper;
import net.grinder.util.SleeperImplementation;
import net.grinder.util.StandardTimeAuthority;


/**
 * Unit test case for <code>ScriptContextImplementation</code>.
 *
 * @author Philip Aston
 * @version $Revision: 4006 $
 */
public class TestScriptContextImplementation extends TestCase {

  private final RandomStubFactory<ThreadContext> m_threadContextStubFactory =
    RandomStubFactory.create(ThreadContext.class);
  private final ThreadContext m_threadContext =
    m_threadContextStubFactory.getStub();

  public TestScriptContextImplementation(String name) {
    super(name);
  }

  public void testConstructorAndGetters() throws Exception {

    final GrinderProperties properties = new GrinderProperties();

    final RandomStubFactory<Logger> loggerStubFactory =
      RandomStubFactory.create(Logger.class);
    final Logger logger = loggerStubFactory.getStub();

    final RandomStubFactory<FilenameFactory> filenameFactoryStubFactory =
      RandomStubFactory.create(FilenameFactory.class);
    final FilenameFactory filenameFactory =
      filenameFactoryStubFactory.getStub();

    final RandomStubFactory<ThreadStarter> threadStarterStubFactory =
      RandomStubFactory.create(ThreadStarter.class);
    final int threadNumber = 99;
    final int runNumber = 3;
    final ThreadContextLocator threadContextLocator =
      new StubThreadContextLocator();
    threadContextLocator.set(m_threadContext);

    m_threadContextStubFactory.setResult("getThreadNumber", threadNumber);
    m_threadContextStubFactory.setResult("getRunNumber", runNumber);

    final RandomStubFactory<Statistics> statisticsStubFactory =
      RandomStubFactory.create(Statistics.class);
    final Statistics statistics = statisticsStubFactory.getStub();
    m_threadContextStubFactory.setResult("getScriptStatistics", statistics);

    final Sleeper sleeper = new SleeperImplementation(null, logger, 1, 0);

    final RandomStubFactory<SSLControl> sslControlStubFactory =
      RandomStubFactory.create(SSLControl.class);
    final SSLControl sslControl = sslControlStubFactory.getStub();

    final StubAgentIdentity agentIdentity =
      new StubAgentIdentity("Agent");
    final WorkerIdentity workerIdentity = agentIdentity.createWorkerIdentity();
    final WorkerIdentity firstWorkerIdentity =
      agentIdentity.createWorkerIdentity();

    final RandomStubFactory<TestRegistry> testRegistryStubFactory =
      RandomStubFactory.create(TestRegistry.class);
    final TestRegistry testRegistry =
      testRegistryStubFactory.getStub();

    final RandomStubFactory<ThreadStopper> threadStopperStubFactory =
      RandomStubFactory.create(ThreadStopper.class);

    final ScriptContextImplementation scriptContext =
      new ScriptContextImplementation(
        workerIdentity, firstWorkerIdentity, threadContextLocator, properties,
        logger, filenameFactory, sleeper, sslControl, statistics, testRegistry,
        threadStarterStubFactory.getStub(), threadStopperStubFactory.getStub());

    assertEquals(workerIdentity.getName(), scriptContext.getProcessName());
    assertEquals(workerIdentity.getNumber(),
                 scriptContext.getProcessNumber());
    assertEquals(firstWorkerIdentity.getNumber(),
                 scriptContext.getFirstProcessNumber());
    assertEquals(threadNumber, scriptContext.getThreadNumber());
    assertEquals(runNumber, scriptContext.getRunNumber());
    assertSame(logger, scriptContext.getLogger());
    assertSame(filenameFactory, scriptContext.getFilenameFactory());
    assertSame(properties, scriptContext.getProperties());
    assertSame(statistics, scriptContext.getStatistics());
    assertSame(sslControl, scriptContext.getSSLControl());
    assertSame(testRegistry, scriptContext.getTestRegistry());

    threadContextLocator.set(null);
    assertEquals(-1, scriptContext.getThreadNumber());
    assertEquals(-1, scriptContext.getRunNumber());
    assertEquals(statistics, scriptContext.getStatistics());

    assertEquals(0, scriptContext.getProcessNumber());
    assertEquals(-1, scriptContext.getAgentNumber());

    agentIdentity.setNumber(10);
    assertEquals(0, scriptContext.getProcessNumber());
    assertEquals(10, scriptContext.getAgentNumber());

    threadStarterStubFactory.assertNoMoreCalls();

    scriptContext.startWorkerThread();
    threadStarterStubFactory.assertSuccess("startThread", Object.class);
    threadStarterStubFactory.assertNoMoreCalls();

    final Object testRunner = new Object();
    scriptContext.startWorkerThread(testRunner);
    threadStarterStubFactory.assertSuccess("startThread", testRunner);
    threadStarterStubFactory.assertNoMoreCalls();

    scriptContext.stopWorkerThread(10);
    threadStopperStubFactory.assertSuccess("stopThread", new Integer(10));
    threadStopperStubFactory.assertNoMoreCalls();
  }

  public void testSleep() throws Exception {

    final RandomStubFactory<Logger> loggerStubFactory =
      RandomStubFactory.create(Logger.class);
    final Sleeper sleeper =
      new SleeperImplementation(new StandardTimeAuthority(),
                                loggerStubFactory.getStub(),
                                1,
                                0);

    final ScriptContextImplementation scriptContext =
      new ScriptContextImplementation(
        null, null, null, null, null, null, sleeper, null, null, null, null,
        null);

    assertTrue(
      new Time(50, 70) {
        public void doIt() throws Exception  { scriptContext.sleep(50); }
      }.run());

    assertTrue(
      new Time(40, 70) {
        public void doIt() throws Exception  { scriptContext.sleep(50, 5); }
      }.run());
  }

  public void testStopThisWorkerThread() throws Exception {
    final ThreadContextLocator threadContextLocator =
      new StubThreadContextLocator();

    final ScriptContextImplementation scriptContext =
      new ScriptContextImplementation(
        null, null, threadContextLocator, null, null, null, null, null, null,
        null, null, null);

    try {
      scriptContext.stopThisWorkerThread();
      fail("Expected InvalidContextException");
    }
    catch (InvalidContextException e) {
    }

    threadContextLocator.set(m_threadContext);

    try {
      scriptContext.stopThisWorkerThread();
      fail("Expected ShutdownException");
    }
    catch (ShutdownException e) {
    }
  }
}
