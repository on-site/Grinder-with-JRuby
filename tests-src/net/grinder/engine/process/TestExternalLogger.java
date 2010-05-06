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

import net.grinder.common.LoggerStubFactory;
import net.grinder.common.Logger;
import net.grinder.testutility.CallData;
import net.grinder.testutility.RandomStubFactory;


/**
 * Unit test case for <code>ExternalLogger</code>.
 *
 * @author Philip Aston
 * @version $Revision: 4006 $
 */
public class TestExternalLogger extends TestCase {

  public void testProcessLogging() throws Exception {
    final LoggerStubFactory processLoggerFactory = new LoggerStubFactory();
    final Logger processLogger = processLoggerFactory.getLogger();

    final ThreadContextLocator threadContextLocator =
      new StubThreadContextLocator();

    final ExternalLogger externalLogger =
      new ExternalLogger(processLogger, threadContextLocator);

    externalLogger.output("Hello");

    processLoggerFactory.assertSuccess("output", "Hello");
    processLoggerFactory.assertNoMoreCalls();

    externalLogger.error("Hello again", Logger.TERMINAL);

    processLoggerFactory.assertSuccess("error", "Hello again",
                                       new Integer(Logger.TERMINAL));

    processLoggerFactory.assertNoMoreCalls();

    final Object errorLogWriter = externalLogger.getErrorLogWriter();
    final CallData callData1 =
      processLoggerFactory.assertSuccess("getErrorLogWriter");
    assertEquals(errorLogWriter, callData1.getResult());

    final Object outputLogWriter = externalLogger.getOutputLogWriter();
    final CallData callData2 =
      processLoggerFactory.assertSuccess("getOutputLogWriter");
    assertEquals(outputLogWriter, callData2.getResult());
  }

  public void testSeveralLoggers() throws Exception {
    final LoggerStubFactory processLoggerFactory = new LoggerStubFactory();
    final Logger processLogger = processLoggerFactory.getLogger();

    final ThreadLoggerStubFactory threadLoggerFactory1 =
      new ThreadLoggerStubFactory();
    final ThreadLogger threadLogger1 = threadLoggerFactory1.getLogger();

    final ThreadLoggerStubFactory threadLoggerFactory2 =
      new ThreadLoggerStubFactory();
    final ThreadLogger threadLogger2 = threadLoggerFactory2.getLogger();

    final ThreadContextStubFactory threadContextFactory1 =
      new ThreadContextStubFactory(threadLogger1);
    final ThreadContext threadContext1 =
      threadContextFactory1.getStub();

    final ThreadContextLocator threadContextLocator =
       new StubThreadContextLocator();

    final ExternalLogger externalLogger =
      new ExternalLogger(processLogger, threadContextLocator);

    threadContextLocator.set(threadContext1);

    externalLogger.output("Testing", Logger.LOG | Logger.TERMINAL);
    threadLoggerFactory1.assertSuccess(
      "output", "Testing", new Integer(Logger.LOG | Logger.TERMINAL));

    processLoggerFactory.assertNoMoreCalls();
    threadLoggerFactory1.assertNoMoreCalls();
    threadLoggerFactory2.assertNoMoreCalls();

    final Object errorLogWriter = externalLogger.getErrorLogWriter();
    final CallData callData =
      threadLoggerFactory1.assertSuccess("getErrorLogWriter");
    assertEquals(errorLogWriter, callData.getResult());

    processLoggerFactory.assertNoMoreCalls();
    threadLoggerFactory1.assertNoMoreCalls();
    threadLoggerFactory2.assertNoMoreCalls();

    threadContextLocator.set(null);

    externalLogger.error("Another test");
    processLoggerFactory.assertSuccess("error", "Another test");

    processLoggerFactory.assertNoMoreCalls();
    threadLoggerFactory1.assertNoMoreCalls();
    threadLoggerFactory2.assertNoMoreCalls();

    threadLogger2.setCurrentRunNumber(10);
    threadLoggerFactory1.assertNoMoreCalls();
  }

  public void testMultithreaded() throws Exception {

    final LoggerStubFactory processLoggerFactory = new LoggerStubFactory();
    final Logger processLogger = processLoggerFactory.getLogger();

    final ThreadContextLocator threadContextLocator =
      new StubThreadContextLocator();

    final ExternalLogger externalLogger =
      new ExternalLogger(processLogger, threadContextLocator);

    final TestThread threads[] = new TestThread[10];

    for (int i=0; i<threads.length; ++i) {
      threads[i] = new TestThread(externalLogger, threadContextLocator);
      threads[i].start();
    }

    for (int i=0; i<threads.length; ++i) {
      threads[i].join();
      assertTrue(threads[i].getOK());
    }

    processLoggerFactory.assertNoMoreCalls();
  }

  private static class TestThread extends Thread {
    private final ExternalLogger m_externalLogger;
    private final ThreadContextLocator m_threadContextLocator;
    private volatile boolean m_ok = false;

    public TestThread(ExternalLogger externalLogger,
                      ThreadContextLocator threadContextLocator) {

      m_externalLogger = externalLogger;
      m_threadContextLocator = threadContextLocator;
    }

    public void run() {
      final ThreadLoggerStubFactory threadLoggerFactory =
        new ThreadLoggerStubFactory();
      final ThreadLogger threadLogger = threadLoggerFactory.getLogger();

      final ThreadContextStubFactory threadContextFactory =
        new ThreadContextStubFactory(threadLogger);
      final ThreadContext threadContext =
        threadContextFactory.getStub();

      m_threadContextLocator.set(threadContext);

      for (int i=0; i<100; ++i) {
        m_externalLogger.output("Testing", Logger.TERMINAL);

        threadLoggerFactory.assertSuccess(
          "output", "Testing", new Integer(Logger.TERMINAL));

        final Object outputLogWriter = m_externalLogger.getOutputLogWriter();
        final CallData callData =
          threadLoggerFactory.assertSuccess("getOutputLogWriter");
        assertEquals(outputLogWriter, callData.getResult());

        threadLoggerFactory.assertNoMoreCalls();
      }

      m_ok = true;
    }

    public boolean getOK() {
      return m_ok;
    }
  }

  /**
   * Must be public so that override_ methods can be called
   * externally.
   */
  public static class ThreadContextStubFactory
    extends RandomStubFactory<ThreadContext> {

    private final ThreadLogger m_threadLogger;

    public ThreadContextStubFactory(ThreadLogger threadLogger) {
      super(ThreadContext.class);
      m_threadLogger = threadLogger;
    }

    public ThreadLogger override_getThreadLogger(Object proxy) {
      return m_threadLogger;
    }
  }
}
