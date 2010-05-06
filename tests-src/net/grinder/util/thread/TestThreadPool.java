// Copyright (C) 2003 - 2008 Philip Aston
// Copyright (C) 2005 Martin Wagner
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

package net.grinder.util.thread;

import net.grinder.common.UncheckedInterruptedException;
import junit.framework.TestCase;

/**
 *  Unit tests for <code>ThreadPool</code>.
 *
 * @author Philip Aston
 * @version $Revision: 3938 $
 */
public class TestThreadPool extends TestCase {

  public TestThreadPool(String name) throws Exception {
    super(name);
  }

  private static final class NullRunnable
    implements InterruptibleRunnable {

    public void interruptibleRun() {
    }
  }

  private int m_count;

  private final class CountingRunnable implements InterruptibleRunnable {

    public void interruptibleRun() {
      for (int i=0; i<20; ++i) {
        synchronized(TestThreadPool.this) {
          ++m_count;
        }

        try {
          Thread.sleep(1);
        }
        catch (InterruptedException e) {
          // Deliberately deaf.
        }
      }
    }
  }

  private static final class ChainInterruptRunnable
    implements InterruptibleRunnable {

    private final Thread m_delegate;

    public ChainInterruptRunnable(Thread delegate) {
      m_delegate = delegate;
    }

    public void interruptibleRun() {
      synchronized (this) {
        try {
          wait();
        }
        catch (InterruptedException e) {
          m_delegate.interrupt();
        }
      }
    }
  }

  private abstract class TestRunnableFactory
    implements ThreadPool.InterruptibleRunnableFactory {

    private int m_callCount = 0;

    public InterruptibleRunnable create() {
      ++m_callCount;
      try {
        return doCreate();
      }
      catch (Exception e) {
        e.printStackTrace();
        return null;
      }
    }

    public abstract InterruptibleRunnable doCreate();

    public int getCallCount() {
      return m_callCount;
    }
  }

  public void testWithNullRunnable() throws Exception {

    final TestRunnableFactory runnableFactory =
      new TestRunnableFactory() {
        public InterruptibleRunnable doCreate() { return new NullRunnable(); }
      };

    final ThreadPool threadPool = new ThreadPool("Test", 5, runnableFactory);

    assertEquals(5, runnableFactory.getCallCount());
    assertNotNull(threadPool.getThreadGroup());
    assertTrue(!threadPool.isStopped());

    // Quote from JDK documentation on ThreadGroup.activeCount:
    // "Due to the inherently imprecise nature of the result, it is
    // recommended that this method only be used for informational purposes."
    // Assertion fails on Linux and under JRockit.
    // assertEquals(5, threadPool.getThreadGroup().activeCount());

    threadPool.start();

    try {
      threadPool.start();
      fail("Expected IllegalStateException");
    }
    catch (IllegalStateException e) {
    }

    assertTrue(!threadPool.isStopped());

    while (threadPool.getThreadGroup().activeCount() > 0) {
      Thread.sleep(10);
    }

    threadPool.stopAndWait();
    assertTrue(threadPool.isStopped());
    assertEquals(0, threadPool.getThreadGroup().activeCount());

    try {
      threadPool.start();
      fail("Expected IllegalStateException");
    }
    catch (IllegalStateException e) {
    }
  }

  public void testWithCountingRunnable() throws Exception {

    final TestRunnableFactory runnableFactory =
      new TestRunnableFactory() {
        public InterruptibleRunnable doCreate() {
          return new CountingRunnable();
        }
      };

    final ThreadPool threadPool = new ThreadPool("Test", 10, runnableFactory);

    assertEquals(10, runnableFactory.getCallCount());

    // Quote from JDK documentation on ThreadGroup.activeCount:
    // "Due to the inherently imprecise nature of the result, it is
    // recommended that this method only be used for informational purposes."
    // Assertion fails on Linux and under JRockit.
    //assertEquals(10, threadPool.getThreadGroup().activeCount());

    threadPool.start();

    threadPool.stop();

    assertTrue(threadPool.isStopped());

    while (threadPool.getThreadGroup().activeCount() > 0) {
      Thread.sleep(10);
    }

    threadPool.stopAndWait();

    assertTrue(threadPool.isStopped());

    // Our runnable ignores interruptions, so count should be 200.
    assertEquals(200, m_count);
  }

  public void testInterruption() throws Exception {

    final TestRunnableFactory runnableFactory =
      new TestRunnableFactory() {
        public InterruptibleRunnable doCreate() {
          return new ChainInterruptRunnable(Thread.currentThread());
        }
      };

    final ThreadPool threadPool = new ThreadPool("Test", 10, runnableFactory);

    threadPool.start();

    try {
      threadPool.stopAndWait();
      // Threads all died early, so we didn't join().
      assertTrue(Thread.interrupted());
    }
    catch (UncheckedInterruptedException e) {
    }

    // Clear interrupt status as it might have been set by other threads.
    Thread.interrupted();
  }
}

