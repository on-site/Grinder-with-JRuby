// Copyright (C) 2000 Paco Gomez
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

package net.grinder.engine.process;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import net.grinder.common.FilenameFactory;
import net.grinder.common.SSLContextFactory;
import net.grinder.common.SkeletonThreadLifeCycleListener;
import net.grinder.common.Test;
import net.grinder.common.ThreadLifeCycleListener;
import net.grinder.engine.common.EngineException;
import net.grinder.engine.process.DispatchContext.DispatchStateException;
import net.grinder.plugininterface.PluginThreadContext;
import net.grinder.script.Statistics.StatisticsForTest;
import net.grinder.statistics.StatisticsSet;
import net.grinder.util.ListenerSupport;
import net.grinder.util.ListenerSupport.Informer;


/**
 * Package scope.
 *
 * @author Philip Aston
 * @version $Revision: 4186 $
 */
final class ThreadContextImplementation
  implements ThreadContext, PluginThreadContext {

  private final ListenerSupport<ThreadLifeCycleListener>
    m_threadLifeCycleListeners =
      new ListenerSupport<ThreadLifeCycleListener>();

  private final DispatchContextStack m_dispatchContextStack =
    new DispatchContextStack();

  private final ThreadLogger m_threadLogger;
  private final FilenameFactory m_filenameFactory;
  private final DispatchResultReporter m_dispatchResultReporter;

  private SSLContextFactory m_sslContextFactory;

  private boolean m_delayReports;

  private DispatchContext m_pendingDispatchContext;

  private StatisticsForTest m_statisticsForLastTest;

  private volatile boolean m_shutdown;
  private boolean m_shutdownReported;

  public ThreadContextImplementation(ProcessContext processContext,
                                     ThreadLogger threadLogger,
                                     FilenameFactory filenameFactory,
                                     PrintWriter dataWriter)
    throws EngineException {

    m_threadLogger = threadLogger;
    m_filenameFactory = filenameFactory;

    // Undocumented property. Added so Tom Barnes can investigate overhead
    // of data logging.
    if (processContext.getProperties().getBoolean("grinder.logData", true)) {
      final ThreadDataWriter threadDataWriter =
        new ThreadDataWriter(
          dataWriter,
          processContext.getStatisticsServices()
          .getDetailStatisticsView().getExpressionViews(),
          m_threadLogger.getThreadNumber());

      m_dispatchResultReporter = new DispatchResultReporter() {
        public void report(Test test,
                           long startTime,
                           StatisticsSet statistics) {
          threadDataWriter.report(
            getRunNumber(),
            test,
            startTime,
            statistics);
        }
      };
    }
    else {
      m_dispatchResultReporter = new DispatchResultReporter() {
        public void report(Test test,
                           long startTime,
                           StatisticsSet statistics) {
          // Null reporter.
        }
      };
    }

    registerThreadLifeCycleListener(
      new SkeletonThreadLifeCycleListener() {
        public void endRun() { reportPendingDispatchContext(); }
      });
  }

  public FilenameFactory getFilenameFactory() {
    return m_filenameFactory;
  }

  public int getThreadNumber() {
    return m_threadLogger.getThreadNumber();
  }

  public int getRunNumber() {
    return m_threadLogger.getCurrentRunNumber();
  }

  public ThreadLogger getThreadLogger() {
    return m_threadLogger;
  }

  public SSLContextFactory getThreadSSLContextFactory() {
    return m_sslContextFactory;
  }

  public void setThreadSSLContextFactory(SSLContextFactory sslContextFactory) {
    m_sslContextFactory = sslContextFactory;
  }

  public DispatchResultReporter getDispatchResultReporter() {
    return m_dispatchResultReporter;
  }

  public void registerThreadLifeCycleListener(
    ThreadLifeCycleListener listener) {
    m_threadLifeCycleListeners.add(listener);
  }

  public void removeThreadLifeCycleListener(ThreadLifeCycleListener listener) {
    m_threadLifeCycleListeners.remove(listener);
  }

  public void fireBeginThreadEvent() {
    m_threadLifeCycleListeners.apply(new Informer<ThreadLifeCycleListener>() {
      public void inform(ThreadLifeCycleListener l) { l.beginThread(); }
    });
  }

  public void fireBeginRunEvent() {
    m_threadLifeCycleListeners.apply(new Informer<ThreadLifeCycleListener>() {
      public void inform(ThreadLifeCycleListener l) { l.beginRun(); }
    });
  }

  public void fireEndRunEvent() {
    m_threadLifeCycleListeners.apply(new Informer<ThreadLifeCycleListener>() {
      public void inform(ThreadLifeCycleListener l) { l.endRun(); }
    });
  }

  public void fireBeginShutdownEvent() {
    m_threadLifeCycleListeners.apply(new Informer<ThreadLifeCycleListener>() {
      public void inform(ThreadLifeCycleListener l) { l.beginShutdown(); }
    });
  }

  public void fireEndThreadEvent() {
    m_threadLifeCycleListeners.apply(new Informer<ThreadLifeCycleListener>() {
      public void inform(ThreadLifeCycleListener l) { l.endThread(); }
    });

  }

  public void pushDispatchContext(DispatchContext dispatchContext)
    throws ShutdownException {

    if (m_shutdown) {
      // As soon as we're shutdown, we disable the instrumentation. This
      // avoids reporting of misleading test failures.

      // We only throw ShutdownException from pushDispatchContext. A test that
      // was in-flight at the time of shutdown will complete normally unless
      // the thread attempts to start a nested test.
      m_shutdownReported = true;
      throw new ShutdownException("Thread has been shut down");
    }

    reportPendingDispatchContext();

    getThreadLogger().setCurrentTestNumber(
      dispatchContext.getTest().getNumber());

    final DispatchContext existingContext = m_dispatchContextStack.peekTop();

    if (existingContext != null) {
      existingContext.setHasNestedContexts();
    }

    m_dispatchContextStack.push(dispatchContext);
  }

  public void popDispatchContext() {
    if (m_shutdownReported) {
      return;
    }

    final DispatchContext dispatchContext = m_dispatchContextStack.pop();

    if (dispatchContext == null) {
      throw new AssertionError("DispatchContext stack unexpectedly empty");
    }

    final DispatchContext parentDispatchContext =
      m_dispatchContextStack.peekTop();

    if (parentDispatchContext != null) {
      parentDispatchContext.getPauseTimer().add(
        dispatchContext.getPauseTimer());
    }

    m_statisticsForLastTest = dispatchContext.getStatisticsForTest();

    // Flush any pending report created by an inner test.
    reportPendingDispatchContext();

    if (m_delayReports) {
      m_pendingDispatchContext = dispatchContext;
    }
    else {
      try {
        dispatchContext.report();
      }
      catch (DispatchStateException e) {
        throw new AssertionError(e);
      }
    }
  }

  public StatisticsForTest getStatisticsForCurrentTest() {
     final DispatchContext dispatchContext = m_dispatchContextStack.peekTop();

     if (dispatchContext == null) {
       return null;
     }

     return dispatchContext.getStatisticsForTest();
  }

  public StatisticsForTest getStatisticsForLastTest() {
    return m_statisticsForLastTest;
  }

  public void setDelayReports(boolean b) {
    if (!b) {
      reportPendingDispatchContext();
    }

    m_delayReports = b;
  }

  public void reportPendingDispatchContext() {
    if (m_pendingDispatchContext != null) {
      try {
        m_pendingDispatchContext.report();
      }
      catch (DispatchStateException e) {
        throw new AssertionError(e);
      }

      m_pendingDispatchContext = null;
    }
  }

  public void pauseClock() {
    final DispatchContext dispatchContext = m_dispatchContextStack.peekTop();

    if (dispatchContext != null) {
      dispatchContext.getPauseTimer().start();
    }
  }

  public void resumeClock() {
    final DispatchContext dispatchContext = m_dispatchContextStack.peekTop();

    if (dispatchContext != null) {
      dispatchContext.getPauseTimer().stop();
    }
  }

  private static final class DispatchContextStack {
    private final List<DispatchContext> m_stack =
      new ArrayList<DispatchContext>();

    public void push(DispatchContext dispatchContext) {
      m_stack.add(dispatchContext);
    }

    public DispatchContext pop() {
      final int size = m_stack.size();

      if (size == 0) {
        return null;
      }

      return m_stack.remove(size - 1);
    }

    public DispatchContext peekTop() {
      final int size = m_stack.size();

      if (size == 0) {
        return null;
      }

      return m_stack.get(size - 1);
    }
  }

  public void shutdown() {
    m_shutdown = true;
  }
}
