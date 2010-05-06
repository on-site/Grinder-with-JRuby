// Copyright (C) 2004 Philip Aston
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

import java.io.File;

import net.grinder.common.FilenameFactory;


/**
 * Unit test case for <code>FilenameFactoryImplementation</code>.
 *
 * @author Philip Aston
 * @version $Revision: 3762 $
 */
public class TestFilenameFactoryImplementation extends TestCase {
  public TestFilenameFactoryImplementation(String name) {
    super(name);
  }

  public void testCreateFilename() throws Exception {
    final File directory = new File("a:\\directory");
    final String grinderID = "Grinder ID";
    final String prefix = "prefix";
    final String suffix = "suffix";

    final FilenameFactory filenameFactory =
      new FilenameFactoryImplementation(directory, grinderID);

    final String s = filenameFactory.createFilename(prefix, suffix);
    
    assertEquals(0, s.indexOf(directory.getPath()));

    final int prefixIndex = s.indexOf(prefix);
    final int grinderIDIndex = s.indexOf(grinderID);
    final int suffixIndex = s.indexOf(suffix);

    assertTrue(prefixIndex > 0);
    assertTrue(grinderIDIndex > prefixIndex);
    assertTrue(suffixIndex > grinderIDIndex);

    final String anotherPrefix = "x";

    final String s2 = filenameFactory.createFilename(anotherPrefix, ".log");
    final String s3 = filenameFactory.createFilename(anotherPrefix);
    assertEquals(s2, s3);
  }

  public void testCreateSubContextFilenameFactory() {

    final File directory = new File("//another/directory");
    final String grinderID = "xxx1231212";
    final String prefix = "foo_bar";
    final String suffix = "BLAH";
    final String subContext = "my-sub-context";

    final FilenameFactoryImplementation filenameFactory =
      new FilenameFactoryImplementation(directory, grinderID);

    final FilenameFactory subContextFilenameFactory =
      filenameFactory.createSubContextFilenameFactory(subContext);

    final String s = subContextFilenameFactory.createFilename(prefix, suffix);

    assertEquals(0, s.indexOf(directory.getPath()));

    final int prefixIndex = s.indexOf(prefix);
    final int grinderIDIndex = s.indexOf(grinderID);
    final int subContextIndex = s.indexOf(subContext);
    final int suffixIndex = s.indexOf(suffix);

    assertTrue(prefixIndex > 0);
    assertTrue(grinderIDIndex > prefixIndex);
    assertTrue(subContextIndex > grinderIDIndex);
    assertTrue(suffixIndex > subContextIndex);
  }
}
