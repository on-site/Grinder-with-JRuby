// Copyright (C) 2005 - 2010 Philip Aston
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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;

import net.grinder.common.GrinderProperties;
import net.grinder.common.Logger;
import net.grinder.common.LoggerStubFactory;
import net.grinder.communication.Acceptor;
import net.grinder.communication.CommunicationException;
import net.grinder.communication.ConnectionIdentity;
import net.grinder.communication.ConnectionType;
import net.grinder.communication.FanOutServerSender;
import net.grinder.communication.Message;
import net.grinder.communication.Sender;
import net.grinder.communication.ServerReceiver;
import net.grinder.communication.StreamReceiver;
import net.grinder.engine.agent.DebugThreadWorker.IsolateGrinderProcessRunner;
import net.grinder.messages.agent.ResetGrinderMessage;
import net.grinder.messages.agent.StartGrinderMessage;
import net.grinder.messages.agent.StopGrinderMessage;
import net.grinder.testutility.AbstractFileTestCase;


/**
 * Unit tests for <code>Agent</code>
 * TestAgent.
 *
 * @author Philip Aston
 * @version $Revision: 4231 $
 */
public class TestAgentImplementation extends AbstractFileTestCase {

  private final LoggerStubFactory m_loggerStubFactory = new LoggerStubFactory();
  private final Logger m_logger = m_loggerStubFactory.getLogger();
  private String m_originalClassPath;

  protected void setUp() throws Exception {
    DebugThreadWorkerFactory.setIsolatedRunnerClass(TestRunner.class);
    m_originalClassPath = System.getProperty("java.class.path");
    super.setUp();
  }

  protected void tearDown() throws Exception {
    super.tearDown();
    System.setProperty("java.class.path", m_originalClassPath);
    DebugThreadWorkerFactory.setIsolatedRunnerClass(null);
  }

  public void testConstruction() throws Exception {
    final File propertyFile = new File(getDirectory(), "properties");
    final Agent agent = new AgentImplementation(m_logger, propertyFile, true);
    agent.shutdown();

    m_loggerStubFactory.assertOutputMessageContains("finished");
    m_loggerStubFactory.assertNoMoreCalls();
  }

  public void testRunDefaultProperties() throws Exception {
    // Files in cwd.
    final File propertyFile = new File("grinder.properties");
    propertyFile.deleteOnExit();

    final File relativeScriptFile = new File("script/blah");
    relativeScriptFile.deleteOnExit();
    relativeScriptFile.getParentFile().mkdirs();
    relativeScriptFile.getParentFile().deleteOnExit();
    relativeScriptFile.createNewFile();

    try {
      final GrinderProperties properties = new GrinderProperties(propertyFile);

      final Agent agent = new AgentImplementation(m_logger, null, true);

      m_loggerStubFactory.assertNoMoreCalls();

      agent.run();

      m_loggerStubFactory.assertOutputMessageContains("The Grinder");
      m_loggerStubFactory.assertErrorMessageContains("Failed to connect");
      m_loggerStubFactory.assertErrorMessageContains("does not exist");
      m_loggerStubFactory.assertNoMoreCalls();

      properties.setBoolean("grinder.useConsole", false);
      properties.save();

      properties.setFile("grinder.script", relativeScriptFile);
      properties.setInt("grinder.processes", 0);
      properties.save();

      agent.run();

      m_loggerStubFactory.assertOutputMessageContains("The Grinder");
      m_loggerStubFactory.assertOutputMessageContains("command line");
      m_loggerStubFactory.assertNoMoreCalls();

      properties.setFile("grinder.logDirectory",
                         getDirectory().getAbsoluteFile());
      properties.save();

      agent.run();

      m_loggerStubFactory.assertOutputMessageContains("The Grinder");
      m_loggerStubFactory.assertOutputMessageContains("command line");
      m_loggerStubFactory.assertNoMoreCalls();

      agent.shutdown();
    }
    finally {
      assertTrue(propertyFile.delete());
      assertTrue(relativeScriptFile.delete());
      assertTrue(relativeScriptFile.getParentFile().delete());
    }
  }

