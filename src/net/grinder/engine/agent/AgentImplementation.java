// Copyright (C) 2000 Paco Gomez
// Copyright (C) 2000 - 2010 Philip Aston
// Copyright (C) 2004 Bertrand Ave
// Copyright (C) 2008 Pawel Lacinski
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
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Timer;
import java.util.TimerTask;

import net.grinder.common.GrinderBuild;
import net.grinder.common.GrinderException;
import net.grinder.common.GrinderProperties;
import net.grinder.common.Logger;
import net.grinder.common.GrinderProperties.PersistenceException;
import net.grinder.communication.ClientReceiver;
import net.grinder.communication.ClientSender;
import net.grinder.communication.CommunicationException;
import net.grinder.communication.ConnectionType;
import net.grinder.communication.Connector;
import net.grinder.communication.FanOutStreamSender;
import net.grinder.communication.IgnoreShutdownSender;
import net.grinder.communication.MessageDispatchSender;
import net.grinder.communication.MessagePump;
import net.grinder.communication.TeeSender;
import net.grinder.engine.common.ConnectorFactory;
import net.grinder.engine.common.EngineException;
import net.grinder.engine.common.ScriptLocation;
import net.grinder.engine.communication.ConsoleListener;
import net.grinder.messages.agent.StartGrinderMessage;
import net.grinder.messages.console.AgentAddress;
import net.grinder.messages.console.AgentProcessReportMessage;
import net.grinder.util.Directory;
import net.grinder.util.Directory.DirectoryException;
import net.grinder.util.thread.Condition;


/**
 * This is the entry point of The Grinder agent process.
 *
 * @author Paco Gomez
 * @author Philip Aston
 * @author Bertrand Ave
 * @author Pawel Lacinski
 * @version $Revision: 4234 $
 */
public final class AgentImplementation implements Agent {

  private static final String AGENT_JAR_FILENAME = "grinder-agent.jar";

  private final Logger m_logger;
  private final File m_alternateFile;
  private final boolean m_proceedWithoutConsole;
  private final File m_javaAgentFile;

  private final Timer m_timer = new Timer(true);
  private final Condition m_eventSynchronisation = new Condition();
  private final AgentIdentityImplementation m_agentIdentity;
  private final ConsoleListener m_consoleListener;
  private final FanOutStreamSender m_fanOutStreamSender =
    new FanOutStreamSender(3);
  private final ConnectorFactory m_connectorFactory =
    new ConnectorFactory(ConnectionType.AGENT);

  /**
   * We use an most one file store throughout an agent's life, but can't
   * initialise it until we've read the properties and connected to the console.
   */
  private volatile FileStore m_fileStore;

  /**
   * Constructor.
   *
   * @param logger Logger.
   * @param alternateFile Alternative properties file, or <code>null</code>.
   * @param proceedWithoutConsole <code>true</code> => proceed if a console
   * connection could not be made.
   * @throws GrinderException If an error occurs.
   */
  public AgentImplementation(Logger logger,
                             File alternateFile,
                             boolean proceedWithoutConsole)
    throws GrinderException {

    m_logger = logger;
    m_alternateFile = alternateFile;
    m_proceedWithoutConsole = proceedWithoutConsole;

    m_consoleListener = new ConsoleListener(m_eventSynchronisation, m_logger);
    m_agentIdentity = new AgentIdentityImplementation(getHostName());
    m_javaAgentFile = findJavaAgentFile();
  }

  /**
   * Package scope for unit tests.
   */
  static File findJavaAgentFile() {
    final String[] classPath =
      System.getProperty("java.class.path").split(File.pathSeparator);

    for (String classPathEntry : classPath) {
      final File siblingFile =
        new File(new File(classPathEntry).getParent(), AGENT_JAR_FILENAME);

      if (siblingFile.exists()) {
        return siblingFile;
      }
    }

    return null;
  }

