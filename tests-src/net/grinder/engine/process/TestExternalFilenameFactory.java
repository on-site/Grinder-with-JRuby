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
import net.grinder.testutility.CallData;
import net.grinder.testutility.RandomStubFactory;


/**
 * Unit test case for <code>ExternalFilenameFactory</code>.
 *
 * @author Philip Aston
 * @version $Revision: 4006 $
 */
public class TestExternalFilenameFactory extends TestCase {

  public void testProcessFilenameFactory() throws Exception {
    final RandomStubFactory<FilenameFactory> filenameFactoryStubFactory =
      RandomStubFactory.create(FilenameFactory.class);

    final ThreadContextLocator threadContextLocator =
      new StubThreadContextLocator();

    final ExternalFilenameFactory externalFilenameFactory =
      new ExternalFilenameFactory(filenameFactoryStubFactory.getStub(),
                                  threadContextLocator);

    final String result1 = externalFilenameFactory.createFilename("Prefix");

    final CallData callData1 =
      filenameFactoryStubFactory.assertSuccess("createFilename", "Prefix");
    assertEquals(result1, callData1.getResult());
    filenameFactoryStubFactory.assertNoMoreCalls();

    final String result2 =
      externalFilenameFactory.createFilename("Prefix", "Suffix");

    final CallData callData2 =
      filenameFactoryStubFactory.assertSuccess("createFilename",
                                               "Prefix", "Suffix");
    assertEquals(result2, callData2.getResult());
    filenameFactoryStubFactory.assertNoMoreCalls();
  }

  public void testSeveralFilenameFactories() throws Exception {
    final RandomStubFactory<FilenameFactory> processFilenameFactoryStubFactory =
      RandomStubFactory.create(FilenameFactory.class);

    final RandomStubFactory<FilenameFactory> threadFilenameFactoryStubFactory1 =
      RandomStubFactory.create(FilenameFactory.class);

    final RandomStubFactory<FilenameFactory> threadFilenameFactoryStubFactory2 =
      RandomStubFactory.create(FilenameFactory.class);

    final ThreadContextStubFactory threadContextFactory1 =
      new ThreadContextStubFactory(threadFilenameFactoryStubFactory1.getStub());
    final ThreadContext threadContext1 =
      threadContextFactory1.getStub();

    final ThreadContextLocator threadContextLocator =
       new StubThreadContextLocator();

    final ExternalFilenameFactory externalFilenameFactory =
      new ExternalFilenameFactory(processFilenameFactoryStubFactory.getStub(),
                                  threadContextLocator);

    threadContextLocator.set(threadContext1);

    final String result1 = externalFilenameFactory.createFilename("p");
    final CallData callData1 =
      threadFilenameFactoryStubFactory1.assertSuccess("createFilename", "p");
    assertEquals(result1, callData1.getResult());
    processFilenameFactoryStubFactory.assertNoMoreCalls();
    threadFilenameFactoryStubFactory1.assertNoMoreCalls();
    threadFilenameFactoryStubFactory2.assertNoMoreCalls();

    final String result2 = externalFilenameFactory.createFilename("p", "s");
    final CallData callData2 =
      threadFilenameFactoryStubFactory1.assertSuccess("createFilename",
                                                      "p", "s");
    assertEquals(result2, callData2.getResult());
    processFilenameFactoryStubFactory.assertNoMoreCalls();
    threadFilenameFactoryStubFactory1.assertNoMoreCalls();
    threadFilenameFactoryStubFactory2.assertNoMoreCalls();

    threadContextLocator.set(null);

    final String result3 =
      externalFilenameFactory.createFilename("foo", "bah");
    final CallData callData3 =
      processFilenameFactoryStubFactory.assertSuccess("createFilename",
                                                      "foo", "bah");
    assertEquals(result3, callData3.getResult());
    processFilenameFactoryStubFactory.assertNoMoreCalls();
    threadFilenameFactoryStubFactory1.assertNoMoreCalls();
    threadFilenameFactoryStubFactory2.assertNoMoreCalls();

    threadFilenameFactoryStubFactory2.getStub().createFilename("lah");
    threadFilenameFactoryStubFactory1.assertNoMoreCalls();
  }

  public void testMultithreaded() throws Exception {
    final RandomStubFactory<FilenameFactory> processFilenameFactoryStubFactory =
      RandomStubFactory.create(FilenameFactory.class);

    final ThreadContextLocator threadContextLocator =
      new StubThreadContextLocator();

    final ExternalFilenameFactory externalFilenameFactory =
      new ExternalFilenameFactory(processFilenameFactoryStubFactory.getStub(),
                                  threadContextLocator);

    final TestThread threads[] = new TestThread[10];

    for (int i=0; i<threads.length; ++i) {
      threads[i] =
        new TestThread(externalFilenameFactory, threadContextLocator);
      threads[i].start();
    }

    for (int i=0; i<threads.length; ++i) {
      threads[i].join();
      assertTrue(threads[i].getOK());
    }

    processFilenameFactoryStubFactory.assertNoMoreCalls();
  }

  private static class TestThread extends Thread {
    private final ExternalFilenameFactory m_externalFilenameFactory;
    private final ThreadContextLocator m_threadContextLocator;
    private volatile boolean m_ok = false;

    public TestThread(ExternalFilenameFactory externalFilenameFactory,
                      ThreadContextLocator threadContextLocator) {

      m_externalFilenameFactory = externalFilenameFactory;
      m_threadContextLocator = threadContextLocator;
    }

    public void run() {
      final RandomStubFactory<FilenameFactory>
        threadFilenameFactoryStubFactory =
          RandomStubFactory.create(FilenameFactory.class);
      final ThreadContextStubFactory threadContextFactory =
        new ThreadContextStubFactory(
          threadFilenameFactoryStubFactory.getStub());
      final ThreadContext threadContext =
        threadContextFactory.getStub();

      m_threadContextLocator.set(threadContext);

      for (int i=0; i<100; ++i) {
        final String result1 =
          m_externalFilenameFactory.createFilename("blab blah", "blugh");

        final CallData callData1 =
          threadFilenameFactoryStubFactory.assertSuccess("createFilename",
                                                         "blab blah", "blugh");

        assertEquals(result1, callData1.getResult());

        final String result2 = m_externalFilenameFactory.createFilename("xxx");

        final CallData callData2 =
          threadFilenameFactoryStubFactory.assertSuccess("createFilename",
                                                         "xxx");
        assertEquals(result2, callData2.getResult());
        threadFilenameFactoryStubFactory.assertNoMoreCalls();
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

    private final FilenameFactory m_filenameFactory;

    public ThreadContextStubFactory(FilenameFactory filenameFactory) {
      super(ThreadContext.class);
      m_filenameFactory = filenameFactory;
    }

    public FilenameFactory override_getFilenameFactory(Object proxy) {
      return m_filenameFactory;
    }
  }
}
