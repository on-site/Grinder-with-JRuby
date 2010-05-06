// Copyright (C) 2001 - 2009 Philip Aston
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

package net.grinder.engine.communication;

import java.io.Serializable;

import junit.framework.TestCase;

import net.grinder.common.GrinderProperties;
import net.grinder.common.Logger;
import net.grinder.common.LoggerStubFactory;
import net.grinder.communication.CommunicationException;
import net.grinder.communication.Message;
import net.grinder.communication.MessageDispatchSender;
import net.grinder.messages.agent.ResetGrinderMessage;
import net.grinder.messages.agent.StartGrinderMessage;
import net.grinder.messages.agent.StopGrinderMessage;
import net.grinder.util.thread.Condition;


/**
 * Unit test case for <code>ConsoleListener</code>.
 *
 * @author Philip Aston
 * @version $Revision: 4004 $
 */
public class TestConsoleListener extends TestCase {

  private final LoggerStubFactory m_loggerFactory = new LoggerStubFactory();
  private final Logger m_logger = m_loggerFactory.getLogger();

  protected void setUp() throws Exception {
    m_loggerFactory.resetCallHistory();
  }

  public void testConstruction() throws Exception {
    final Condition myCondition = new Condition();

    new ConsoleListener(myCondition, m_logger);

    m_loggerFactory.assertNoMoreCalls();
  }

  public void testSendNotification() throws Exception {
    final Condition myCondition = new Condition();
    final ConsoleListener listener = new ConsoleListener(myCondition, m_logger);

    final MessageDispatchSender messageDispatcher = new MessageDispatchSender();
    listener.registerMessageHandlers(messageDispatcher);

    final WaitForNotification notified = new WaitForNotification(myCondition);

    messageDispatcher.send(new StopGrinderMessage());

    assertTrue(notified.wasNotified());
  }

  public void testCheckForMessageAndReceive() throws Exception {

    final Condition myCondition = new Condition();
    final ConsoleListener listener = new ConsoleListener(myCondition, m_logger);

    assertFalse(listener.checkForMessage(ConsoleListener.ANY));
    assertFalse(listener.checkForMessage(ConsoleListener.RESET |
                                         ConsoleListener.SHUTDOWN));
    assertFalse(listener.checkForMessage(ConsoleListener.SHUTDOWN));

    final MessageDispatchSender messageDispatcher = new MessageDispatchSender();
    listener.registerMessageHandlers(messageDispatcher);

    messageDispatcher.send(
      new StartGrinderMessage(new GrinderProperties(), -1));
    messageDispatcher.send(new MyMessage());
    messageDispatcher.send(new ResetGrinderMessage());

    m_loggerFactory.assertSuccess("output", String.class);
    m_loggerFactory.assertSuccess("output", String.class);
    m_loggerFactory.assertNoMoreCalls();

    assertFalse(listener.checkForMessage(ConsoleListener.ANY ^
                                         (ConsoleListener.START |
                                          ConsoleListener.RESET)));
    assertTrue(listener.checkForMessage(ConsoleListener.START |
                                        ConsoleListener.STOP));
    assertTrue(listener.received(ConsoleListener.START));
    assertFalse(listener.received(ConsoleListener.STOP));
    assertTrue(listener.received(ConsoleListener.ANY));
    assertFalse(listener.received(ConsoleListener.STOP |
                                 ConsoleListener.RESET));
    assertFalse(listener.received(ConsoleListener.STOP));
    assertFalse(listener.received(ConsoleListener.SHUTDOWN));
    assertFalse(listener.received(ConsoleListener.RESET));

    assertFalse(listener.checkForMessage(ConsoleListener.START));
    assertFalse(listener.received(ConsoleListener.ANY));
    assertFalse(listener.received(ConsoleListener.START));

    assertTrue(listener.checkForMessage(ConsoleListener.RESET));
    assertTrue(listener.received(ConsoleListener.RESET));
    assertTrue(listener.received(ConsoleListener.RESET));

    assertFalse(listener.checkForMessage(ConsoleListener.RESET));
    assertFalse(listener.received(ConsoleListener.RESET));

    messageDispatcher.send(
      new StartGrinderMessage(new GrinderProperties(), -1));
    messageDispatcher.send(new ResetGrinderMessage());

    m_loggerFactory.assertSuccess("output", String.class);
    m_loggerFactory.assertSuccess("output", String.class);
    m_loggerFactory.assertNoMoreCalls();

    assertTrue(listener.checkForMessage(ConsoleListener.RESET |
                                        ConsoleListener.START));
    messageDispatcher.send(new ResetGrinderMessage());

    m_loggerFactory.assertSuccess("output", String.class);
    m_loggerFactory.assertNoMoreCalls();

    assertTrue(listener.checkForMessage(ConsoleListener.RESET |
                                        ConsoleListener.START));
    assertTrue(listener.received(ConsoleListener.RESET));
    assertFalse(listener.checkForMessage(ConsoleListener.RESET |
                                         ConsoleListener.START));
    assertFalse(listener.received(ConsoleListener.START));

    messageDispatcher.shutdown();

    assertTrue(listener.checkForMessage(ConsoleListener.SHUTDOWN));
    assertTrue(listener.received(ConsoleListener.SHUTDOWN));
    assertTrue(listener.received(ConsoleListener.SHUTDOWN));
  }