  /**
   * Run the Grinder agent process.
   *
   * @throws GrinderException
   *             If an error occurs.
   */
  public void run() throws GrinderException {

    StartGrinderMessage startMessage = null;
    ConsoleCommunication consoleCommunication = null;

    try {
      while (true) {
        m_logger.output(GrinderBuild.getName());

        ScriptLocation script = null;
        GrinderProperties properties;

        do {
          properties =
            createAndMergeProperties(startMessage != null ?
                                     startMessage.getProperties() : null);

          m_agentIdentity.setName(
            properties.getProperty("grinder.hostID", getHostName()));

          final Connector connector =
            properties.getBoolean("grinder.useConsole", true) ?
            m_connectorFactory.create(properties) : null;

          // We only reconnect if the connection details have changed.
          if (consoleCommunication != null &&
              !consoleCommunication.getConnector().equals(connector)) {
            shutdownConsoleCommunication(consoleCommunication);
            consoleCommunication = null;
            // Accept any startMessage from previous console - see bug 2092881.
          }

          if (consoleCommunication == null && connector != null) {
            try {
              consoleCommunication = new ConsoleCommunication(connector);
              consoleCommunication.start();
              m_logger.output(
                "connected to console at " + connector.getEndpointAsString());
            }
            catch (CommunicationException e) {
              if (m_proceedWithoutConsole) {
                m_logger.error(
                  e.getMessage() + ", proceeding without the console; set " +
                  "grinder.useConsole=false to disable this warning.");
              }
              else {
                m_logger.error(e.getMessage());
                return;
              }
            }
          }

          if (consoleCommunication != null && startMessage == null) {
            m_logger.output("waiting for console signal");
            m_consoleListener.waitForMessage();

            if (m_consoleListener.received(ConsoleListener.START)) {
              startMessage = m_consoleListener.getLastStartGrinderMessage();
              continue; // Loop to handle new properties.
            }
            else {
              break;    // Another message, check at end of outer while loop.
            }
          }

          if (startMessage != null) {
            final GrinderProperties messageProperties =
              startMessage.getProperties();
            final Directory fileStoreDirectory = m_fileStore.getDirectory();

            // Convert relative path to absolute path.
            messageProperties.setAssociatedFile(
              fileStoreDirectory.getFile(
                messageProperties.getAssociatedFile()));

            final File consoleScript =
              messageProperties.resolveRelativeFile(
                messageProperties.getFile(GrinderProperties.SCRIPT,
                                          GrinderProperties.DEFAULT_SCRIPT));

            // We only fall back to the agent properties if the start message
            // doesn't specify a script and there is no default script.
            if (messageProperties.containsKey(GrinderProperties.SCRIPT) ||
                consoleScript.canRead()) {
              // The script directory may not be the file's direct parent.
              script = new ScriptLocation(fileStoreDirectory, consoleScript);
            }

            m_agentIdentity.setNumber(startMessage.getAgentNumber());
          }
          else {
            m_agentIdentity.setNumber(-1);
          }

          if (script == null) {
            final File scriptFile =
              properties.resolveRelativeFile(
                properties.getFile(GrinderProperties.SCRIPT,
                                   GrinderProperties.DEFAULT_SCRIPT));

            try {
              script = new ScriptLocation(scriptFile);
            }
            catch (DirectoryException e) {
              m_logger.error("The script '" + scriptFile + "' does not exist.");
              break;
            }
          }

          if (!script.getFile().canRead()) {
            m_logger.error("The script file '" + script +
                           "' does not exist or is not readable.");
            script = null;
            break;
          }
        }
        while (script == null);

        if (script != null) {
          final String jvmArguments =
            properties.getProperty("grinder.jvm.arguments");

          final WorkerFactory workerFactory;

          if (!properties.getBoolean("grinder.debug.singleprocess", false)) {

            final WorkerProcessCommandLine workerCommandLine =
              new WorkerProcessCommandLine(properties,
                                           System.getProperties(),
                                           m_javaAgentFile,
                                           jvmArguments);

            m_logger.output(
              "Worker process command line: " + workerCommandLine);

            workerFactory =
              new ProcessWorkerFactory(
                workerCommandLine, m_agentIdentity, m_fanOutStreamSender,
                consoleCommunication != null, script, properties);
          }
          else {
            m_logger.output(
              "DEBUG MODE: Spawning threads rather than processes");

            if (jvmArguments != null) {
              m_logger.output("WARNING grinder.jvm.arguments (" + jvmArguments +
                              ") ignored in single process mode");
            }

            workerFactory =
              new DebugThreadWorkerFactory(
                m_agentIdentity, m_fanOutStreamSender,
                consoleCommunication != null, script, properties);
          }

          final WorkerLauncher workerLauncher =
            new WorkerLauncher(properties.getInt("grinder.processes", 1),
                               workerFactory,
                               m_eventSynchronisation,
                               m_logger);

          final int increment =
            properties.getInt("grinder.processIncrement", 0);

          if (increment > 0) {
            final boolean moreProcessesToStart =
              workerLauncher.startSomeWorkers(
                properties.getInt("grinder.initialProcesses", increment));

            if (moreProcessesToStart) {
              final int incrementInterval =
                properties.getInt("grinder.processIncrementInterval", 60000);

              final RampUpTimerTask rampUpTimerTask =
                new RampUpTimerTask(workerLauncher, increment);

              m_timer.scheduleAtFixedRate(
                rampUpTimerTask, incrementInterval, incrementInterval);
            }
          }
          else {
            workerLauncher.startAllWorkers();
          }

          // Wait for a termination event.
          synchronized (m_eventSynchronisation) {
            final long maximumShutdownTime = 20000;
            long consoleSignalTime = -1;

            while (!workerLauncher.allFinished()) {
              if (consoleSignalTime == -1 &&
                  m_consoleListener.checkForMessage(ConsoleListener.ANY ^
                                                    ConsoleListener.START)) {
                workerLauncher.dontStartAnyMore();
                consoleSignalTime = System.currentTimeMillis();
              }

              if (consoleSignalTime >= 0 &&
                  System.currentTimeMillis() - consoleSignalTime >
                  maximumShutdownTime) {

                m_logger.output("forcibly terminating unresponsive processes");

                // destroyAllWorkers() prevents further workers from starting.
                workerLauncher.destroyAllWorkers();
              }

              m_eventSynchronisation.waitNoInterrruptException(
                maximumShutdownTime);
            }
          }

          workerLauncher.shutdown();
        }

        if (consoleCommunication == null) {
          break;
        }
        else {
          // Ignore any pending start messages.
          m_consoleListener.discardMessages(ConsoleListener.START);

          if (!m_consoleListener.received(ConsoleListener.ANY)) {
            // We've got here naturally, without a console signal.
            m_logger.output("finished, waiting for console signal");
            m_consoleListener.waitForMessage();
          }

          if (m_consoleListener.received(ConsoleListener.START)) {
            startMessage = m_consoleListener.getLastStartGrinderMessage();
          }
          else if (m_consoleListener.received(ConsoleListener.STOP |
                                              ConsoleListener.SHUTDOWN)) {
            break;
          }
          else {
            // ConsoleListener.RESET or natural death.
            startMessage = null;
          }
        }
      }
    }
    finally {
      shutdownConsoleCommunication(consoleCommunication);
    }
  }

