// Copyright (C) 2000 - 2009 Philip Aston
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

import java.util.LinkedList;

import net.grinder.common.GrinderException;
import net.grinder.common.UncheckedInterruptedException;


/**
 * Thread-safe queue.
 *
 * @param <T> Entry type.
 *
 * @author Philip Aston
 * @version $Revision: 4003 $
 */
public final class ThreadSafeQueue<T> {

  private final LinkedList<T> m_messages = new LinkedList<T>();
  private final Condition m_condition = new Condition();
  private volatile boolean m_shutdown = false;

  /**
   * Constructor.
   *
   */
  public ThreadSafeQueue() {
  }

  /**
   * Queue the given item.
   *
   * @param item The object.
   * @exception ShutdownException If the queue has been shutdown.
   * @see #shutdown
   */
  public void queue(T item) throws ShutdownException {
    synchronized (getCondition()) {
      checkIfShutdown();
      m_messages.add(item);
      getCondition().notifyAll();
    }
  }

  /**
   * Dequeue an item.
   *
   * @param block
   *          {@code true} => block until message is available, {@code false} =>
   *          return {@code null} if no message is available.
   * @exception ShutdownException
   *              If the queue has been shutdown.
   * @return The dequeued object.
   * @see #shutdown
   */
  public T dequeue(boolean block) throws ShutdownException {
    synchronized (getCondition()) {
      while (!m_shutdown && block && m_messages.size() == 0) {
        try {
          getCondition().wait();
        }
        catch (InterruptedException e) {
          // Don't leave other threads dangling.
          shutdown();
          throw new UncheckedInterruptedException(e);
        }
      }

      checkIfShutdown();

      if (m_messages.size() == 0) {
        return null;
      }
      else {
        getCondition().notifyAll();
        return m_messages.removeFirst();
      }
    }
  }

  /**
   * Shutdown the {@code ThreadSafeQueue}. Any items in the queue are discarded.
   */
  public void shutdown() {
    synchronized (getCondition()) {
      m_shutdown = true;
      m_messages.clear();
      getCondition().notifyAll();
    }
  }

  /**
   * Wait until the queue is empty.
   */
  public void gracefulShutdown() {
    synchronized (getCondition()) {
      while (getSize() > 0) {
        getCondition().waitNoInterrruptException();
      }

      shutdown();
    }
  }

  /**
   * Get the lock object which is used to control and notify changes
   * to the queue.
   *
   * @return The lock object.
   */
  public Condition getCondition() {
    return m_condition;
  }

  /**
   * The size of the queue.
   *
   * @return The size of the queue.
   */
  public int getSize() {
    synchronized (getCondition()) {
      return m_messages.size();
    }
  }

  /**
   * Throw an ShutdownException if we are shutdown.
   *
   * @throws ShutdownException Thrown if the {@code ThreadSafeQueue} is
   * shutdown.
   */
  public void checkIfShutdown() throws ShutdownException {
    if (m_shutdown) {
      throw new ShutdownException("ThreadSafeQueue shutdown");
    }
  }

  /**
   * Exception that indicates {@code ThreadSafeQueue} has been shutdown.
   */
  public static final class ShutdownException extends GrinderException {
    private ShutdownException(String s) {
      super(s);
    }
  }
}
