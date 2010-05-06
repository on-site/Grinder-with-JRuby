// Copyright (C) 2004, 2005 Philip Aston
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

package net.grinder.engine.agent;

import junit.framework.TestCase;

import java.io.File;
import java.util.Properties;

import net.grinder.common.GrinderProperties;


/**
 *  Unit tests for <code>WorkerProcessCommandLine</code>.
 *
 * @author Philip Aston
 * @version $Revision: 4033 $
 */
public class TestWorkerProcessCommandLine extends TestCase {

  public void testConstructorWithEmptyProperties() throws Exception {

    final WorkerProcessCommandLine workerProcessCommandLine =
      new WorkerProcessCommandLine(new GrinderProperties(),
                                   new Properties(),
                                   null,
                                   null);

    assertEquals(
      "java net.grinder.engine.process.WorkerProcessEntryPoint",
      workerProcessCommandLine.toString());
  }

  public void testConstructorWithProperties() throws Exception {

    final GrinderProperties grinderProperties = new GrinderProperties() {{
      setProperty("grinder.jvm.arguments", "-server -Xmx1024M");
      setProperty("grinder.jvm.classpath", "abc;def");
    }};

    final Properties overrideProperties = new Properties();

    final WorkerProcessCommandLine workerProcessCommandLine =
      new WorkerProcessCommandLine(grinderProperties,
                                   overrideProperties,
                                   null,
                                   grinderProperties.getProperty("grinder.jvm.arguments"));

    assertEquals("java -server '-Xmx1024M' -classpath 'abc;def' net.grinder.engine.process.WorkerProcessEntryPoint",
                 workerProcessCommandLine.toString());

    // Should work twice.
    assertEquals("java -server '-Xmx1024M' -classpath 'abc;def' net.grinder.engine.process.WorkerProcessEntryPoint",
                 workerProcessCommandLine.toString());
  }

  public void testConstructorWithAgent() throws Exception {

    final GrinderProperties grinderProperties = new GrinderProperties();

    final Properties overrideProperties = new Properties();

    final File agentFile = new File("foobah.jar");

    final WorkerProcessCommandLine workerProcessCommandLine =
      new WorkerProcessCommandLine(grinderProperties,
                                   overrideProperties,
                                   agentFile,
                                   grinderProperties.getProperty("grinder.jvm.arguments"));

    assertEquals("java '-javaagent:" + agentFile.getAbsolutePath() +
                 "' net.grinder.engine.process.WorkerProcessEntryPoint",
                 workerProcessCommandLine.toString());
  }

  public void testWithSystemProperties() throws Exception {
    final GrinderProperties grinderProperties = new GrinderProperties() {{
      setProperty("grinder.jvm.arguments", "-Xmx1024M");
      setProperty("grinder.jvm.classpath", "abc;def");
    }};

    final Properties systemProperties = new Properties() {{
      setProperty("grinder.processes", "99"); // Ignored
      setProperty("java.class.path", "jvd;vg;nc");
    }};

    final WorkerProcessCommandLine workerProcessCommandLine =
      new WorkerProcessCommandLine(grinderProperties,
                                   systemProperties,
                                   null,
                                   grinderProperties.getProperty("grinder.jvm.arguments"));

    String commandLine = workerProcessCommandLine.toString();

    final String expectedPrefix = "java '-Xmx1024M' ";

    assertTrue(commandLine.startsWith(expectedPrefix));

    commandLine = commandLine.substring(expectedPrefix.length());

    final String expectedSuffix =
      "-classpath 'abc;def" + File.pathSeparatorChar + "jvd;vg;nc' " +
      "net.grinder.engine.process.WorkerProcessEntryPoint";

    assertEquals(expectedSuffix, commandLine);
  }
}