  private GrinderProperties createAndMergeProperties(
      GrinderProperties startMessageProperties)
    throws PersistenceException {

    final GrinderProperties properties =
      new GrinderProperties(
        m_alternateFile != null ?
            m_alternateFile : GrinderProperties.DEFAULT_PROPERTIES);

    if (startMessageProperties != null) {
      properties.putAll(startMessageProperties);
    }

    // Ensure the log directory property is set and is absolute.
    final File nullFile = new File("");

    final File originalLogDirectory =
      properties.getFile(GrinderProperties.LOG_DIRECTORY, nullFile);

    if (!originalLogDirectory.isAbsolute()) {
      properties.setFile(GrinderProperties.LOG_DIRECTORY,
        new File(nullFile.getAbsoluteFile(), originalLogDirectory.getPath()));
    }

    return properties;
  }

  private void shutdownConsoleCommunication(
    ConsoleCommunication consoleCommunication) {

    if (consoleCommunication != null) {
      consoleCommunication.shutdown();
    }

    m_consoleListener.discardMessages(ConsoleListener.ANY);
  }

  /**
   * Clean up resources.
   */
  public void shutdown() {
    m_timer.cancel();
    m_fanOutStreamSender.shutdown();
    m_consoleListener.shutdown();

    m_logger.output("finished");
  }

