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

import java.io.File;
import java.io.IOException;

import HTTPClient.NVPair;

import net.grinder.common.LoggerStubFactory;
import net.grinder.plugin.http.xml.BasicAuthorizationHeaderType;
import net.grinder.plugin.http.xml.CommonHeadersType;
import net.grinder.plugin.http.xml.HTTPRecordingType;
import net.grinder.plugin.http.xml.HeaderType;
import net.grinder.plugin.http.xml.HeadersType;
import net.grinder.plugin.http.xml.HttpRecordingDocument;
import net.grinder.plugin.http.xml.PageType;
import net.grinder.plugin.http.xml.RequestType;
import net.grinder.plugin.http.xml.TokenReferenceType;
import net.grinder.plugin.http.xml.HTTPRecordingType.Metadata;
import net.grinder.testutility.AssertUtilities;
import net.grinder.testutility.RandomStubFactory;
import net.grinder.testutility.XMLBeansUtilities;
import net.grinder.tools.tcpproxy.ConnectionDetails;
import net.grinder.tools.tcpproxy.EndPoint;
import net.grinder.util.URIParser;
import net.grinder.util.URIParserImplementation;

import junit.framework.TestCase;


/**
 * Unit tests for {@link HTTPRecordingImplementation}.
 *
 * @author Philip Aston
 * @version $Revision: 4006 $
 */
public class TestHTTPRecordingImplementation extends TestCase {

  private final RandomStubFactory<HTTPRecordingResultProcessor>
    m_resultProcessorStubFactory =
      RandomStubFactory.create(HTTPRecordingResultProcessor.class);
  private final HTTPRecordingResultProcessor m_resultProcessor =
    m_resultProcessorStubFactory.getStub();

  private final RegularExpressions m_regularExpressions =
    new RegularExpressionsImplementation();

  private final URIParser m_uriParser = new URIParserImplementation();

  public void testConstructorAndDispose() throws Exception {
    final LoggerStubFactory loggerStubFactory = new LoggerStubFactory();

    final HTTPRecordingImplementation httpRecording =
      new HTTPRecordingImplementation(m_resultProcessor,
                                      loggerStubFactory.getLogger(),
                                      m_regularExpressions,
                                      m_uriParser);

    m_resultProcessorStubFactory.assertNoMoreCalls();

    httpRecording.dispose();

    final HttpRecordingDocument recording =
      (HttpRecordingDocument)
      m_resultProcessorStubFactory.assertSuccess("process",
      HttpRecordingDocument.class).getParameters()[0];

    XMLBeansUtilities.validate(recording);

    httpRecording.dispose();

    final HttpRecordingDocument recording2 =
      (HttpRecordingDocument)
      m_resultProcessorStubFactory.assertSuccess("process",
      HttpRecordingDocument.class).getParameters()[0];

    XMLBeansUtilities.validate(recording2);

    assertNotSame("We get a copy", recording, recording2);

    final Metadata metadata = recording.getHttpRecording().getMetadata();
    assertTrue(metadata.getVersion().length() > 0);
    assertNotNull(metadata.getTime());
    assertEquals(0, recording.getHttpRecording().getCommonHeadersArray().length);
    assertEquals(0, recording.getHttpRecording().getBaseUriArray().length);
    assertEquals(0, recording.getHttpRecording().getPageArray().length);
    m_resultProcessorStubFactory.assertNoMoreCalls();

    final IOException exception = new IOException("Eat me");
    m_resultProcessorStubFactory.setThrows("process", exception);

    httpRecording.dispose();

    m_resultProcessorStubFactory.assertException(
      "process",
      exception,
      HttpRecordingDocument.class);
    loggerStubFactory.assertSuccess("error", exception.getMessage());
    loggerStubFactory.assertSuccess("getErrorLogWriter");
    loggerStubFactory.assertNoMoreCalls();

    m_resultProcessorStubFactory.assertNoMoreCalls();
  }

