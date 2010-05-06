// Copyright (C) 2005 - 2009 Philip Aston
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

import java.net.URLClassLoader;

import junit.framework.TestCase;


public class TestIsolatingClassLoader extends TestCase {

  public void testWithBootLoaderClass() throws Exception {
    final IsolatingClassLoader clA =
      new IsolatingClassLoader((URLClassLoader)getClass().getClassLoader(),
                               new String[0],
                               true);

    assertSame(String.class, Class.forName("java.lang.String", false, clA));

    final IsolatingClassLoader clB =
      new IsolatingClassLoader(clA,
                               new String[0],
                               false);

    try {
      Class.forName("java.lang.String", false, clB);
      fail("Expected ClassNotFoundException");
    }
    catch (ClassNotFoundException e) {
    }

    final IsolatingClassLoader clC =
      new IsolatingClassLoader(clA,
                               new String[] { "java.lang.String" },
                               false);

    assertSame(String.class, Class.forName("java.lang.String", false, clC));

    final IsolatingClassLoader clD =
      new IsolatingClassLoader(clA,
                               new String[] { "foo.*", "java.lang.*" },
                               false);


    assertSame(String.class, Class.forName("java.lang.String", false, clD));
  }

  public void testWithLoadableClass() throws Exception {
    final IsolatingClassLoader clA =
      new IsolatingClassLoader((URLClassLoader)getClass().getClassLoader(),
                               new String[0],
                               true);

    final String name = "net.grinder.util.AnIsolatedClass";

    final Class<?> c = Class.forName(name, true, clA);

    // AFAICT, the SUN JVM/JDK never calls loadClass(.., true).
    // Do so by here to force coverage.
    assertSame(c, clA.loadClass(name, true));

    assertSame(c, Class.forName(name, false, clA));
  }
}
