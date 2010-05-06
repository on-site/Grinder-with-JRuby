// Copyright (C) 2008 - 2009 Philip Aston
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

package net.grinder.plugin.http;

import java.net.URLClassLoader;

import junit.framework.TestCase;
import net.grinder.common.GrinderException;
import net.grinder.plugininterface.GrinderPlugin;
import net.grinder.plugininterface.PluginException;
import net.grinder.plugininterface.PluginProcessContext;
import net.grinder.plugininterface.PluginRegistry;
import net.grinder.plugininterface.PluginThreadContext;
import net.grinder.plugininterface.PluginThreadListener;
import net.grinder.script.Statistics;
import net.grinder.script.Grinder.ScriptContext;
import net.grinder.testutility.RandomStubFactory;
import net.grinder.util.IsolatingClassLoader;


/**
 * Unit tests for {@link HTTPPlugin}.
 *
 * @author Philip Aston
 * @version $Revision: 4006 $
 */
public class TestHTTPPlugin extends TestCase {

  private final RandomStubFactory<PluginProcessContext>
    m_pluginProcessContextStubFactory =
      RandomStubFactory.create(PluginProcessContext.class);
  private final PluginProcessContext m_pluginProcessContext =
    m_pluginProcessContextStubFactory.getStub();

  private final RandomStubFactory<ScriptContext> m_scriptContextStubFactory =
    RandomStubFactory.create(ScriptContext.class);
  private final ScriptContext m_scriptContext =
    m_scriptContextStubFactory.getStub();

  {
    m_pluginProcessContextStubFactory.setResult(
      "getScriptContext", m_scriptContext);
  }

  public void testInitialiseWithBadHTTPClient() throws Exception {

    final ClassLoader classLoader =
      new IsolatingClassLoader(
        (URLClassLoader) getClass().getClassLoader(), new String[] { }, false) {
      protected Class<?> loadClass(String name, boolean resolve)
        throws ClassNotFoundException  {

        if (name.equals("HTTPClient.RetryModule")) {
          return null;
        }

        if (name.equals("net.grinder.plugin.http.HTTPPlugin")) {
          // Isolate HTTPPlugin.
          return super.loadClass(name, resolve);
        }

        // Share everything else.
        return Class.forName(name, resolve, getParent());
      }
    };

    new PluginRegistry() {
      {
        setInstance(this);
      }

      public void register(GrinderPlugin plugin) throws GrinderException {
        plugin.initialize(m_pluginProcessContext);
      }
    };

    try {
      Class.forName("net.grinder.plugin.http.HTTPPlugin", true, classLoader);
      fail("Expected PluginException");
    }
    catch (ExceptionInInitializerError e) {
      // EIIE ->  PluginException -> ClassNotFoundException
      assertTrue(e.getCause().getCause() instanceof ClassNotFoundException);
    }
  }

  public void testInitializeWithBadStatistics() throws Exception {

    final RandomStubFactory<Statistics> statisticsStubFactory =
      RandomStubFactory.create(Statistics.class);
    final GrinderException grinderException = new GrinderException("Hello") {};

    statisticsStubFactory.setThrows("registerDataLogExpression",
                                    grinderException);

    m_scriptContextStubFactory.setResult("getStatistics",
                                         statisticsStubFactory.getStub());

    final HTTPPlugin plugin = new HTTPPlugin();

    try {
      plugin.initialize(m_pluginProcessContext);
      fail("Expected PluginException");
    }
    catch (PluginException e) {
      assertSame(grinderException, e.getCause());
    }
  }

  public void testCreateThreadListener() throws Exception {
    final HTTPPlugin plugin = new HTTPPlugin();

    plugin.initialize(m_pluginProcessContext);

    assertSame(m_pluginProcessContext, plugin.getPluginProcessContext());

    final RandomStubFactory<PluginThreadContext>
      pluginThreadContextStubFactory =
        RandomStubFactory.create(PluginThreadContext.class);
    final PluginThreadListener threadListener =
      plugin.createThreadListener(pluginThreadContextStubFactory.getStub());

    assertNotNull(threadListener);
  }
}
