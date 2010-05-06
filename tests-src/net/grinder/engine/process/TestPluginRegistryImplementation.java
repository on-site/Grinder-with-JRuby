// Copyright (C) 2004 - 2009 Philip Aston
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

import junit.framework.TestCase;

import net.grinder.common.Logger;
import net.grinder.common.ThreadLifeCycleListener;
import net.grinder.engine.common.EngineException;
import net.grinder.plugininterface.GrinderPlugin;
import net.grinder.plugininterface.PluginException;
import net.grinder.plugininterface.PluginRegistry;
import net.grinder.script.Grinder.ScriptContext;
import net.grinder.statistics.StatisticsServicesImplementation;
import net.grinder.testutility.CallData;
import net.grinder.testutility.RandomStubFactory;
import net.grinder.util.TimeAuthority;


/**
 * Unit test case for <code>PluginRegistry</code>.
 *
 * @author Philip Aston
 * @version $Revision: 4006 $
 */
public class TestPluginRegistryImplementation extends TestCase {

  private final RandomStubFactory<Logger> m_loggerStubFactory =
    RandomStubFactory.create(Logger.class);
  private final Logger m_logger = m_loggerStubFactory.getStub();

  private final RandomStubFactory<ScriptContext> m_scriptContextStubFactory =
    RandomStubFactory.create(ScriptContext.class);
  private final ScriptContext m_scriptContext =
    m_scriptContextStubFactory.getStub();

  private final RandomStubFactory<TimeAuthority> m_timeAuthorityStubFactory =
    RandomStubFactory.create(TimeAuthority.class);
  private final TimeAuthority m_timeAuthority =
    m_timeAuthorityStubFactory.getStub();

  private final ThreadContextLocator m_threadContextLocator =
    new StubThreadContextLocator();
  private final RandomStubFactory<GrinderPlugin> m_grinderPluginStubFactory =
    RandomStubFactory.create(GrinderPlugin.class);
  private final GrinderPlugin m_grinderPlugin =
    m_grinderPluginStubFactory.getStub();

  public void setUp() {
    m_grinderPluginStubFactory.setIgnoreObjectMethods();
  }

  public void testConstructorAndSingleton() throws Exception {
    final PluginRegistry pluginRegistry =
      new PluginRegistryImplementation(
        m_logger, m_scriptContext, m_threadContextLocator,
        StatisticsServicesImplementation.getInstance(), m_timeAuthority);

    assertSame(pluginRegistry, PluginRegistry.getInstance());
  }

  public void testRegister() throws Exception {
    final PluginRegistry pluginRegistry =
      new PluginRegistryImplementation(
        m_logger, m_scriptContext, m_threadContextLocator,
        StatisticsServicesImplementation.getInstance(), m_timeAuthority);

    pluginRegistry.register(m_grinderPlugin);

    final CallData callData =
      m_grinderPluginStubFactory.assertSuccess("initialize",
                                               RegisteredPlugin.class);

    final RegisteredPlugin registeredPlugin =
      (RegisteredPlugin)callData.getParameters()[0];
    assertSame(m_scriptContext, registeredPlugin.getScriptContext());
    assertSame(m_timeAuthority, registeredPlugin.getTimeAuthority());

    m_grinderPluginStubFactory.assertNoMoreCalls();

    m_loggerStubFactory.assertSuccess("output", String.class);
    m_loggerStubFactory.assertNoMoreCalls();

    pluginRegistry.register(m_grinderPlugin);

    m_grinderPluginStubFactory.assertNoMoreCalls();
    m_loggerStubFactory.assertNoMoreCalls();
  }

  public void testRegisterWithBadPlugin() throws Exception {
    final PluginRegistry pluginRegistry =
      new PluginRegistryImplementation(
        m_logger, m_scriptContext, m_threadContextLocator,
        StatisticsServicesImplementation.getInstance(), m_timeAuthority);

    final PluginException initialiseException =
      new PluginException("barf");
    m_grinderPluginStubFactory.setThrows("initialize", initialiseException);

    try {
      pluginRegistry.register(m_grinderPlugin);
      fail("Expected EngineException");
    }
    catch (EngineException e) {
      assertSame(initialiseException, e.getCause());
    }
  }

  public void testListeners() throws Exception {
    final PluginRegistryImplementation pluginRegistry =
      new PluginRegistryImplementation(
        m_logger, m_scriptContext, m_threadContextLocator,
        StatisticsServicesImplementation.getInstance(), m_timeAuthority);

    final RandomStubFactory<ThreadContext> threadContextStubFactory =
      RandomStubFactory.create(ThreadContext.class);

    pluginRegistry.threadCreated(threadContextStubFactory.getStub());

    final CallData callData =
      threadContextStubFactory.assertSuccess(
        "registerThreadLifeCycleListener",
        ThreadLifeCycleListener.class);

    final ThreadLifeCycleListener threadListener =
      (ThreadLifeCycleListener)callData.getParameters()[0];

    assertNotNull(threadListener);

    threadListener.beginThread();
    threadContextStubFactory.assertNoMoreCalls();
    threadListener.beginRun();
    threadListener.endRun();
    threadListener.endThread();

    pluginRegistry.register(m_grinderPlugin);
    m_grinderPluginStubFactory.assertSuccess(
      "initialize", RegisteredPlugin.class);

    threadListener.beginThread();
    m_grinderPluginStubFactory.assertSuccess(
      "createThreadListener", threadContextStubFactory.getStub());
    threadContextStubFactory.assertSuccess(
      "registerThreadLifeCycleListener", ThreadLifeCycleListener.class);

    threadListener.beginRun();
    threadListener.endRun();
    threadListener.endThread();
    threadListener.beginShutdown();

    threadContextStubFactory.assertNoMoreCalls();
  }
}
