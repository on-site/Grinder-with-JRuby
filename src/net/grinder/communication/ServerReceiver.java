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

package net.grinder.communication;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.grinder.common.UncheckedInterruptedException;
import net.grinder.communication.ResourcePool.Reservation;
import net.grinder.util.thread.InterruptibleRunnable;
import net.grinder.util.thread.ThreadPool;
import net.grinder.util.thread.ThreadSafeQueue;
import net.grinder.util.thread.ThreadSafeQueue.ShutdownException;


/**
 * Manages the receipt of messages from many clients.
 *
 * @author Philip Aston
 * @version $Revision: 4003 $
 */
public final class ServerReceiver implements Receiver {

  private final MessageQueue m_messageQueue = new MessageQueue(true);
  private final List<ThreadPool> m_threadPools = new ArrayList<ThreadPool>();

  /**
   * Registers a new {@link Acceptor} from which the <code>ServerReceiver</code>
   * should process messages. Actively polls connections of the given types for
   * messages, deserialises them, and queues them for retrieval using
   * {@link #waitForMessage()}.
   *
   * <p>
   * A single <code>ServerReceiver</code> can listen to messages from multiple
   * {@link Acceptor}s. You can register the same {@link Acceptor} with
   * multiple <code>ServerReceiver</code>s, but then there is no way of
   * controlling which receiver will receive messages from a given
   * {@link Acceptor}.
   * </p>
   *
   * @param acceptor
   *          Acceptor.
   * @param connectionTypes
   *          Type of connections to listen for.
   * @param numberOfThreads
   *          How many threads to dedicate to processing the Acceptor. The
   *          threads this method spawns just read, deserialise, and queue. Set
   *          <code>numberOfThreads</code> to the number of concurrent streams
   *          you expect to be able to read.
   * @param idleThreadPollDelay
   *          Time in milliseconds that an idle thread should sleep if there are
   *          no sockets to process.
   *
   * @exception CommunicationException
   *              If this <code>ServerReceiver</code> has been shutdown.
   */
  public void receiveFrom(Acceptor acceptor,
                          ConnectionType[] connectionTypes,
                          int numberOfThreads,
                          final int idleThreadPollDelay)
    throws CommunicationException {

    if (connectionTypes.length == 0) {
      // Nothing to do.
      return;
    }

    final ResourcePool[] acceptedSocketSets =
      new ResourcePool[connectionTypes.length];

    for (int i = 0; i < connectionTypes.length; ++i) {
      acceptedSocketSets[i] = acceptor.getSocketSet(connectionTypes[i]);
    }

    final ThreadPool.InterruptibleRunnableFactory runnableFactory =
      new ThreadPool.InterruptibleRunnableFactory() {
        public InterruptibleRunnable create() {
          return new ServerReceiverRunnable(
            new CombinedResourcePool(acceptedSocketSets),
            idleThreadPollDelay);
        }
      };

    final ThreadPool threadPool =
      new ThreadPool("ServerReceiver (" + acceptor.getPort() + ", " +
                     Arrays.asList(connectionTypes) + ")",
                     numberOfThreads,
                     runnableFactory);

    synchronized (this) {
      try {
        m_messageQueue.checkIfShutdown();
      }
      catch (ShutdownException e) {
        throw new CommunicationException("Shut down", e);
      }

      m_threadPools.add(threadPool);
    }

    threadPool.start();
  }

  /**
   * Block until a message is available, or another thread has called
   * {@link #shutdown}. Typically called from a message dispatch loop.
   *
   * <p>Multiple threads can call this method, but only one thread
   * will receive a given message.</p>
   *
   * @return The message or <code>null</code> if shut down.
   * @throws CommunicationException If an error occurred receiving a message.
   */
  public Message waitForMessage() throws CommunicationException {

    try {
      return m_messageQueue.dequeue(true);
    }
    catch (ThreadSafeQueue.ShutdownException e) {
      return null;
    }
  }

  /**
   * Shut down this receiver.
   */
  public synchronized void shutdown() {

    m_messageQueue.shutdown();

    for (ThreadPool threadPool : m_threadPools) {
      threadPool.stop();
    }
  }

  /**
   * Return the number of active threads. Package scope; used by the unit tests.
   *
   * @return The number of active threads.
   */
  synchronized int getActveThreadCount() {
    int result = 0;

    for (ThreadPool threadPool : m_threadPools) {
      result += threadPool.getThreadGroup().activeCount();
    }

    return result;
  }

