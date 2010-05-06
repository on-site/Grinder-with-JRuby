// Copyright (C) 2007 - 2010 Philip Aston
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

import HTTPClient.HTTPConnection;
import HTTPClient.HTTPResponse;
import HTTPClient.URI;
import net.grinder.common.SSLContextFactory;
import net.grinder.plugininterface.PluginThreadContext;
import net.grinder.testutility.DelegatingStubFactory;
import net.grinder.testutility.RandomStubFactory;
import net.grinder.util.InsecureSSLContextFactory;
import net.grinder.util.Sleeper;

import junit.framework.TestCase;


/**
 * Unit tests for {@link HTTPPluginThreadState}.
 *
 * @author Philip Aston
 * @version $Revision: 4222 $
 */
public class TestHTTPPluginThreadState extends TestCase {

  private final RandomStubFactory<PluginThreadContext>
    m_threadContextStubFactory =
      RandomStubFactory.create(PluginThreadContext.class);
  private final PluginThreadContext m_threadContext =
    m_threadContextStubFactory.getStub();

  private final DelegatingStubFactory<InsecureSSLContextFactory>
    m_sslContextFactoryStubFactory =
      DelegatingStubFactory.create(new InsecureSSLContextFactory());
  private final SSLContextFactory m_sslContextFactory =
    m_sslContextFactoryStubFactory.getStub();

  private final RandomStubFactory<Sleeper> m_sleeperStubFactory =
    RandomStubFactory.create(Sleeper.class);
  private final Sleeper m_sleeper = m_sleeperStubFactory.getStub();

  public void testHTTPPluginThreadState() throws Exception {
    final HTTPPluginThreadState pluginThreadState =
      new HTTPPluginThreadState(m_threadContext,
                                m_sslContextFactory,
                                m_sleeper,
                                null);

    assertSame(m_threadContext, pluginThreadState.getThreadContext());

    pluginThreadState.beginThread();

    pluginThreadState.beginRun();

    pluginThreadState.endRun();

    pluginThreadState.beginRun();

    final HTTPConnectionWrapper wrapper1 =
      pluginThreadState.getConnectionWrapper(new URI("http://blah.com"));

    assertNotNull(wrapper1);

    final HTTPConnectionWrapper wrapper2 =
      pluginThreadState.getConnectionWrapper(new URI("https://secure.com"));

    m_sslContextFactoryStubFactory.assertSuccess("getSSLContext");

    assertNotNull(wrapper2);

    final HTTPConnectionWrapper wrapper3 =
      pluginThreadState.getConnectionWrapper(new URI("http://blah.com/lah"));

    assertSame(wrapper1, wrapper3);
    assertNotSame(wrapper2, wrapper3);

    pluginThreadState.endRun();

    pluginThreadState.beginRun();

    final HTTPConnectionWrapper wrapper4 =
      pluginThreadState.getConnectionWrapper(new URI("http://blah.com/lah"));

    assertNotSame(wrapper1, wrapper4);

    pluginThreadState.endRun();

    pluginThreadState.endThread();

    pluginThreadState.beginShutdown();

    m_sslContextFactoryStubFactory.assertNoMoreCalls();
  }

  public void testSetResponse() throws Exception {
    final HTTPRequestHandler handler = new HTTPRequestHandler();
    handler.start();

    try {
      final HTTPConnection connection =
        new HTTPConnection(new URI(handler.getURL()));

      final HTTPPluginThreadState pluginThreadState =
        new HTTPPluginThreadState(m_threadContext,
                                  m_sslContextFactory,
                                  m_sleeper,
                                  null);

      final HTTPResponse response = connection.Get("foo");

      pluginThreadState.setLastResponse(response);

      assertSame(response, pluginThreadState.getLastResponse());
    }
    finally {
      handler.shutdown();
    }
  }
}
