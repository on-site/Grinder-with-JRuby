// Copyright (C) 2009 Philip Aston
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

package net.grinder;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.List;

import javax.xml.stream.XMLStreamException;

import junit.framework.TestCase;
import net.grinder.plugin.http.xml.CommentType;
import net.grinder.testutility.BlockingClassLoader;

import org.apache.xmlbeans.XmlBeans;
import org.objectweb.asm.ClassReader;
import org.picocontainer.PicoContainer;
import org.python.core.PyObject;

import extra166y.CustomConcurrentHashMap;

/**
 * Unit test that checks the manifest classpath is correct.
 *
 * @author Philip Aston
 * @version $Revision:$
 */
public class TestManifestClasspath extends TestCase {

  public void testManifest() throws Exception {
    final List<String> blockedClasses =
      Arrays.<String>asList("org.objectweb.asm.*",
                            "extra166y.*",
                            "net.grinder.*",
                            "org.python.*",
                            "javax.xml.stream.*",
                            "org.picocontainer.*",
                            "org.apache.xmlbeans.*");

    final URLClassLoader ourClassLoader =
      (URLClassLoader)TestManifestClasspath.class.getClassLoader();

    final BlockingClassLoader blockingClassLoader =
      new BlockingClassLoader(ourClassLoader, blockedClasses);

    final ClassLoader classLoader =
      new URLClassLoader(new URL[] { new URL("file:lib/grinder.jar") },
                         blockingClassLoader);

    final List<Class<?>> testClasses =
      Arrays.<Class<?>>asList(Grinder.class,
                              CustomConcurrentHashMap.class,
                              PyObject.class,
                              ClassReader.class,
                              CommentType.class,
                              XMLStreamException.class,
                              PicoContainer.class,
                              XmlBeans.class);

    for (Class<?> c : testClasses) {
      assertSame("Can load " + c.getName(),
                 classLoader,
                 classLoader.loadClass(c.getName()).getClassLoader());
    }
  }
}
