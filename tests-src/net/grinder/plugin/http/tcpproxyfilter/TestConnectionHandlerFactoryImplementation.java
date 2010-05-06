// Copyright (C) 2005 - 2009 Philip Aston
// Copyright (C) 2007 Venelin Mitov
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

package net.grinder.plugin.http.tcpproxyfilter;

import junit.framework.TestCase;

import net.grinder.common.LoggerStubFactory;
import net.grinder.testutility.RandomStubFactory;
import net.grinder.tools.tcpproxy.CommentSource;
import net.grinder.tools.tcpproxy.CommentSourceImplementation;
import net.grinder.tools.tcpproxy.ConnectionDetails;
import net.grinder.tools.tcpproxy.EndPoint;
import net.grinder.util.AttributeStringParser;
import net.grinder.util.URIParser;


/**
 * Unit tests for {@link ConnectionHandlerFactoryImplementation}.
 *
 * @author Philip Aston
 * @version $Revision: 4006 $
 */
public class TestConnectionHandlerFactoryImplementation extends TestCase {

  private final RandomStubFactory<HTTPRecording> m_httpRecordingStubFactory =
    RandomStubFactory.create(HTTPRecording.class);
  private final HTTPRecording m_httpRecording =
    m_httpRecordingStubFactory.getStub();

  private final LoggerStubFactory m_loggerStubFactory =
    new LoggerStubFactory();

  final RandomStubFactory<RegularExpressions>
    m_regularExpressionsStubFactory =
      RandomStubFactory.create(RegularExpressions.class);
  final RegularExpressions m_regularExpressions =
    m_regularExpressionsStubFactory.getStub();

  final RandomStubFactory<URIParser> m_uriParserStubFactory =
    RandomStubFactory.create(URIParser.class);
  final URIParser m_uriParser = m_uriParserStubFactory.getStub();

  final RandomStubFactory<AttributeStringParser>
    m_attributeStringParserStubFactory =
      RandomStubFactory.create(AttributeStringParser.class);
  final AttributeStringParser m_attributeStringParser =
    m_attributeStringParserStubFactory.getStub();

  final CommentSource m_commentSource = new CommentSourceImplementation();

  private final ConnectionDetails m_connectionDetails =
    new ConnectionDetails(
      new EndPoint("hostA", 80),
      new EndPoint("hostB", 80),
      false);

  public void testFactory() {
    final ConnectionHandlerFactory factory =
      new ConnectionHandlerFactoryImplementation(m_httpRecording,
        m_loggerStubFactory.getLogger(), m_regularExpressions, m_uriParser,
        m_attributeStringParser, null, m_commentSource);

    final ConnectionHandler handler1 = factory.create(m_connectionDetails);
    final ConnectionHandler handler2 = factory.create(m_connectionDetails);
    assertNotSame(handler1, handler2);

    m_httpRecordingStubFactory.assertNoMoreCalls();
    m_loggerStubFactory.assertNoMoreCalls();
    m_regularExpressionsStubFactory.assertNoMoreCalls();
    m_uriParserStubFactory.assertNoMoreCalls();
  }
}
