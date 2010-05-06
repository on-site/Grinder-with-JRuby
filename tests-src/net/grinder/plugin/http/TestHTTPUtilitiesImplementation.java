// Copyright (C) 2006 - 2010 Philip Aston
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

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Arrays.asList;

import net.grinder.common.GrinderException;
import net.grinder.common.SSLContextFactory;
import net.grinder.plugininterface.GrinderPlugin;
import net.grinder.plugininterface.PluginProcessContext;
import net.grinder.plugininterface.PluginRegistry;
import net.grinder.plugininterface.PluginThreadContext;
import net.grinder.script.Statistics;
import net.grinder.script.Grinder.ScriptContext;
import net.grinder.statistics.StatisticsServicesImplementation;
import net.grinder.testutility.RandomStubFactory;
import net.grinder.util.StandardTimeAuthority;
import net.grinder.util.TimeAuthority;
import HTTPClient.HTTPResponse;
import HTTPClient.NVPair;
import junit.framework.TestCase;


/**
 * Unit test case for <code>HTTPUtilitiesImplementation</code>.
 *
 * @author Philip Aston
 * @version $Revision: 4223 $
 */
public class TestHTTPUtilitiesImplementation extends TestCase {

  private final RandomStubFactory<PluginProcessContext>
    m_pluginProcessContextStubFactory =
      RandomStubFactory.create(PluginProcessContext.class);
  private final PluginProcessContext m_pluginProcessContext =
    m_pluginProcessContextStubFactory.getStub();

  private final RandomStubFactory<ScriptContext> m_scriptContextStubFactory =
    RandomStubFactory.create(ScriptContext.class);

  private final RandomStubFactory<Statistics> m_statisticsStubFactory =
    RandomStubFactory.create(Statistics.class);

  protected void setUp() throws Exception {
    final PluginThreadContext threadContext =
      RandomStubFactory.create(PluginThreadContext.class).getStub();

    final SSLContextFactory sslContextFactory =
      RandomStubFactory.create(SSLContextFactory.class).getStub();

    final TimeAuthority timeAuthority = new StandardTimeAuthority();

    final HTTPPluginThreadState threadState =
      new HTTPPluginThreadState(threadContext,
                                sslContextFactory,
                                null,
                                timeAuthority);

    m_statisticsStubFactory.setResult("availableForUpdate", Boolean.FALSE);
    final Statistics statistics =
      m_statisticsStubFactory.getStub();

    m_scriptContextStubFactory.setResult("getStatistics", statistics);
    final ScriptContext scriptContext =
      m_scriptContextStubFactory.getStub();

    m_pluginProcessContextStubFactory.setResult("getPluginThreadListener",
                                                threadState);
    m_pluginProcessContextStubFactory.setResult("getScriptContext",
                                                scriptContext);
    m_pluginProcessContextStubFactory.setResult("getStatisticsServices",
      StatisticsServicesImplementation.getInstance());

    m_pluginProcessContextStubFactory.setResult("getTimeAuthority",
      timeAuthority);

    new PluginRegistry() {
      {
        setInstance(this);
      }

      public void register(GrinderPlugin plugin) throws GrinderException {
        plugin.initialize(m_pluginProcessContext);
      }
    };

    HTTPPlugin.getPlugin().initialize(m_pluginProcessContext);
  }

  public void testBasicAuthorizationHeader() throws Exception {
    final HTTPUtilities httpUtilities =
      new HTTPUtilitiesImplementation(m_pluginProcessContext);

    final NVPair pair =
      httpUtilities.basicAuthorizationHeader("foo", "secret");
    assertEquals("Authorization", pair.getName());
    assertEquals("Basic Zm9vOnNlY3JldA==", pair.getValue());

    final NVPair pair2 =
      httpUtilities.basicAuthorizationHeader("", "");
    assertEquals("Authorization", pair2.getName());
    assertEquals("Basic Og==", pair2.getValue());
  }

  public void testGetLastResponse() throws Exception {
    final HTTPUtilities httpUtilities =
      new HTTPUtilitiesImplementation(m_pluginProcessContext);

    assertEquals(null, httpUtilities.getLastResponse());

    // HTTPClient isn't hot on interfaces, so we can't stub these.
    final HTTPRequestHandler handler = new HTTPRequestHandler();
    handler.start();
    final HTTPRequest request = new HTTPRequest();
    final HTTPResponse httpResponse = request.GET(handler.getURL());

    assertSame(httpResponse, httpUtilities.getLastResponse());

    handler.shutdown();
  }