  /**
   * Obtain reservations from multiple ResourcePools. Delegates reserveNext()
   * to the next resource pool.
   *
   * @author Philip Aston
   * @version $Revision: 4003 $
   */
  private static final class CombinedResourcePool {
    private final ResourcePool[] m_resourcePools;

    // Guarded by m_resourcePools.
    private int m_next;

    CombinedResourcePool(ResourcePool[] resourcePools) {
      assert resourcePools.length > 0;
      m_resourcePools = resourcePools;
    }

    public Reservation reserveNext() {
      int i = 0;
      int start;

      synchronized (m_resourcePools) {
        start = ++m_next;
      }

      while (true) {
        final Reservation reservation =
          m_resourcePools[(start + i) % m_resourcePools.length].reserveNext();

        if (!reservation.isSentinel() ||
            i == m_resourcePools.length - 1) {
          return reservation;
        }

        ++i;
      }
    }
  }

  private final class ServerReceiverRunnable
    implements InterruptibleRunnable {

    private final CombinedResourcePool m_sockets;
    private final int m_delay;

    private ServerReceiverRunnable(CombinedResourcePool sockets, int delay) {
      m_sockets = sockets;
      m_delay = delay;
    }

    public void interruptibleRun() {
      try {
        // Did we do some work on the last pass?
        boolean idle = false;

        while (true) {
          final Reservation reservation = m_sockets.reserveNext();
          boolean holdReservation = false;

          try {
            if (reservation.isSentinel()) {
              if (idle) {
                Thread.sleep(m_delay);
              }

              idle = true;
            }
            else {
              final SocketWrapper socketWrapper =
                (SocketWrapper)reservation.getResource();

              // We don't need to synchronise access to the SocketWrapper
              // stream; access is protected through the socket set and only we
              // hold the reservation.
              final InputStream inputStream = socketWrapper.getInputStream();

              if (inputStream.available() > 0) {

                idle = false;

                final ObjectInputStream objectStream =
                  new ObjectInputStream(inputStream);

                final Message message = (Message)objectStream.readObject();

                if (message instanceof CloseCommunicationMessage) {
                  reservation.close();
                  continue;
                }

                if (message instanceof MessageRequiringResponse) {

                  final MessageRequiringResponse messageRequiringResponse =
                    (MessageRequiringResponse)message;

                  messageRequiringResponse.setResponder(
                    new SenderWithReservation(
                      new StreamSender(socketWrapper.getOutputStream()),
                      reservation));

                  m_messageQueue.queue(message);

                  // Whatever handles the MessageExpectingResponse takes
                  // responsibility for the reservation.
                  holdReservation = true;
                }
                else {
                  m_messageQueue.queue(message);
                }
              }
            }
          }
          catch (IOException e) {
            reservation.close();
            UncheckedInterruptedException.ioException(e);
            m_messageQueue.queue(e);
          }
          catch (ClassNotFoundException e) {
            reservation.close();
            m_messageQueue.queue(e);
          }
          catch (InterruptedException e) {
            reservation.close();
            throw new UncheckedInterruptedException(e);
          }
          finally {
            if (!holdReservation) {
              reservation.free();
            }
          }
        }
      }
      catch (ThreadSafeQueue.ShutdownException e) {
        // We've been shutdown, exit this thread.
      }
      finally {
        // Ensure we're shutdown.
        shutdown();
      }
    }
  }

  /**
   * SenderWithReservation. A one-shot use Sender, only accessed through a
   * {@link MessageRequiringResponse} wrapper which ensures send() is called at
   * most once.
   *
   * <p>
   * If no {@link MessageRequiringResponse#sendResponse(Message)} is not called,
   * the reservation will not be freed. No way to avoid this - we're handing off
   * responsibility to respond to a stream.
   * </p>
   *
   */
  private static final class SenderWithReservation implements Sender {
    private final Sender m_delegateSender;
    private final Reservation m_reservation;

    private SenderWithReservation(Sender delegateSender,
                                  Reservation reservation) {
      m_delegateSender = delegateSender;
      m_reservation = reservation;
    }

    public void send(Message message) throws CommunicationException {
      try {
        m_delegateSender.send(message);
      }
      finally {
        shutdown();
      }
    }

    public void shutdown() {
      // We rely on our ResponseSender's single call to send to ensure we don't
      // free the reservation multiple times.
      m_reservation.free();
    }
  }
}
