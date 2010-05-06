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

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

/**
 *  Unit test case for <code>ThreadSafeQueue</code>.
 *
 * @author Philip Aston
 * @version $Revision: 4007 $
 **/
public class TestThreadSafeQueue extends TestCase {

  public TestThreadSafeQueue(String name) {
    super(name);
  }

  public void testConstructionAndBasicAccessors() throws Exception {
    final ThreadSafeQueue<Object> threadSafeQueue =
      new ThreadSafeQueue<Object>();
    assertNotNull(threadSafeQueue.getCondition());
    assertSame(threadSafeQueue.getCondition(), threadSafeQueue.getCondition());
    assertEquals(0, threadSafeQueue.getSize());
  }

  public void testQueueAndDequeSingleThreaded() throws Exception {
    final ThreadSafeQueue<Object> threadSafeQueue =
      new ThreadSafeQueue<Object>();

    final Object o1 = new Object();
    final Object o2 = new Object();
    final Object o3 = new Object();

    threadSafeQueue.queue(o1);
    threadSafeQueue.queue(o2);
    threadSafeQueue.queue(o3);
    assertEquals(3, threadSafeQueue.getSize());

    assertSame(o1, threadSafeQueue.dequeue(false));
    assertSame(o2, threadSafeQueue.dequeue(true));
    assertEquals(1, threadSafeQueue.getSize());
    assertSame(o3, threadSafeQueue.dequeue(false));

    assertNull(threadSafeQueue.dequeue(false));
    assertEquals(0, threadSafeQueue.getSize());

    threadSafeQueue.queue(o2);
    threadSafeQueue.queue(o3);
    assertEquals(2, threadSafeQueue.getSize());

    assertSame(o2, threadSafeQueue.dequeue(false));
    assertSame(o3, threadSafeQueue.dequeue(true));
  }

  public void testQueueAndDequeMultiThreaded() throws Exception {

    final ThreadSafeQueue<MyMessage> threadSafeQueue =
      new ThreadSafeQueue<MyMessage>();
    final List<Object> answerList = new ArrayList<Object>();

    final Queuer[] queuers = new Queuer[10];
    final Dequeuer dequeuer = new Dequeuer(100, threadSafeQueue, answerList);
    final Thread[] threads = new Thread[queuers.length + 1];

    for (int i=0; i<queuers.length; ++i) {
      queuers[i] = new Queuer(i, 10, threadSafeQueue);
      threads[i] = new Thread(queuers[i]);
    }

    threads[queuers.length] = new Thread(dequeuer);

    for (int i=0; i<threads.length; ++i) {
      threads[i].start();
    }

    for (int i=0; i<threads.length; ++i) {
      threads[i].join();
    }

    for (int i=0; i<queuers.length; ++i) {
      assertNull(queuers[i].getException());
    }

    assertNull(dequeuer.getException());

    assertEquals(0, threadSafeQueue.getSize());
    assertEquals(100, answerList.size());

    final MyMessage[] messages = new MyMessage[100];
    answerList.toArray(messages);

    final int[] messageNumbers = new int[queuers.length];
    Arrays.fill(messageNumbers, 0);

    for (int i=0; i<messages.length; ++i) {
      final int queuerNumber = messages[i].getQueuerNumber();
      final int messageNumber = messages[i].getMessageNumber();

      assertEquals(answerList.toString(), messageNumbers[queuerNumber],
                   messageNumber);
      messageNumbers[queuerNumber]++;
    }

    for (int i=0; i<messageNumbers.length; ++i) {
      assertEquals(10, messageNumbers[i]);
    }

    assertNull(threadSafeQueue.dequeue(false));
  }

  public void testQueueWithMutex() throws Exception {

    final ThreadSafeQueue<MyMessage> threadSafeQueue =
      new ThreadSafeQueue<MyMessage>();

    final Queuer[] queuers = new Queuer[10];
    final Thread[] threads = new Thread[queuers.length ];

    for (int i=0; i<queuers.length; ++i) {
      queuers[i] = new Queuer(i, 10, threadSafeQueue) {
          public void run() {
            synchronized(threadSafeQueue.getCondition()) {
              super.run();
            }
          }
        };

      threads[i] = new Thread(queuers[i]);
    }

    for (int i=0; i<threads.length; ++i) {
      threads[i].start();
    }

    for (int i=0; i<threads.length; ++i) {
      threads[i].join();
    }

    for (int i=0; i<queuers.length; ++i) {
      assertNull(queuers[i].getException());
    }

    for (int i=0; i<100; ++i) {
      final MyMessage message = threadSafeQueue.dequeue(true);
      assertEquals(i % 10, message.getMessageNumber());
    }

    assertNull(threadSafeQueue.dequeue(false));
  }