  public void testRun() throws Exception {
    final File propertyFile = new File(getDirectory(), "properties");
    final GrinderProperties properties = new GrinderProperties(propertyFile);

    final Agent agent = new AgentImplementation(m_logger, propertyFile, true);

    m_loggerStubFactory.assertNoMoreCalls();

    agent.run();

    m_loggerStubFactory.assertOutputMessageContains("The Grinder");
    m_loggerStubFactory.assertErrorMessageContains("Failed to connect");
    m_loggerStubFactory.assertErrorMessageContains("does not exist");
    m_loggerStubFactory.assertNoMoreCalls();

    properties.setBoolean("grinder.useConsole", false);
    properties.save();

    agent.run();

    m_loggerStubFactory.assertOutputMessageContains("The Grinder");
    m_loggerStubFactory.assertErrorMessageContains("does not exist");
    m_loggerStubFactory.assertNoMoreCalls();

    final File scriptFile = new File(getDirectory(), "script");
    assertTrue(scriptFile.createNewFile());

    final File badFile = new File(scriptFile.getAbsoluteFile(), "blah");
    properties.setFile("grinder.script", badFile);
    properties.save();

    agent.run();

    m_loggerStubFactory.assertOutputMessageContains("The Grinder");
    m_loggerStubFactory.assertErrorMessageContains("does not exist");
    m_loggerStubFactory.assertNoMoreCalls();

    properties.setFile("grinder.script", scriptFile);
    properties.setInt("grinder.processes", 0);
    properties.save();

    agent.run();

    m_loggerStubFactory.assertOutputMessageContains("The Grinder");
    m_loggerStubFactory.assertOutputMessageContains("command line");
    m_loggerStubFactory.assertNoMoreCalls();

    properties.setBoolean("grinder.debug.singleprocess", true);
    properties.save();

    agent.run();

    m_loggerStubFactory.assertOutputMessageContains("The Grinder");
    m_loggerStubFactory.assertOutputMessageContains(
      "threads rather than processes");
    m_loggerStubFactory.assertNoMoreCalls();

    properties.setProperty("grinder.jvm.arguments", "-Dsome_stuff=blah");
    properties.save();

    agent.run();

    m_loggerStubFactory.assertOutputMessageContains("The Grinder");
    m_loggerStubFactory.assertOutputMessageContains(
    "threads rather than processes");
    m_loggerStubFactory.assertOutputMessageContains("grinder.jvm.arguments");
    m_loggerStubFactory.assertNoMoreCalls();

    agent.shutdown();

    m_loggerStubFactory.assertOutputMessageContains("finished");
    m_loggerStubFactory.assertNoMoreCalls();
  }

  public void testWithConsole() throws Exception {
    final ConsoleStub console = new ConsoleStub() {
      public void onConnect() throws Exception {
        // After we accept an agent connection...
        m_loggerStubFactory.assertOutputMessageContains("The Grinder");

        m_loggerStubFactory.waitUntilCalled(5000);
        m_loggerStubFactory.assertOutputMessageContains("connected");

        m_loggerStubFactory.waitUntilCalled(5000);
        m_loggerStubFactory.assertOutputMessageContains("waiting");

        // ...send a start message...
        final GrinderProperties grinderProperties = new GrinderProperties();
        getSender().send(new StartGrinderMessage(grinderProperties, 99));

        m_loggerStubFactory.waitUntilCalled(5000);
        m_loggerStubFactory.assertOutputMessage("received a start message");

        m_loggerStubFactory.waitUntilCalled(5000);
        m_loggerStubFactory.assertErrorMessageContains("grinder.py");

        m_loggerStubFactory.waitUntilCalled(5000);
        m_loggerStubFactory.assertOutputMessageContains("waiting");

        // ...send another start message...
        getSender().send(new StartGrinderMessage(grinderProperties, 99));

        m_loggerStubFactory.waitUntilCalled(5000);
        m_loggerStubFactory.assertOutputMessage("received a start message");

        m_loggerStubFactory.waitUntilCalled(5000);
        m_loggerStubFactory.assertOutputMessageContains("The Grinder");

        m_loggerStubFactory.waitUntilCalled(5000);
        m_loggerStubFactory.assertErrorMessageContains("grinder.py");

        m_loggerStubFactory.waitUntilCalled(5000);
        m_loggerStubFactory.assertOutputMessageContains("waiting");

        // ...then a reset message...
        getSender().send(new ResetGrinderMessage());

        m_loggerStubFactory.waitUntilCalled(5000);
        m_loggerStubFactory.assertOutputMessage("received a reset message");

        // Version string.
        m_loggerStubFactory.waitUntilCalled(5000);
        m_loggerStubFactory.assertOutputMessageContains("The Grinder");

        m_loggerStubFactory.waitUntilCalled(5000);
        m_loggerStubFactory.assertOutputMessageContains("waiting");

        // ...now try specifying the script...
        grinderProperties.setFile(GrinderProperties.SCRIPT, new File("foo.py"));
        getSender().send(new StartGrinderMessage(grinderProperties, 99));

        m_loggerStubFactory.waitUntilCalled(5000);
        m_loggerStubFactory.assertOutputMessage("received a start message");

        m_loggerStubFactory.waitUntilCalled(5000);
        m_loggerStubFactory.assertErrorMessageContains("foo.py");

        m_loggerStubFactory.waitUntilCalled(5000);
        m_loggerStubFactory.assertOutputMessageContains("waiting");

        // ..then a stop message.
        getSender().send(new StopGrinderMessage());
      }
    };

    final File propertyFile = new File(getDirectory(), "properties");
    final GrinderProperties properties = new GrinderProperties(propertyFile);

    final Agent agent = new AgentImplementation(m_logger, propertyFile, true);

    properties.setInt("grinder.consolePort", console.getPort());
    properties.save();

    agent.run();

    console.shutdown();

    m_loggerStubFactory.assertOutputMessage("received a stop message");

    // communication shutdown.
    m_loggerStubFactory.waitUntilCalled(5000);
    m_loggerStubFactory.assertSuccess("output", String.class, Integer.class);

    m_loggerStubFactory.assertNoMoreCalls();

    agent.shutdown();
    m_loggerStubFactory.assertOutputMessage("finished");
    m_loggerStubFactory.assertNoMoreCalls();
  }

