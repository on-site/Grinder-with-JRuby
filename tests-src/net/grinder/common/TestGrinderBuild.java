// Copyright (C) 2004, 2005, 2006 Philip Aston
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

package net.grinder.common;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;

import net.grinder.util.IsolatingClassLoader;

import junit.framework.TestCase;


/**
 * Unit tests for {@link GrinderBuild}.
 *
 * @author Philip Aston
 * @version $Revision: 3762 $
 */
public class TestGrinderBuild extends TestCase {
  public void testGrinderBuildStrings() throws Exception {
    final String expectedVersion = System.getProperty("grinder.version");

    if (expectedVersion != null) {
      // Our build has told us what to expect.
      assertEquals(expectedVersion, GrinderBuild.getVersionString());
      assertEquals(System.getProperty("grinder.date"),
                   GrinderBuild.getDateString());
    }
    else {
      assertNotNull(GrinderBuild.getVersionString());
      assertNotNull(GrinderBuild.getDateString());
    }

    assertTrue(GrinderBuild.getName().indexOf("The Grinder") >= 0);
  }

  public void testGrinderBuildExceptions() throws Exception {
    String[] shared = {
        "java.*",
    };

    final ClassLoader classLoader =
      new IsolatingClassLoader(
        (URLClassLoader) getClass().getClassLoader(), shared, false) {

        public URL getResource(String name) {
          // Be evil.
          return null;
        }
    };

    try {
      Class.forName("net.grinder.common.GrinderBuild", true, classLoader);
      fail("Expected ExceptionInInitializerError");
    }
    catch (ExceptionInInitializerError e) {
      assertTrue(e.getCause() instanceof IOException);
    }
  }
}