  private static String getHostName() {
    try {
      return InetAddress.getLocalHost().getHostName();
    }
    catch (UnknownHostException e) {
      return "UNNAMED HOST";
    }
  }

  private static class RampUpTimerTask extends TimerTask {

    private final WorkerLauncher m_processLauncher;
    private final int m_processIncrement;

    public RampUpTimerTask(WorkerLauncher processLauncher,
                           int processIncrement) {
      m_processLauncher = processLauncher;
      m_processIncrement = processIncrement;
    }

    public void run() {
      try {
        final boolean moreProcessesToStart =
          m_processLauncher.startSomeWorkers(m_processIncrement);

        if (!moreProcessesToStart) {
          super.cancel();
        }
      }
      catch (EngineException e) {
        // Really an assertion. Can't use logger because its not thread-safe.
        System.err.println("Failed to start processes");
        e.printStackTrace();
      }
    }
  }

  private final class ConsoleCommunication {
    private final ClientSender m_sender;
    private final Connector m_connector;
    private final TimerTask m_reportRunningTask;
    private final MessagePump m_messagePump;

    public ConsoleCommunication(Connector connector)
        throws CommunicationException, FileStore.FileStoreException {

      final ClientReceiver receiver =
        ClientReceiver.connect(connector, new AgentAddress(m_agentIdentity));
      m_sender = ClientSender.connect(receiver);
      m_connector = connector;

      if (m_fileStore == null) {
        // Only create the file store if we connected.
        m_fileStore =
          new FileStore(
            new File("./" + m_agentIdentity.getName() + "-file-store"),
            m_logger);
      }

      m_sender.send(
        new AgentProcessReportMessage(
          m_agentIdentity,
          AgentProcessReportMessage.STATE_STARTED,
          m_fileStore.getCacheHighWaterMark()));

      final MessageDispatchSender fileStoreMessageDispatcher =
        new MessageDispatchSender();
      m_fileStore.registerMessageHandlers(fileStoreMessageDispatcher);

      final MessageDispatchSender messageDispatcher =
        new MessageDispatchSender();
      m_consoleListener.registerMessageHandlers(messageDispatcher);

      // Everything that the file store doesn't handle is tee'd to the
      // worker processes and our message handlers.
      fileStoreMessageDispatcher.addFallback(
        new TeeSender(messageDispatcher,
          new IgnoreShutdownSender(m_fanOutStreamSender)));

      m_messagePump =
        new MessagePump(receiver, fileStoreMessageDispatcher, 1);

      m_reportRunningTask = new TimerTask() {
        public void run() {
          try {
            m_sender.send(
              new AgentProcessReportMessage(
                m_agentIdentity,
                AgentProcessReportMessage.STATE_RUNNING,
                m_fileStore.getCacheHighWaterMark()));
          }
          catch (CommunicationException e) {
            cancel();
            e.printStackTrace();
          }
        }
      };
    }

    public void start() {
      m_messagePump.start();
      m_timer.schedule(m_reportRunningTask, 1000, 1000);
    }

    public Connector getConnector() {
      return m_connector;
    }

    public void shutdown() {
      m_reportRunningTask.cancel();

      try {
        m_sender.send(
          new AgentProcessReportMessage(
            m_agentIdentity,
            AgentProcessReportMessage.STATE_FINISHED,
            m_fileStore.getCacheHighWaterMark()));
      }
      catch (CommunicationException e) {
        // Ignore - peer has probably shut down.
      }
      finally {
        m_messagePump.shutdown();
      }
    }
  }
}
