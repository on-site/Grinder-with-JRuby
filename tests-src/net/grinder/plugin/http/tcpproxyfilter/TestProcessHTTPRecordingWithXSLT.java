// Copyright (C) 2005, 2006 Philip Aston
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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;

import net.grinder.common.LoggerStubFactory;
import net.grinder.plugin.http.tcpproxyfilter.ProcessHTTPRecordingWithXSLT.StyleSheetInputStream;
import net.grinder.plugin.http.xml.HTTPRecordingType;
import net.grinder.plugin.http.xml.HttpRecordingDocument;
import net.grinder.testutility.AbstractFileTestCase;
import net.grinder.testutility.AssertUtilities;
import net.grinder.testutility.RedirectStandardStreams;
import net.grinder.util.StreamCopier;


public class TestProcessHTTPRecordingWithXSLT extends AbstractFileTestCase {

  private final LoggerStubFactory m_loggerStubFactory =
    new LoggerStubFactory();

  public void testWithIdentityTransform() throws Exception {

    final StreamCopier streamCopier = new StreamCopier(4096, true);

    final InputStream identityStyleSheetStream =
      getClass().getResourceAsStream("resources/identity.xsl");

    final File identityStyleSheetFile =
      new File(getDirectory(), "identity.xsl");

    streamCopier.copy(identityStyleSheetStream,
                      new FileOutputStream(identityStyleSheetFile));

    final StyleSheetInputStream styleSheetInputStream =
      new StyleSheetInputStream(identityStyleSheetFile);

    final ProcessHTTPRecordingWithXSLT processor =
      new ProcessHTTPRecordingWithXSLT(
        styleSheetInputStream, m_loggerStubFactory.getLogger());

    final HttpRecordingDocument emptyDocument =
      HttpRecordingDocument.Factory.newInstance();

    processor.process(emptyDocument);

    final String output =
      m_loggerStubFactory.getOutputLogWriter().getOutputAndReset();
    AssertUtilities.assertContainsPattern(output,
      "^<\\?xml version=.*\\?>\\s*$");

    m_loggerStubFactory.assertSuccess("getOutputLogWriter");
    m_loggerStubFactory.assertNoMoreCalls();

    try {
      styleSheetInputStream.getInputStream().read();
      fail("Input stream not closed");
    }
    catch (IOException e) {
    }

    final ProcessHTTPRecordingWithXSLT processor2 =
      new ProcessHTTPRecordingWithXSLT(
        new StyleSheetInputStream(identityStyleSheetFile),
        m_loggerStubFactory.getLogger());

    final HttpRecordingDocument document2 =
      HttpRecordingDocument.Factory.newInstance();
    final HTTPRecordingType recording = document2.addNewHttpRecording();
    recording.addNewMetadata().setVersion("blah");

    processor2.process(document2);

    final String output2 =
      m_loggerStubFactory.getOutputLogWriter().getOutputAndReset();
    AssertUtilities.assertContainsPattern(output2,
      "^<\\?xml version=.*\\?>\\s*" +
      "<http-recording .*?>\\s*" +
      "<metadata>\\s*" +
      "<version>blah</version>\\s*" +
      "</metadata>\\s*" +
      "</http-recording>\\s*$");

    m_loggerStubFactory.assertSuccess("getOutputLogWriter");
    m_loggerStubFactory.assertNoMoreCalls();
  }

  public void testWithStandardTransform() throws Exception {
    final ProcessHTTPRecordingWithXSLT processor =
      new ProcessHTTPRecordingWithXSLT(m_loggerStubFactory.getLogger());

    final HttpRecordingDocument document =
      HttpRecordingDocument.Factory.newInstance();
    final HTTPRecordingType recording = document.addNewHttpRecording();
    recording.addNewMetadata().setVersion("blah");

    // Will fail with an un-parseable date TransformerException
    processor.process(document);

    final String output2 =
      m_loggerStubFactory.getOutputLogWriter().getOutputAndReset();
    AssertUtilities.assertContains(output2, "# blah");

    m_loggerStubFactory.assertSuccess("getOutputLogWriter");
    AssertUtilities.assertContains(
      (String)
      m_loggerStubFactory.assertSuccess("error", String.class).getParameters()[0],
      "Unparseable date");

    m_loggerStubFactory.assertNoMoreCalls();

    // This time it will work.
    recording.addNewMetadata().setTime(Calendar.getInstance());

    final ProcessHTTPRecordingWithXSLT processor2 =
      new ProcessHTTPRecordingWithXSLT(m_loggerStubFactory.getLogger());

    processor2.process(document);
    m_loggerStubFactory.assertSuccess("getOutputLogWriter");
    m_loggerStubFactory.assertNoMoreCalls();
  }

  public void testWithBadTransform() throws Exception {
    final File badStyleSheetFile = new File(getDirectory(), "bad.xsl");
    badStyleSheetFile.createNewFile();

    final ProcessHTTPRecordingWithXSLT processor =
      new ProcessHTTPRecordingWithXSLT(
        new StyleSheetInputStream(badStyleSheetFile),
        m_loggerStubFactory.getLogger());

    final HttpRecordingDocument emptyDocument =
      HttpRecordingDocument.Factory.newInstance();

    // Redirect streams, because XSLTC still chucks some stuff out to stderr.
    new RedirectStandardStreams() {
      protected void runWithRedirectedStreams() throws Exception {
        processor.process(emptyDocument);
    }}.run();

    m_loggerStubFactory.assertSuccess("error", String.class);

    // Processor might log multiple messages; ignore.
    // m_loggerStubFactory.assertNoMoreCalls();
  }
}
