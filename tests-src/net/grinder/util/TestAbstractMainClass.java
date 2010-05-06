// Copyright (C) 2008 Philip Aston
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

import net.grinder.common.GrinderException;
import net.grinder.common.Logger;
import net.grinder.common.LoggerStubFactory;
import net.grinder.testutility.AssertUtilities;
import net.grinder.testutility.CallData;
import net.grinder.util.AbstractMainClass.LoggedInitialisationException;
import net.grinder.util.JVM.VersionException;
import junit.framework.TestCase;


/**
 * Unit tests for {@link AbstractMainClass}.
 *
 * @author Philip Aston
 * @version $Revision: 3995 $
 */
public class TestAbstractMainClass extends TestCase {

  public void testAbstractMainClass() throws Exception {

    final LoggerStubFactory loggerStubFactory = new LoggerStubFactory();
    final Logger logger = loggerStubFactory.getLogger();
    final String myUsage = "do some stuff";

    final MyMainClass mainClass = new MyMainClass(logger, myUsage);

    assertSame(logger, mainClass.getLogger());

    loggerStubFactory.assertNoMoreCalls();

    final String javaVersion = System.getProperty("java.version");

    try {
      try {
        System.setProperty("java.version", "whatever");
        new MyMainClass(logger, myUsage);
        fail("Expected VersionException");
      }
      catch (VersionException e) {
      }

      loggerStubFactory.assertNoMoreCalls();

      try {
        System.setProperty("java.version", "1.3");
        new MyMainClass(logger, myUsage);
        fail("Expected LoggedInitialisationException");
      }
      catch (LoggedInitialisationException e) {
        AssertUtilities.assertContains(e.getMessage(), "Unsupported");
        loggerStubFactory.assertSuccess("error", String.class);
      }
    }
    finally {
      System.setProperty("java.version", javaVersion);
    }

    loggerStubFactory.assertNoMoreCalls();

    final LoggedInitialisationException barfError = mainClass.barfError("foo");
    assertEquals("foo", barfError.getMessage());
    final CallData errorCall =
      loggerStubFactory.assertSuccess("error", String.class);
    AssertUtilities.assertContains(
      errorCall.getParameters()[0].toString(), "foo");
    loggerStubFactory.assertNoMoreCalls();

    final LoggedInitialisationException barfUsage = mainClass.barfUsage();
    AssertUtilities.assertContains(barfUsage.getMessage(), myUsage);
    final CallData errorCall2 =
      loggerStubFactory.assertSuccess("error", String.class);
    AssertUtilities.assertContains(
      errorCall2.getParameters()[0].toString(), myUsage);
    loggerStubFactory.assertNoMoreCalls();
  }

  private static class MyMainClass extends AbstractMainClass {
    public MyMainClass(Logger logger, String usage) throws GrinderException {
      super(logger, usage);
    }
  }
}
