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

package net.grinder.util;

import junit.framework.TestCase;

import net.grinder.common.Logger;

import net.grinder.testutility.RandomStubFactory;


/**
 * Unit test case for {@link JVM}.
 *
 * @author Philip Aston
 * @version $Revision: 4006 $
 */
public class TestJVM extends TestCase {

  public void testIsAtLeastVersion() throws Exception {
    final JVM jvm = JVM.getInstance();

    assertTrue(jvm.isAtLeastVersion(1, 1));
    assertTrue(jvm.isAtLeastVersion(1, 2));
    assertTrue(jvm.isAtLeastVersion(1, 3));

    assertFalse(jvm.isAtLeastVersion(3, 0));
    assertFalse(jvm.isAtLeastVersion(1, 9));

    final String[] badVersions = {
      "not parseable",
      "123123",
      "",
    };

    final String oldVersion = System.getProperty("java.version");

    try {
      for (int i = 0; i < badVersions.length; ++i) {
        System.setProperty("java.version", badVersions[i]);

        try {
          jvm.isAtLeastVersion(1, 3);
          fail("Expected JVM.VersionException");
        }
        catch (JVM.VersionException e) {
        }
      }
    }
    finally {
      System.setProperty("java.version", oldVersion);
    }
  }

  public void testHaveRequisites() throws Exception {
    final RandomStubFactory<Logger> loggerFactory =
      RandomStubFactory.create(Logger.class);
    final JVM jvm = JVM.getInstance();

    assertTrue(jvm.haveRequisites(loggerFactory.getStub()));
    loggerFactory.assertNoMoreCalls();

    final String oldVersion = System.getProperty("java.version");

    try {
      System.setProperty("java.version", "1.2");

      assertFalse(jvm.haveRequisites(loggerFactory.getStub()));
      loggerFactory.assertSuccess("error", String.class);
      loggerFactory.assertNoMoreCalls();
    }
    finally {
      System.setProperty("java.version", oldVersion);
    }
  }

  public void testToString() throws Exception {
    final String result = JVM.getInstance().toString();

    assertTrue(result.indexOf(System.getProperty("java.vm.version")) > 0);
    assertTrue(result.indexOf(System.getProperty("os.version")) > 0);
  }
}
