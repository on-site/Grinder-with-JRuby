// Copyright (C) 2003 - 2009 Philip Aston
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

import junit.framework.TestCase;


/**
 *  Unit tests for <code>Executor</code>.
 *
 * @author Philip Aston
 * @version $Revision: 4007 $
 */
public class TestExecutor extends TestCase {

  private int m_counter = 0;

  private class IncrementCounter implements InterruptibleRunnable {

    private final int m_sleep;

    public IncrementCounter(int sleep) {
      m_sleep = sleep;
    }

    public void interruptibleRun() {
      synchronized (TestExecutor.this) {
        ++m_counter;
        try {
          Thread.sleep(m_sleep);
        }
        catch (InterruptedException e) {
          // Exit.
        }
      }
    }
  }

  public void testExecuteAndGracefulShutdown() throws Exception {
    final Executor executor = new Executor(5);

    for (int i=0; i<50; ++i) {
      executor.execute(new IncrementCounter(1));
    }

    executor.gracefulShutdown();

    assertEquals(50, m_counter);
  }


  public void testWorkQueueShutdown() throws Exception {

    // The work queue can be shutdown is if the kernel is in the
    // middle of shutting down. Another thread in execute() could be
    // just about to queue some work.

    final ThreadSafeQueue<InterruptibleRunnable> myQueue =
      new ThreadSafeQueue<InterruptibleRunnable>();

    final Executor executor = new Executor(myQueue, 5);

    executor.execute(new IncrementCounter(1));

    myQueue.shutdown();

    try {
      executor.execute(new IncrementCounter(1));
      fail("Expected ShutdownException");
    }
    catch (Executor.ShutdownException e) {
    }

    executor.forceShutdown();
  }

  public void testForceShutdown() throws Exception {
    final Executor executor = new Executor(2);

    for (int i=0; i<50; ++i) {
      executor.execute(new IncrementCounter(10));
    }
    Thread.sleep(20);

    executor.forceShutdown();

    assertTrue(m_counter != 50);

    try {
      executor.execute(new IncrementCounter(10));
      fail("Expected ShutdownException");
    }
    catch (Executor.ShutdownException e) {
    }
  }
}