  public void testValueFromLocationHeader() throws Exception {
    final HTTPRequest request = new HTTPRequest();

    final HTTPUtilities httpUtilities =
      new HTTPUtilitiesImplementation(m_pluginProcessContext);
    assertEquals("", httpUtilities.valueFromLocationURI("foo"));

    final HTTPRequestHandler handler = new HTTPRequestHandler();
    handler.start();
    request.GET(handler.getURL());
    assertEquals("", httpUtilities.valueFromLocationURI("foo"));

    handler.addHeader("Location", "http://www.w3.org/pub/WWW/People.html");
    request.GET(handler.getURL());
    assertEquals("", httpUtilities.valueFromLocationURI("foo"));

    handler.clearHeaders();
    handler.addHeader(
      "Location", "http://www.w3.org/pub/WWW/People.html?foo=bah&lah=dah");
    request.GET(handler.getURL());
    assertEquals("bah", httpUtilities.valueFromLocationURI("foo"));
    assertEquals("", httpUtilities.valueFromLocationURI("bah"));

    handler.clearHeaders();
    handler.addHeader(
      "Location", "http://www.w3.org/pub/WWW/People.html;foo=?foo=bah&lah=dah");
    request.GET(handler.getURL());
    assertEquals("", httpUtilities.valueFromLocationURI("foo"));
    assertEquals("dah", httpUtilities.valueFromLocationURI("lah"));

    handler.clearHeaders();
    handler.addHeader(
      "Location", "http://www.w3.org/pub/WWW/People.html;JSESSIONID=1234");
    request.GET(handler.getURL());
    assertEquals("1234", httpUtilities.valueFromLocationURI("JSESSIONID"));
    assertEquals("", httpUtilities.valueFromLocationURI("foo"));

    handler.shutdown();
  }

  public void testValueFromBodyURI() throws Exception {
    final HTTPRequest request = new HTTPRequest();

    final HTTPUtilities httpUtilities =
      new HTTPUtilitiesImplementation(m_pluginProcessContext);
    assertEquals("", httpUtilities.valueFromBodyURI("foo"));

    final HTTPRequestHandler handler = new HTTPRequestHandler();
    handler.start();
    request.GET(handler.getURL());
    assertEquals("", httpUtilities.valueFromBodyURI("foo"));

    handler.setBody(
      "<body><a href='http://www.w3.org/pub/WWW/People.html'>foo</a></body>");
    request.GET(handler.getURL());
    assertEquals("", httpUtilities.valueFromBodyURI("foo"));

    handler.setBody(
      "<body><a href='http://www.w3.org/pub/WWW/People.html?foo=bah&lah=dah'>foo</a></body>");
    request.GET(handler.getURL());
    assertEquals("bah", httpUtilities.valueFromBodyURI("foo"));
    assertEquals("", httpUtilities.valueFromBodyURI("bah"));

    handler.setBody(
      "<body><a href='http://www.w3.org/pub/WWW/People.html;foo=?foo=bah&lah=dah'>foo</a></body>");
    request.GET(handler.getURL());
    assertEquals("", httpUtilities.valueFromBodyURI("foo"));
    assertEquals("dah", httpUtilities.valueFromBodyURI("lah"));

    handler.setBody(
    "<body><a href='http://www.w3.org/pub/WWW/People.html;JSESSIONID=1234'>foo</a>" +
    "<a href='http://www.w3.org/pub/WWW/People.html;JSESSIONID=5678'>foo</a></body>");
    request.GET(handler.getURL());
    assertEquals("1234", httpUtilities.valueFromBodyURI("JSESSIONID"));
    assertEquals("", httpUtilities.valueFromBodyURI("foo"));

    handler.addHeader("Content-type", "garbage");
    request.GET(handler.getURL());
    assertEquals("1234", httpUtilities.valueFromBodyURI("JSESSIONID"));
    assertEquals("", httpUtilities.valueFromBodyURI("foo"));
    assertEquals("1234", httpUtilities.valueFromBodyURI("JSESSIONID", "<body>"));
    assertEquals("5678", httpUtilities.valueFromBodyURI("JSESSIONID", "</a>"));
    assertEquals("", httpUtilities.valueFromBodyURI("JSESSIONID", "5"));
    assertEquals("", httpUtilities.valueFromBodyURI("JSESSIONID", "999"));

    handler.shutdown();
  }

