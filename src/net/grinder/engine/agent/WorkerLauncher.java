// Copyright (C) 2004 - 2008 Philip Aston
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

import net.grinder.common.Logger;
import net.grinder.common.UncheckedInterruptedException;
import net.grinder.engine.common.EngineException;
import net.grinder.util.thread.Condition;
import net.grinder.util.thread.Executor;
import net.grinder.util.thread.InterruptibleRunnable;


/**
 * Manages launching a set of workers.
 *
 * @author Philip Aston
 * @version $Revision: 3940 $
 */
final class WorkerLauncher {

  private final Executor m_executor;
  private final WorkerFactory m_workerFactory;
  private final Condition m_notifyOnFinish;
  private final Logger m_logger;

  /**
   * Fixed size array with a slot for all potential workers. Synchronise on
   * m_workers before accessing entries. If an entry is null and its index is
   * less than m_nextWorkerIndex, the worker has finished or the WorkerLauncher
   * has been shutdown.
   */
  private final Worker[] m_workers;

  /**
   * The next worker to start. Only increases.
   */
  private int m_nextWorkerIndex = 0;

  public WorkerLauncher(int numberOfWorkers,
                        WorkerFactory workerFactory,
                        Condition notifyOnFinish,
                        Logger logger) {

    this(new Executor(1),
         numberOfWorkers,
         workerFactory,
         notifyOnFinish,
         logger);
  }

  /**
   * Package scope for unit tests.
   */
  WorkerLauncher(Executor executor,
                 int numberOfWorkers,
                 WorkerFactory workerFactory,
                 Condition notifyOnFinish,
                 Logger logger) {
    m_executor = executor;
    m_workerFactory = workerFactory;
    m_notifyOnFinish = notifyOnFinish;
    m_logger = logger;

    m_workers = new Worker[numberOfWorkers];
  }

  public void startAllWorkers() throws EngineException {
    startSomeWorkers(m_workers.length - m_nextWorkerIndex);
  }

  public boolean startSomeWorkers(int numberOfWorkers)
    throws EngineException {

    final int numberToStart =
      Math.min(numberOfWorkers, m_workers.length - m_nextWorkerIndex);

    for (int i = 0; i < numberToStart; ++i) {
      final int workerIndex = m_nextWorkerIndex;

      final Worker worker = m_workerFactory.create(System.out, System.err);

      synchronized (m_workers) {
        m_workers[workerIndex] = worker;
      }

      try {
        m_executor.execute(new WaitForWorkerTask(workerIndex));
      }
      catch (Executor.ShutdownException e) {
        m_logger.error("Executor unexpectedly shutdown");
        e.printStackTrace(m_logger.getErrorLogWriter());
        worker.destroy();
        return false;
      }

      m_logger.output("worker " + worker.getIdentity().getName() + " started");

      ++m_nextWorkerIndex;
    }

    return m_workers.length > m_nextWorkerIndex;
  }

  private final class WaitForWorkerTask implements InterruptibleRunnable {

    private final int m_workerIndex;

    public WaitForWorkerTask(int workerIndex) {
      m_workerIndex = workerIndex;
    }

    public void interruptibleRun() {
      final Worker worker;

      synchronized (m_workers) {
        worker = m_workers[m_workerIndex];
      }

      assert worker != null;

      try {
        worker.waitFor();
      }
      catch (UncheckedInterruptedException e) {
        // We're taking our worker down with us.
        worker.destroy();
      }

      synchronized (m_workers) {
        m_workers[m_workerIndex] = null;
      }

      if (allFinished()) {
        synchronized (m_notifyOnFinish) {
          m_notifyOnFinish.notifyAll();
        }
      }
    }
  }

  public boolean allFinished() {
    if (m_nextWorkerIndex < m_workers.length) {
      return false;
    }

    synchronized (m_workers) {
      for (int i = 0; i < m_workers.length; i++) {
        if (m_workers[i] != null) {
          return false;
        }
      }
    }

    return true;
  }

  /**
   * Used to be done in {@link #allFinished()} if our workers hand completed.
   * Moved to a separate method since:
   *
   * <ol>
   * <li>We need to shutdown the kernel even if we never started a worker.</li>
   * <li>Shutting down the kernel joins our
   * {@link net.grinder.engine.agent.WorkerLauncher.WaitForWorkerTask} threads,
   * and the last thread didn't complete as the caller of {@link #allFinished()}
   * was holding the <em>notifyOnFinish</em> {@link Condition}.</li>
   * </ol>
   *
   * <p>
   * Do not call this whilst holding the <em>notifyOnFinish</em>
   * {@link Condition}.
   * </p>
   */
  public void shutdown() {
    m_executor.gracefulShutdown();
  }

  public void dontStartAnyMore() {
    m_nextWorkerIndex = m_workers.length;
  }

  public void destroyAllWorkers() {
    dontStartAnyMore();

    synchronized (m_workers) {
      for (int i = 0; i < m_workers.length; i++) {
        if (m_workers[i] != null) {
          m_workers[i].destroy();
        }
      }
    }
  }
}