  private static final class MyMessage {
    private final int m_queuerNumber;
    private final int m_messageNumber;

    public MyMessage(int queuerNumber, int messageNumber) {
      m_queuerNumber = queuerNumber;
      m_messageNumber = messageNumber;
    }

    public int getQueuerNumber() {
      return m_queuerNumber;
    }

    public int getMessageNumber() {
      return m_messageNumber;
    }

    public String toString() {
      return "(" + m_queuerNumber + ", " + m_messageNumber + ")";
    }
  }

  private class Queuer implements Runnable {
    private final int m_queuerNumber;
    private final ThreadSafeQueue<MyMessage> m_queue;
    private final int m_numberOfMessages;
    private Exception m_exception;

    public Queuer(int queuerNumber, int numberOfMessages,
                  ThreadSafeQueue<MyMessage> queue) {
      m_queuerNumber = queuerNumber;
      m_numberOfMessages = numberOfMessages;
      m_queue = queue;
    }

    public void run() {
      try {
        for (int i=0; i<m_numberOfMessages; ++i) {
          m_queue.queue(new MyMessage(m_queuerNumber, i));
          if (i % 3 == 0) {
            Thread.sleep(1);
            Thread.yield();
          }
        }
      }
      catch (Exception e) {
        m_exception = e;
      }
    }

    public final Exception getException() {
      return m_exception;
    }
  }

  private static final class Dequeuer implements Runnable {
    private final int m_numberOfMessages;
    private final ThreadSafeQueue<MyMessage> m_queue;
    private final List<Object> m_answerList;
    private Exception m_exception;

    public Dequeuer(int numberOfMessages, ThreadSafeQueue<MyMessage> queue,
                    List<Object> answerList) {
      m_numberOfMessages = numberOfMessages;
      m_queue = queue;
      m_answerList = answerList;
    }

    public void run() {
      try {
        for (int i=0; i<m_numberOfMessages; ++i) {
          final Object o = m_queue.dequeue(true);

          synchronized(m_answerList) {
            m_answerList.add(o);
          }

          if (i % 3 == 0) {
            Thread.yield();
          }
        }
      }
      catch (Exception e) {
        m_exception = e;
      }
    }

    public Exception getException() {
      return m_exception;
    }
  }

  public void testShutdownSingleThreaded() throws Exception {
    final ThreadSafeQueue<Object> threadSafeQueue = new ThreadSafeQueue<Object>();
    threadSafeQueue.shutdown();

    try {
      threadSafeQueue.queue(new Object());
      fail("Expected ShutdownException");
    }
    catch (ThreadSafeQueue.ShutdownException e) {
    }

    try {
      threadSafeQueue.dequeue(false);
      fail("Expected ShutdownException");
    }
    catch (ThreadSafeQueue.ShutdownException e) {
    }

    try {
      threadSafeQueue.dequeue(true);
      fail("Expected ShutdownException");
    }
    catch (ThreadSafeQueue.ShutdownException e) {
    }

    // Shutting down again should be a no-op.
    threadSafeQueue.shutdown();
    threadSafeQueue.shutdown();
  }

  public void testShutdownMultiThreaded() throws Exception {
    final ThreadSafeQueue<Object> threadSafeQueue = new ThreadSafeQueue<Object>();

    synchronized(threadSafeQueue.getCondition()) {
      new Thread() {
        public void run() {
          // shutdown won't do anything until dequeue blocks and
          // releases the mutex we hold.
          threadSafeQueue.shutdown();
        }
      }.start();

      final Object o = new Object();
      threadSafeQueue.queue(o);
      assertEquals(o, threadSafeQueue.dequeue(true));

      // Still holding the mutex, non-blocking dequeue should report
      // queue is empty.
      assertNull(threadSafeQueue.dequeue(false));

      try {
        // Blocking dequeue will release mutex, allowing other thread
        // to shutdown the queue.
        threadSafeQueue.dequeue(true);
        fail("Expected ShutdownException");
      }
      catch (ThreadSafeQueue.ShutdownException e) {
      }
    }

    try {
      threadSafeQueue.dequeue(false);
      fail("Expected ShutdownException");
    }
    catch (ThreadSafeQueue.ShutdownException e) {
    }

    // Hack to return result from thread.
    final Object[] returnHolder = new Object[1];

    final Thread t = new Thread() {
        public void run() {
          // Should also be shutdown for other threads.
          try {
            threadSafeQueue.dequeue(false);
            returnHolder[0] = Boolean.TRUE;
          }
          catch (ThreadSafeQueue.ShutdownException e) {
          }
        }
      };

    t.start();
    t.join();

    assertNull(returnHolder[0]);
  }
}