  public void testValuesFromBodyURI() throws Exception {
    final HTTPRequest request = new HTTPRequest();

    final HTTPUtilities httpUtilities =
      new HTTPUtilitiesImplementation(m_pluginProcessContext);
    assertEquals(emptyList(), httpUtilities.valuesFromBodyURI("foo"));

    final HTTPRequestHandler handler = new HTTPRequestHandler();
    handler.start();
    request.GET(handler.getURL());
    assertEquals(emptyList(), httpUtilities.valuesFromBodyURI("foo"));

    handler.setBody(
      "<body><a href='http://www.w3.org/pub/WWW/People.html'>foo</a></body>");
    request.GET(handler.getURL());
    assertEquals(emptyList(), httpUtilities.valuesFromBodyURI("foo"));

    handler.setBody(
      "<body><a href='http://www.w3.org/pub/WWW/People.html?foo=bah&lah=dah'>foo</a></body>");
    request.GET(handler.getURL());
    assertEquals(singletonList("bah"), httpUtilities.valuesFromBodyURI("foo"));
    assertEquals(emptyList(), httpUtilities.valuesFromBodyURI("bah"));

    handler.setBody(
      "<body><a href='http://www.w3.org/pub/WWW/People.html;foo=?foo=bah&lah=dah'>foo</a></body>");
    request.GET(handler.getURL());
    assertEquals(singletonList(""), httpUtilities.valuesFromBodyURI("foo"));
    assertEquals(singletonList("dah"), httpUtilities.valuesFromBodyURI("lah"));

    handler.setBody(
    "<body><a href='http://www.w3.org/pub/WWW/People.html;JSESSIONID=1234'>foo</a>" +
    "<a href='http://www.w3.org/pub/WWW/People.html;JSESSIONID=5678'>foo</a></body>");
    request.GET(handler.getURL());
    assertEquals(asList("1234", "5678"),
                 httpUtilities.valuesFromBodyURI("JSESSIONID"));
    assertEquals(emptyList(), httpUtilities.valuesFromBodyURI("foo"));

    handler.addHeader("Content-Type", "garbage");
    request.GET(handler.getURL());
    assertEquals(asList("1234", "5678"),
                 httpUtilities.valuesFromBodyURI("JSESSIONID"));
    assertEquals(emptyList(), httpUtilities.valuesFromBodyURI("foo"));
    assertEquals(asList("1234", "5678"),
                 httpUtilities.valuesFromBodyURI("JSESSIONID", "<body>"));
    assertEquals(asList("5678"),
                 httpUtilities.valuesFromBodyURI("JSESSIONID", "</a>"));
    assertEquals(emptyList(),
                 httpUtilities.valuesFromBodyURI("JSESSIONID", "5"));
    assertEquals(emptyList(),
                 httpUtilities.valuesFromBodyURI("JSESSIONID", "999"));

    handler.shutdown();
  }

  public void testValueFromBodyInput() throws Exception {
    final HTTPRequest request = new HTTPRequest();

    final HTTPUtilities httpUtilities =
      new HTTPUtilitiesImplementation(m_pluginProcessContext);
    assertEquals("", httpUtilities.valueFromBodyInput("foo"));

    final HTTPRequestHandler handler = new HTTPRequestHandler();
    handler.start();
    request.GET(handler.getURL());
    assertEquals("", httpUtilities.valueFromBodyInput("foo"));

    handler.setBody("<body><input name='foo'>foo</input></body>");
    request.GET(handler.getURL());
    assertEquals("", httpUtilities.valueFromBodyInput("foo"));

    // input tags should be empty. The content has no meaning
    handler.setBody("<body><input type='hidden' name='foo' value='bah'>foo</input>" +
                    "<input name='foo' value='blah'>foo</input></body>");
    request.GET(handler.getURL());
    assertEquals("bah", httpUtilities.valueFromBodyInput("foo"));
    assertEquals("", httpUtilities.valueFromBodyInput("bah"));
    assertEquals("bah", httpUtilities.valueFromBodyInput("foo", "<body>"));
    assertEquals("blah", httpUtilities.valueFromBodyInput("foo", "input"));
    assertEquals("", httpUtilities.valueFromBodyInput("foo", "not there"));

    handler.shutdown();
  }