  public void testAddRequest() throws Exception {
    final HTTPRecordingImplementation httpRecording =
      new HTTPRecordingImplementation(
        m_resultProcessor, null, m_regularExpressions, m_uriParser);

    final EndPoint endPoint1 = new EndPoint("hostA", 80);
    final EndPoint endPoint2 = new EndPoint("hostB", 80);
    final EndPoint endPoint3 = new EndPoint("hostC", 80);
    final String[] userComments = new String[]{
        "BEGIN ENTER gmail homepage",
        "END ENTER gmail homepage",
        "BEGIN CLICK Sign In",
        "END CLICK Sign In",
    };

    // Request 1
    final ConnectionDetails connectionDetails1 =
      new ConnectionDetails(endPoint1, endPoint2, false);

    final RequestType request1 =
      httpRecording.addRequest(connectionDetails1, "GET", "/");
    for (int i = 0; i < userComments.length; i++) {
      request1.addComment(userComments[i]);
    }
    assertEquals("/", request1.getUri().getUnparsed());
    assertEquals("GET", request1.getMethod().toString());
    assertEquals("GET /", request1.getDescription());
    AssertUtilities.assertArraysEqual(userComments, request1.getCommentArray());
    assertEquals("END CLICK Sign In", request1.getCommentArray(3));
    assertFalse(request1.isSetSleepTime());
    request1.addNewResponse();
    httpRecording.markLastResponseTime();

    // Request 2
    final ConnectionDetails connectionDetails2 =
      new ConnectionDetails(endPoint1, endPoint2, false);

    final RequestType request2 =
      httpRecording.addRequest(connectionDetails2, "GET", "/foo.gif");
    assertFalse(request2.isSetSleepTime());
    request2.addNewResponse();
    httpRecording.markLastResponseTime();
    Thread.sleep(20);

    // Request 3
    final ConnectionDetails connectionDetails3 =
      new ConnectionDetails(endPoint3, endPoint2, true);

    final RequestType request3 =
      httpRecording.addRequest(connectionDetails3, "GET", "bah.gif");
    assertEquals("bah.gif", request3.getUri().getUnparsed());
    assertTrue(request3.isSetSleepTime());
    request3.addNewResponse().setStatusCode(302);
    assertFalse(request3.isSetAnnotation());

    final RequestType request4 =
      httpRecording.addRequest(connectionDetails3, "GET", "bah.gif");
    request4.addNewResponse().setStatusCode(301);
    assertFalse(request4.isSetAnnotation());

    final RequestType request5 =
      httpRecording.addRequest(connectionDetails3, "GET", "bah.gif");
    request5.addNewResponse().setStatusCode(307);
    assertFalse(request5.isSetAnnotation());

    // Ignored because it doesn't have a response.
    httpRecording.addRequest(connectionDetails3, "GET", "bah.gif");

    httpRecording.dispose();

    final HttpRecordingDocument recording =
      (HttpRecordingDocument)
      m_resultProcessorStubFactory.assertSuccess("process",
      HttpRecordingDocument.class).getParameters()[0];

    XMLBeansUtilities.validate(recording);

    m_resultProcessorStubFactory.assertNoMoreCalls();

    final HTTPRecordingType result = recording.getHttpRecording();
    assertEquals(1, result.getCommonHeadersArray().length);
    assertEquals(0, result.getCommonHeadersArray(0).getHeaderArray().length);

    assertEquals(2, result.getBaseUriArray().length);
    assertEquals("hostb", result.getBaseUriArray(0).getHost());
    assertEquals("https", result.getBaseUriArray(1).getScheme().toString());

    assertEquals(2, result.getPageArray().length);

    final PageType page0 = result.getPageArray(0);
    assertEquals(2, page0.getRequestArray().length);
    assertEquals(result.getBaseUriArray(0).getUriId(),
                 page0.getRequestArray(1).getUri().getExtends());
    assertEquals("/foo.gif", page0.getRequestArray(1).getUri().getPath().getTextArray(0));
    assertFalse(page0.getRequestArray(1).isSetAnnotation());

    final PageType page1 = result.getPageArray(1);
    assertEquals(3, page1.getRequestArray().length);
    assertEquals(0, page1.getRequestArray(0).getHeaders().sizeOfHeaderArray());
    assertTrue(page1.getRequestArray(0).isSetAnnotation());
    assertTrue(page1.getRequestArray(1).isSetAnnotation());
    assertTrue(page1.getRequestArray(2).isSetAnnotation());
  }

