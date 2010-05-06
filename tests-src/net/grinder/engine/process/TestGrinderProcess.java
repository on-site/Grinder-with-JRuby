// Copyright (C) 2008 Philip Aston
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

import net.grinder.engine.process.GrinderProcess.ThreadSynchronisation;
import net.grinder.util.thread.Condition;
import junit.framework.TestCase;

/**
 * Unit tests for {@link GrinderProcess}
 *
 * @author Philip Aston
 * @version $Revision: 3995 $
 */
public class TestGrinderProcess extends TestCase {

  public void testThreaadSynchronisationZeroThreads() throws Exception {
    final Condition c = new Condition();

    final ThreadSynchronisation ts =
      new GrinderProcess.ThreadSynchronisation(c);

    assertEquals(0, ts.getTotalNumberOfThreads());
    assertEquals(0, ts.getNumberOfRunningThreads());
    assertTrue(ts.isReadyToStart());
    assertTrue(ts.isFinished());

    ts.startThreads();
    ts.awaitStart();
  }

  public void testThreaadSynchronisationNThreads() throws Exception {
    final Condition c = new Condition();
    final Thread[] threads = new Thread[100];

    final ThreadSynchronisation ts =
      new GrinderProcess.ThreadSynchronisation(c);

    for (int i = 0; i < threads.length; ++i) {
      threads[i] = new Thread(new MyRunnable(ts, i % 3 == 0));
    }

    assertEquals(100, ts.getTotalNumberOfThreads());
    assertEquals(100, ts.getNumberOfRunningThreads());
    assertFalse(ts.isReadyToStart());
    assertFalse(ts.isFinished());

    for (int i = 0; i < threads.length; ++i) {
      threads[i].start();
    }

    ts.startThreads();

    synchronized (c) {
      while (!ts.isFinished()) {
        c.waitNoInterrruptException();
      }
    }

    assertTrue(ts.isFinished());
    assertEquals(0, ts.getNumberOfRunningThreads());
    assertEquals(100, ts.getTotalNumberOfThreads());
  }

  private static class MyRunnable implements Runnable {
    private final ThreadSynchronisation m_ts;
    private final boolean m_failBeforeStart;

    public MyRunnable(ThreadSynchronisation ts, boolean failBeforeStart) {
      m_ts = ts;
      m_failBeforeStart = failBeforeStart;
      ts.threadCreated();
    }

    public void run() {
      shortSleep();

      if (m_failBeforeStart) {
        m_ts.threadFinished();
      }
      else {
        m_ts.awaitStart();

        shortSleep();

        m_ts.threadFinished();
      }
    }

    private void shortSleep() {
      try {
        Thread.sleep(10);
      }
      catch (InterruptedException e) {
      }
    }
  }
}