  public void testRampUp() throws Exception {
    final ConsoleStub console = new ConsoleStub() {
      public void onConnect() throws Exception {
        // After we accept an agent connection...
        m_loggerStubFactory.assertOutputMessageContains("The Grinder");

        m_loggerStubFactory.waitUntilCalled(5000);
        m_loggerStubFactory.assertOutputMessageContains("connected");

        m_loggerStubFactory.waitUntilCalled(5000);
        m_loggerStubFactory.assertOutputMessageContains("waiting");

        // ...send a start message...
        final GrinderProperties grinderProperties = new GrinderProperties();
        getSender().send(new StartGrinderMessage(grinderProperties, 99));

        m_loggerStubFactory.waitUntilCalled(5000);
        m_loggerStubFactory.assertOutputMessage("received a start message");

        m_loggerStubFactory.waitUntilCalled(5000);

        m_loggerStubFactory.assertOutputMessageContains("DEBUG MODE");

        // 10 workers started.
        for (int i =  0; i < 10; ++i) {
          m_loggerStubFactory.waitUntilCalled(5000);
          m_loggerStubFactory.assertOutputMessageContains("started");
        }

        // Interrupt our workers.
        getSender().send(new ResetGrinderMessage());

        m_loggerStubFactory.waitUntilCalled(5000);
        m_loggerStubFactory.assertOutputMessageContains("reset");

        m_loggerStubFactory.waitUntilCalled(5000);
        m_loggerStubFactory.assertOutputMessageContains("The Grinder");

        m_loggerStubFactory.waitUntilCalled(5000);
        m_loggerStubFactory.assertOutputMessageContains("waiting");

        // Now try again, with no ramp up.
        grinderProperties.setInt("grinder.initialProcesses", 10);

        getSender().send(new StartGrinderMessage(grinderProperties, 99));

        m_loggerStubFactory.waitUntilCalled(5000);
        m_loggerStubFactory.assertOutputMessage("received a start message");

        m_loggerStubFactory.waitUntilCalled(5000);
        m_loggerStubFactory.assertOutputMessageContains("DEBUG MODE");

        // 10 workers started.
        for (int i = 0; i < 10; ++i) {
          m_loggerStubFactory.waitUntilCalled(5000);
          m_loggerStubFactory.assertOutputMessageContains("started");
        }

        // Shut down our workers.
        getSender().send(new StopGrinderMessage());
      }
    };

    final File propertyFile = new File(getDirectory(), "properties");
    final GrinderProperties properties = new GrinderProperties(propertyFile);

    final Agent agent = new AgentImplementation(m_logger, propertyFile, true);

    final File script = new File(getDirectory(), "grinder.py");
    assertTrue(script.createNewFile());

    properties.setInt("grinder.consolePort", console.getPort());
    properties.setInt("grinder.initialProcesses", 0);
    properties.setInt("grinder.processes", 10);
    properties.setInt("grinder.processIncrement", 1);
    properties.setInt("grinder.processIncrementInterval", 10);
    properties.setBoolean("grinder.debug.singleprocess", true);
    properties.setFile("grinder.script", script);
    properties.save();

    agent.run();

    console.shutdown();
    agent.shutdown();
  }