  public void testAddRequestWithComplexPaths() throws Exception {
    final HTTPRecording httpRecording =
      new HTTPRecordingImplementation(
        m_resultProcessor, null, m_regularExpressions, m_uriParser);

    final EndPoint endPoint1 = new EndPoint("hostA", 80);
    final EndPoint endPoint2 = new EndPoint("hostB", 80);

    // Request 1
    final ConnectionDetails connectionDetails1 =
      new ConnectionDetails(endPoint1, endPoint2, false);

    final RequestType request1 =
      httpRecording.addRequest(
        connectionDetails1,
        "GET",
        "/path;name=value/blah;dah/foo?foo=y"
      );

    assertEquals("GET", request1.getMethod().toString());
    assertEquals("GET foo", request1.getDescription());
    assertEquals("/path;", request1.getUri().getPath().getTextArray(0));
    assertEquals("token_name", request1.getUri().getPath().getTokenReferenceArray(0).getTokenId());
    assertEquals("/blah;dah/foo", request1.getUri().getPath().getTextArray(1));
    assertEquals(0, request1.getUri().getQueryString().getTextArray().length);
    assertEquals("y", request1.getUri().getQueryString().getTokenReferenceArray(0).getNewValue());
    assertFalse(request1.getUri().isSetFragment());

    final RequestType request2 =
      httpRecording.addRequest(connectionDetails1, "POST", "/?x=y&fo--o=bah#lah?;blah");

    assertEquals("POST", request2.getMethod().toString());
    assertEquals("POST /", request2.getDescription());
    assertEquals("/", request2.getUri().getPath().getTextArray(0));
    assertEquals(0, request2.getUri().getPath().getTokenReferenceArray().length);
    AssertUtilities.assertArraysEqual(new String[] {"&"}, request2.getUri().getQueryString().getTextArray());
    assertEquals("token_foo2", request2.getUri().getQueryString().getTokenReferenceArray(1).getTokenId());
    assertEquals("bah", request2.getUri().getQueryString().getTokenReferenceArray(1).getNewValue());
    assertEquals("lah?;blah", request2.getUri().getFragment());

    final RequestType request3 =
      httpRecording.addRequest(connectionDetails1, "POST", "/?x=y&fo--o=bah#lah?;blah");

    AssertUtilities.assertArraysEqual(new String[] {"&"}, request3.getUri().getQueryString().getTextArray());
    assertEquals("token_foo2", request3.getUri().getQueryString().getTokenReferenceArray(1).getTokenId());
    assertFalse(request3.getUri().getQueryString().getTokenReferenceArray(1).isSetNewValue());
    assertEquals("lah?;blah", request3.getUri().getFragment());
  }