  public void testDiscardMessages() throws Exception {
    final Condition myCondition = new Condition();
    final ConsoleListener listener = new ConsoleListener(myCondition, m_logger);

    assertFalse(listener.checkForMessage(ConsoleListener.ANY));
    assertFalse(listener.checkForMessage(ConsoleListener.RESET |
                                         ConsoleListener.SHUTDOWN));
    assertFalse(listener.checkForMessage(ConsoleListener.SHUTDOWN));

    final MessageDispatchSender messageDispatcher = new MessageDispatchSender();
    listener.registerMessageHandlers(messageDispatcher);

    listener.discardMessages(ConsoleListener.ANY);

    messageDispatcher.send(
      new StartGrinderMessage(new GrinderProperties(), -1));
    messageDispatcher.send(new MyMessage());
    messageDispatcher.send(new ResetGrinderMessage());

    assertTrue(listener.checkForMessage(ConsoleListener.START |
                                        ConsoleListener.STOP));
    assertTrue(listener.received(ConsoleListener.START));
    assertFalse(listener.received(ConsoleListener.RESET));

    listener.discardMessages(ConsoleListener.RESET);

    assertFalse(listener.checkForMessage(ConsoleListener.RESET));
    assertFalse(listener.received(ConsoleListener.RESET));

    messageDispatcher.send(new ResetGrinderMessage());

    assertTrue(listener.checkForMessage(ConsoleListener.RESET));

    listener.discardMessages(ConsoleListener.RESET);

    assertFalse(listener.received(ConsoleListener.RESET));

    listener.discardMessages(ConsoleListener.RESET);

    assertFalse(listener.received(ConsoleListener.RESET));

    messageDispatcher.shutdown();

    assertTrue(listener.checkForMessage(ConsoleListener.SHUTDOWN));
    assertTrue(listener.received(ConsoleListener.SHUTDOWN));
    listener.discardMessages(ConsoleListener.SHUTDOWN);
    assertFalse(listener.received(ConsoleListener.SHUTDOWN));
  }

  public void testWaitForMessage() throws Exception {
    final Condition myCondition = new Condition();
    final ConsoleListener listener = new ConsoleListener(myCondition, m_logger);
    final MessageDispatchSender messageDispatcher = new MessageDispatchSender();
    listener.registerMessageHandlers(messageDispatcher);

    final Thread t = new Thread() {
        public void run() {
          // We synchronise to ensure main thread is blocked in
          // waitForMessage();
          synchronized (myCondition) {
            try {
              messageDispatcher.send(
                new StartGrinderMessage(new GrinderProperties(), -1));
            }
            catch (CommunicationException e) {
              e.printStackTrace();
            }
          }
        }
    };

    synchronized (myCondition) {
      t.start();
      listener.waitForMessage();
    }

    assertTrue(listener.received(ConsoleListener.START));
  }