  public void testReconnect() throws Exception {
    final File propertyFile = new File(getDirectory(), "properties");
    final GrinderProperties properties = new GrinderProperties(propertyFile);

    final boolean[] secondConsoleContacted = new boolean[1];

    final GrinderProperties startProperties = new GrinderProperties();

    final ConsoleStub console2 = new ConsoleStub() {
      public void onConnect() throws Exception {

        startProperties.setFile("grinder.script", new File("not there"));

        getSender().send(new StartGrinderMessage(startProperties, 99));

        synchronized (secondConsoleContacted) {
          secondConsoleContacted[0] = true;
          secondConsoleContacted.notifyAll();
        }

        getSender().send(new StopGrinderMessage());
      }
    };

    final ConsoleStub console1 = new ConsoleStub() {
      public void onConnect() throws Exception {
        startProperties.setInt("grinder.consolePort", console2.getPort());

        getSender().send(new StartGrinderMessage(startProperties, 22));
      }
    };

    properties.setInt("grinder.consolePort", console1.getPort());
    properties.save();

    final Agent agent = new AgentImplementation(m_logger, propertyFile, true);

    agent.run();

    synchronized (secondConsoleContacted) {
      final long start = System.currentTimeMillis();

      while (!secondConsoleContacted[0] &&
             System.currentTimeMillis() < start + 10000) {
        secondConsoleContacted.wait(500);
      }
    }

    assertTrue(secondConsoleContacted[0]);

    console1.shutdown();
    console2.shutdown();

    agent.shutdown();
  }

  public void testFindAgentFile() throws Exception {
    System.setProperty("java.class.path", "");

    assertNull(AgentImplementation.findJavaAgentFile());

    System.setProperty("java.class.path",
                       "somewhere " + File.pathSeparatorChar + "somewhereelse");

    assertNull(AgentImplementation.findJavaAgentFile());

    final File directories = new File(getDirectory(), "a/b");
    directories.mkdirs();

    System.setProperty(
      "java.class.path",
      new File(directories.getAbsoluteFile(), "c.jar").getPath());
    assertNull(AgentImplementation.findJavaAgentFile());

    new File(directories, "grinder-agent.jar").createNewFile();
    assertNotNull(AgentImplementation.findJavaAgentFile());

    System.setProperty(
      "java.class.path",
      new File(getDirectory().getAbsoluteFile(), "c.jar").getPath());
    assertNull(AgentImplementation.findJavaAgentFile());

    new File(getDirectory(), "grinder-agent.jar").createNewFile();
    assertNotNull(AgentImplementation.findJavaAgentFile());

    System.setProperty("java.class.path", m_originalClassPath);

    assertNotNull(AgentImplementation.findJavaAgentFile());

    // I'd like also to test with relative paths, but this is impossible to
    // do in a platform independent manner.
  }

  private abstract class ConsoleStub {
    private final Acceptor m_acceptor;
    private final ServerReceiver m_receiver;
    private final Sender m_sender;

    public ConsoleStub() throws CommunicationException, IOException {
      // Figure out a free local port.
      final ServerSocket serverSocket = new ServerSocket(0);
      final int port = serverSocket.getLocalPort();
      serverSocket.close();

      m_acceptor = new Acceptor("", port, 1);
      m_receiver = new ServerReceiver();
      m_receiver.receiveFrom(
        m_acceptor, new ConnectionType[] { ConnectionType.AGENT }, 1, 10);
      m_sender = new FanOutServerSender(m_acceptor, ConnectionType.AGENT, 3);

      m_acceptor.addListener(ConnectionType.AGENT, new Acceptor.Listener() {
        public void connectionAccepted(ConnectionType connectionType,
                                       ConnectionIdentity connection) {
          try {
            onConnect();
          }
          catch (Exception e) {
            e.printStackTrace();
          }
        }

        public void connectionClosed(ConnectionType connectionType,
                                     ConnectionIdentity connection) { }
      });
    }

    public int getPort() {
      return m_acceptor.getPort();
    }

    public final void shutdown() throws CommunicationException {
      m_acceptor.shutdown();
      m_receiver.shutdown();
      getSender().shutdown();
    }

    public final Sender getSender() {
      return m_sender;
    }

    public abstract void onConnect() throws Exception;
  }

  public static class TestRunner implements IsolateGrinderProcessRunner {

    public int run(InputStream in) {
      try {
        final StreamReceiver receiver = new StreamReceiver(in);
        while (true) {
          final Message message = receiver.waitForMessage();
          if (message == null ||
              message instanceof ResetGrinderMessage ||
              message instanceof StopGrinderMessage) {
            return 0;
          }
        }
      }
      catch (Exception e) {
        e.printStackTrace();
        return -1;
      }
    }
  }
}