  public void testAddRequestWithHeaders() throws Exception {
    final HTTPRecordingImplementation httpRecording =
      new HTTPRecordingImplementation(
        m_resultProcessor, null, m_regularExpressions, m_uriParser);

    final EndPoint endPoint1 = new EndPoint("hostA", 80);
    final EndPoint endPoint2 = new EndPoint("hostB", 80);
    final ConnectionDetails connectionDetails1 =
      new ConnectionDetails(endPoint1, endPoint2, false);

    final RequestType request1 =
      httpRecording.addRequest(connectionDetails1, "GET", "/path");

    request1.setHeaders(createHeaders(new NVPair[] {
        new NVPair("foo", "bah"),
        new NVPair("User-Agent", "blah"),
        new NVPair("Accept", "x"),
    }));
    request1.addNewResponse();

    final RequestType request2 =
      httpRecording.addRequest(connectionDetails1, "GET", "/path");

    request2.setHeaders(createHeaders(new NVPair[] {
        new NVPair("fu", "bar"),
        new NVPair("User-Agent", "blah"),
        new NVPair("Accept", "y"),
    }));
    request2.addNewResponse();

    final RequestType request3 =
      httpRecording.addRequest(connectionDetails1, "GET", "/path");

    request3.setHeaders(createHeaders(new NVPair[] {
        new NVPair("fu", "bar"),
        new NVPair("User-Agent", "blah"),
        new NVPair("Accept", "y"),
    }));
    request3.addNewResponse();

    final RequestType request4 =
      httpRecording.addRequest(connectionDetails1, "GET", "/path");

    request4.setHeaders(createHeaders(new NVPair[] {
        new NVPair("User-Agent", "blah"),
        new NVPair("Accept", "z"),
    }));
    final BasicAuthorizationHeaderType basicAuthorizationHeaderType =
      request4.getHeaders().addNewAuthorization().addNewBasic();
    basicAuthorizationHeaderType.setUserid("phil");
    basicAuthorizationHeaderType.setPassword("abracaduh");
    request4.addNewResponse();

    httpRecording.dispose();

    final HTTPRecordingType recording =
      ((HttpRecordingDocument)
      m_resultProcessorStubFactory.assertSuccess("process",
      HttpRecordingDocument.class).getParameters()[0]).getHttpRecording();

    // Default, plus 3 sets.
    assertEquals(4, recording.getCommonHeadersArray().length);

    final CommonHeadersType defaultHeaders = recording.getCommonHeadersArray(0);
    assertEquals(0, defaultHeaders.getAuthorizationArray().length);
    assertEquals(1, defaultHeaders.getHeaderArray().length);
    assertEquals("User-Agent", defaultHeaders.getHeaderArray(0).getName());

    final CommonHeadersType commonHeaders1 = recording.getCommonHeadersArray(1);
    assertEquals(defaultHeaders.getHeadersId(), commonHeaders1.getExtends());
    assertEquals(1, commonHeaders1.getHeaderArray().length);
    assertEquals("x", commonHeaders1.getHeaderArray(0).getValue());
    assertEquals(0, commonHeaders1.getAuthorizationArray().length);
    assertEquals(
      commonHeaders1.getHeadersId(),
      recording.getPageArray(0).getRequestArray(0).getHeaders().getExtends());

    final CommonHeadersType commonHeaders2 = recording.getCommonHeadersArray(2);
    assertEquals(defaultHeaders.getHeadersId(), commonHeaders2.getExtends());
    assertEquals(1, commonHeaders2.getHeaderArray().length);
    assertEquals("y", commonHeaders2.getHeaderArray(0).getValue());
    assertEquals(0, commonHeaders2.getAuthorizationArray().length);

    final HeadersType headers = recording.getPageArray(3).getRequestArray(0).getHeaders();
    assertEquals(0, headers.getHeaderArray().length);
    assertEquals(1, headers.getAuthorizationArray().length);
    assertEquals("phil", headers.getAuthorizationArray(0).getBasic().getUserid());
  }

  private HeadersType createHeaders(NVPair[] nvPairs) {
    final HeadersType result = HeadersType.Factory.newInstance();

    for (int i = 0; i < nvPairs.length; ++i) {
      final HeaderType header = result.addNewHeader();
      header.setName(nvPairs[i].getName());
      header.setValue(nvPairs[i].getValue());
    }

    return result;
  }

  public void testCreateBodyDataFileName() throws Exception {
    final HTTPRecording httpRecording =
      new HTTPRecordingImplementation(
        m_resultProcessor, null, m_regularExpressions, m_uriParser);

    final File file1 = httpRecording.createBodyDataFileName();
    final File file2 = httpRecording.createBodyDataFileName();

    assertTrue(!file1.equals(file2));
  }

  public void testTokenReferenceMethods() throws Exception {
    final HTTPRecording httpRecording =
      new HTTPRecordingImplementation(
        m_resultProcessor, null, m_regularExpressions, m_uriParser);

    assertFalse(httpRecording.tokenReferenceExists("foo", null));
    assertFalse(httpRecording.tokenReferenceExists("foo", "somewhere"));
    assertNull(httpRecording.getLastValueForToken("foo"));

    final TokenReferenceType tokenReference = TokenReferenceType.Factory.newInstance();
    tokenReference.setSource("somewhere");
    httpRecording.setTokenReference("foo", "bah", tokenReference);

    assertFalse(httpRecording.tokenReferenceExists("foo", null));
    assertTrue(httpRecording.tokenReferenceExists("foo", "somewhere"));
    assertEquals("bah", httpRecording.getLastValueForToken("foo"));

    tokenReference.unsetSource();
    httpRecording.setTokenReference("foo", "bah", tokenReference);

    assertTrue(httpRecording.tokenReferenceExists("foo", null));
    assertTrue(httpRecording.tokenReferenceExists("foo", "somewhere"));
    assertEquals("bah", httpRecording.getLastValueForToken("foo"));

    httpRecording.setTokenReference("foo", "blah", tokenReference);
    assertEquals("blah", httpRecording.getLastValueForToken("foo"));
  }
}