  public void testValuesFromBodyInput() throws Exception {
    final HTTPRequest request = new HTTPRequest();

    final HTTPUtilities httpUtilities =
      new HTTPUtilitiesImplementation(m_pluginProcessContext);
    assertEquals(emptyList(), httpUtilities.valuesFromBodyInput("foo"));

    final HTTPRequestHandler handler = new HTTPRequestHandler();
    handler.start();
    request.GET(handler.getURL());
    assertEquals(emptyList(), httpUtilities.valuesFromBodyInput("foo"));

    handler.setBody("<body><input name='foo'>foo</input></body>");
    request.GET(handler.getURL());
    assertEquals(emptyList(), httpUtilities.valuesFromBodyInput("foo"));

    // input tags should be empty. The content has no meaning
    handler.setBody("<body><input name='foo' value='bah'>foo</input>" +
                    "<input type='hidden' name='foo' value='blah'>foo</input></body>");
    request.GET(handler.getURL());
    assertEquals(asList("bah", "blah"),
                 httpUtilities.valuesFromBodyInput("foo"));
    assertEquals(emptyList(), httpUtilities.valuesFromBodyInput("bah"));
    assertEquals(asList("blah"),
                 httpUtilities.valuesFromBodyInput("foo", "bah"));
    assertEquals(emptyList(),
                 httpUtilities.valuesFromBodyInput("foo", "blah"));
    assertEquals(emptyList(),
                 httpUtilities.valuesFromBodyInput("foo", "not there"));

    handler.shutdown();
  }

  public void testValueFromHiddenInput() throws Exception {
    final HTTPRequest request = new HTTPRequest();

    final HTTPUtilities httpUtilities =
      new HTTPUtilitiesImplementation(m_pluginProcessContext);
    assertEquals("", httpUtilities.valueFromHiddenInput("foo"));

    final HTTPRequestHandler handler = new HTTPRequestHandler();
    handler.start();
    request.GET(handler.getURL());
    assertEquals("", httpUtilities.valueFromHiddenInput("foo"));

    handler.setBody("<body><input type='hidden' name='foo'>foo</input></body>");
    request.GET(handler.getURL());
    assertEquals("", httpUtilities.valueFromHiddenInput("foo"));

    // input tags should be empty. The content has no meaning
    handler.setBody("<body><input name='foo' value='blah'>foo</input>" +
                    "<input type='hidden' name='foo' value='bah'>foo</input></body>");
    request.GET(handler.getURL());
    assertEquals("bah", httpUtilities.valueFromHiddenInput("foo"));
    assertEquals("", httpUtilities.valueFromHiddenInput("bah"));
    assertEquals("bah", httpUtilities.valueFromHiddenInput("foo", "<body>"));
    assertEquals("", httpUtilities.valueFromHiddenInput("foo", "input"));
    assertEquals("", httpUtilities.valueFromHiddenInput("foo", "not there"));

    handler.shutdown();
  }

  public void testValuesFromHiddenInput() throws Exception {
    final HTTPRequest request = new HTTPRequest();

    final HTTPUtilities httpUtilities =
      new HTTPUtilitiesImplementation(m_pluginProcessContext);
    assertEquals(emptyList(), httpUtilities.valuesFromHiddenInput("foo"));

    final HTTPRequestHandler handler = new HTTPRequestHandler();
    handler.start();
    request.GET(handler.getURL());
    assertEquals(emptyList(), httpUtilities.valuesFromHiddenInput("foo"));

    handler.setBody("<body><input type='hidden' name='foo'>foo</input></body>");
    request.GET(handler.getURL());
    assertEquals(emptyList(), httpUtilities.valuesFromHiddenInput("foo"));

    // input tags should be empty. The content has no meaning
    handler.setBody("<body><input type='hidden' name='foo' value='bah'>foo</input>" +
                    "<input type='hidden' name='foo' value='blah'>foo</input></body>");
    request.GET(handler.getURL());
    assertEquals(asList("bah", "blah"),
                 httpUtilities.valuesFromHiddenInput("foo"));
    assertEquals(emptyList(), httpUtilities.valuesFromHiddenInput("bah"));
    assertEquals(asList("blah"),
                 httpUtilities.valuesFromHiddenInput("foo", "bah"));
    assertEquals(emptyList(),
                 httpUtilities.valuesFromHiddenInput("foo", "blah"));
    assertEquals(emptyList(),
                 httpUtilities.valuesFromHiddenInput("foo", "not there"));

    handler.shutdown();
  }
}