  private static final class MyMessage implements Message, Serializable {
  }

  public void testDispatcherShutdown() throws Exception {

    final Condition myCondition = new Condition();
    final ConsoleListener listener = new ConsoleListener(myCondition, m_logger);
    final MessageDispatchSender messageDispatcher = new MessageDispatchSender();
    listener.registerMessageHandlers(messageDispatcher);

    final WaitForNotification notified = new WaitForNotification(myCondition);

    messageDispatcher.shutdown();

    assertTrue(notified.wasNotified());

    m_loggerFactory.assertSuccess("output", String.class, Integer.class);

    m_loggerFactory.assertNoMoreCalls();

    assertFalse(listener.checkForMessage(ConsoleListener.ANY ^
                                          ConsoleListener.SHUTDOWN));
    assertTrue(listener.checkForMessage(ConsoleListener.SHUTDOWN));
    assertTrue(listener.received(ConsoleListener.SHUTDOWN));
    assertFalse(listener.checkForMessage(ConsoleListener.SHUTDOWN));
    assertFalse(listener.received(ConsoleListener.SHUTDOWN));
  }

  public void testShutdown() throws Exception {

    final Condition myCondition = new Condition();
    final ConsoleListener listener = new ConsoleListener(myCondition, m_logger);

    final WaitForNotification notified = new WaitForNotification(myCondition);

    listener.shutdown();

    assertTrue(notified.wasNotified());

    m_loggerFactory.assertNoMoreCalls();

    assertFalse(listener.checkForMessage(ConsoleListener.ANY ^
                                          ConsoleListener.SHUTDOWN));
    assertTrue(listener.checkForMessage(ConsoleListener.SHUTDOWN));
    assertTrue(listener.received(ConsoleListener.SHUTDOWN));
    assertFalse(listener.checkForMessage(ConsoleListener.SHUTDOWN));
    assertFalse(listener.received(ConsoleListener.SHUTDOWN));
  }


  private static class WaitForNotification implements Runnable {
    private final Thread m_thread;
    private final Object m_condition;
    private boolean m_started = false;
    private boolean m_notified = false;

    public WaitForNotification(Object condition) throws InterruptedException {
      m_condition = condition;

      m_thread = new Thread(this);
      m_thread.start();

      synchronized (m_condition) {
        while (!m_started) {
          m_condition.wait();
        }
      }
    }

    public boolean wasNotified() throws InterruptedException {
      m_thread.join();

      return m_notified;
    }

    public final void run() {
      synchronized(m_condition) {
        final long startTime = System.currentTimeMillis();
        final long maximumTime = 10000;
        m_started = true;
        m_condition.notifyAll();

        try {
          m_condition.wait(maximumTime);

          if (System.currentTimeMillis() - startTime < maximumTime) {
            m_notified = true;
          }
        }
        catch (InterruptedException e) {
        }
      }
    }
  }

  public void testGetLastStartGrinderMessage() throws Exception {

    final ConsoleListener listener =
      new ConsoleListener(new Condition(), m_logger);

    final Message m1 = new StartGrinderMessage(new GrinderProperties(), -1);
    final Message m2 = new StartGrinderMessage(new GrinderProperties(), -1);
    final Message m3 = new MyMessage();

    final MessageDispatchSender messageDispatcher = new MessageDispatchSender();
    listener.registerMessageHandlers(messageDispatcher);

    assertNull(listener.getLastStartGrinderMessage());

    messageDispatcher.send(m1);
    assertEquals(m1, listener.getLastStartGrinderMessage());

    messageDispatcher.send(m3);
    assertEquals(m1, listener.getLastStartGrinderMessage());

    messageDispatcher.send(m2);
    assertEquals(m2, listener.getLastStartGrinderMessage());
  }
}

