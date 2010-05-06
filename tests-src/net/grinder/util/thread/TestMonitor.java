// Copyright (C) 2005 - 2009 Philip Aston
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

import java.util.TimerTask;

import net.grinder.common.UncheckedInterruptedException;
import net.grinder.testutility.Time;

import junit.framework.Assert;
import junit.framework.TestCase;


/**
 * Unit tests for {@link Condition}.
 *
 * @author Philip Aston
 * @version $Revision: 4007 $
 */
public class TestMonitor extends TestCase {

  public void testWaitNoInterruptException() throws Exception {
    final Condition monitor = new Condition();

    final DoWait wait1 = new DoWait(monitor);

    final Thread thread1 = new Thread(wait1);
    thread1.start();
    wait1.waitUntilWaiting(true);
    synchronized (monitor) {
      monitor.notify();
    }
    wait1.assertSuccess();
    thread1.join();

    final Thread thread2 = new Thread(wait1);
    thread2.start();
    wait1.waitUntilWaiting(true);
    synchronized (monitor) {
      thread2.interrupt();
    }
    wait1.assertException(UncheckedInterruptedException.class);
    thread1.join();

    final DoWait wait2 = new DoWait(monitor, 100);

    final Thread thread3 = new Thread(wait2);
    thread3.start();
    wait2.waitUntilWaiting(true);
    synchronized (monitor) {
      monitor.notify();
    }
    wait2.assertSuccess();
    thread3.join();

    final Thread thread4 = new Thread(wait2);
    thread4.start();
    wait2.waitUntilWaiting(true);
    synchronized (monitor) {
      thread4.interrupt();
    }
    wait2.assertException(UncheckedInterruptedException.class);
    thread4.join();

    assertTrue(new Time(100, 200) {
      public void doIt() throws Exception {
        wait2.run();
      }
    }.run());
  }

  private static final class DoWait extends TimerTask {
    private final Condition m_monitor;
    private final long m_time;
    private Throwable m_threw;
    private boolean m_waiting;

    public DoWait(Condition monitor) {
      this(monitor, -1);
    }

    private DoWait(Condition monitor, long time) {
      super();
      m_monitor = monitor;
      m_time = time;
    }

    public void waitUntilWaiting(boolean b) throws InterruptedException {
      synchronized (m_monitor) {
        while (m_waiting != b) {
          m_monitor.wait();
        }
      }
    }

    public void run() {
      m_threw = null;

      synchronized (m_monitor) {
        try {
          m_waiting = true;
          m_monitor.notifyAll();

          if (m_time >= 0) {
            m_monitor.waitNoInterrruptException(m_time);
          }
          else {
            m_monitor.waitNoInterrruptException();
          }
        }
        catch (Throwable t) {
          m_threw = t;
        }
        finally {
          m_waiting = false;
          m_monitor.notifyAll();
        }
      }
    }

    public void assertSuccess() throws InterruptedException {
      waitUntilWaiting(false);
      Assert.assertNull(m_threw);
    }

    public void assertException(Class<?> c) throws InterruptedException {
      waitUntilWaiting(false);
      Assert.assertTrue(c.isAssignableFrom(m_threw.getClass()));
    }
  }
}
