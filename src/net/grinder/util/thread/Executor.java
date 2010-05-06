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

import net.grinder.common.GrinderException;
import net.grinder.common.UncheckedInterruptedException;


/**
 * Work queue and worker threads.
 *
 * @author Philip Aston
 * @version $Revision: 4003 $
 */
public final class Executor {

  private final ThreadSafeQueue<InterruptibleRunnable> m_workQueue;
  private final ThreadPool m_threadPool;

  /**
   * Constructor.
   *
   * @param numberOfThreads Number of worker threads to use.
   */
  public Executor(int numberOfThreads) {

    this(new ThreadSafeQueue<InterruptibleRunnable>(), numberOfThreads);
  }

  /**
   * Constructor. Allows unit tests to provide different work queue
   * implementation.
   *
   * @param workQueue Queue to use.
   * @param numberOfThreads Number of worker threads to use.
   */
  Executor(ThreadSafeQueue<InterruptibleRunnable> workQueue,
           int numberOfThreads) {

    m_workQueue = workQueue;

    final ThreadPool.InterruptibleRunnableFactory runnableFactory =
      new ThreadPool.InterruptibleRunnableFactory() {
        public InterruptibleRunnable create() {
          return new ExecutorRunnable();
        }
      };

    m_threadPool = new ThreadPool("Executor", numberOfThreads, runnableFactory);
    m_threadPool.start();
  }

  /**
   * Queue some work.
   *
   * @param work
   *          The work
   * @throws ShutdownException
   *           If the Executor has been stopped.
   */
  public void execute(InterruptibleRunnable work) throws ShutdownException {
    if (m_threadPool.isStopped()) {
      throw new ShutdownException("Executor is stopped");
    }

    try {
      m_workQueue.queue(work);
    }
    catch (ThreadSafeQueue.ShutdownException e) {
      throw new ShutdownException("Executor is stopped", e);
    }
  }

  /**
   * Shut down this executor, waiting for work to complete.
   *
   */
  public void gracefulShutdown() {
    try {
      m_workQueue.gracefulShutdown();
    }
    finally {
      m_threadPool.stopAndWait();
    }
  }

  /**
   * Shut down this executor, discarding any outstanding work.
   */
  public void forceShutdown() {
    m_workQueue.shutdown();
    m_threadPool.stop();
  }

  private class ExecutorRunnable implements InterruptibleRunnable {

    public void interruptibleRun() {
      while (true) {
        final InterruptibleRunnable runnable;

        try {
          runnable = m_workQueue.dequeue(true);
        }
        catch (ThreadSafeQueue.ShutdownException e) {
          // We've been shut down, exit the thread cleanly.
          break;
        }
        catch (UncheckedInterruptedException e) {
          // We've been interrupted, exit the thread cleanly.
          forceShutdown();
          break;
        }

        runnable.interruptibleRun();
      }
    }
  }

  /**
   * Exception that indicates <code>Executor</code> has been shutdown.
   */
  public static final class ShutdownException extends GrinderException {

    private ShutdownException(String s) {
      super(s);
    }

    private ShutdownException(String s, Exception e) {
      super(s, e);
    }
  }
}
