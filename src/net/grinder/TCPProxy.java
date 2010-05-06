// Copyright (C) 2000 Phil Dawes
// Copyright (C) 2000 - 2010 Philip Aston
// Copyright (C) 2001 Paddy Spencer
// Copyright (C) 2003, 2004, 2005 Bertrand Ave
// Copyright (C) 2007 Venelin Mitov
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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import net.grinder.common.Logger;
import net.grinder.plugin.http.tcpproxyfilter.ConnectionCache;
import net.grinder.plugin.http.tcpproxyfilter.ConnectionHandlerFactoryImplementation;
import net.grinder.plugin.http.tcpproxyfilter.HTTPRecordingImplementation;
import net.grinder.plugin.http.tcpproxyfilter.HTTPRequestFilter;
import net.grinder.plugin.http.tcpproxyfilter.HTTPResponseFilter;
import net.grinder.plugin.http.tcpproxyfilter.ProcessHTTPRecordingWithXSLT;
import net.grinder.plugin.http.tcpproxyfilter.RegularExpressionsImplementation;
import net.grinder.tools.tcpproxy.CommentSourceImplementation;
import net.grinder.tools.tcpproxy.CompositeFilter;
import net.grinder.tools.tcpproxy.ConnectionDetails;
import net.grinder.tools.tcpproxy.EchoFilter;
import net.grinder.tools.tcpproxy.EndPoint;
import net.grinder.tools.tcpproxy.HTTPProxyTCPProxyEngine;
import net.grinder.tools.tcpproxy.NullFilter;
import net.grinder.tools.tcpproxy.PortForwarderTCPProxyEngine;
import net.grinder.tools.tcpproxy.TCPProxyConsole;
import net.grinder.tools.tcpproxy.TCPProxyEngine;
import net.grinder.tools.tcpproxy.TCPProxyFilter;
import net.grinder.tools.tcpproxy.TCPProxySSLSocketFactory;
import net.grinder.tools.tcpproxy.TCPProxySSLSocketFactoryImplementation;
import net.grinder.tools.tcpproxy.UpdatableCommentSource;
import net.grinder.util.AbstractMainClass;
import net.grinder.util.AttributeStringParserImplementation;
import net.grinder.util.FixedWidthFormatter;
import net.grinder.util.SimpleLogger;
import net.grinder.util.SimpleStringEscaper;
import net.grinder.util.URIParserImplementation;

import org.picocontainer.ComponentAdapter;
import org.picocontainer.defaults.DefaultPicoContainer;
import org.picocontainer.monitors.WriterComponentMonitor;


/**
 * This is the entry point of The TCPProxy process.
 *
 * @author Phil Dawes
 * @author Philip Aston
 * @author Bertrand Ave
 * @author Venelin Mitov
 * @version $Revision: 4220 $
 */
public final class TCPProxy extends AbstractMainClass {

  private static final String USAGE =
    "\n  java " + TCPProxy.class + " <options>" +
    "\n\n" +
    "Commonly used options:" +
    "\n  [-http [<stylesheet>]]       See below." +
    "\n  [-console]                   Display the console." +
    "\n  [-requestfilter <filter>]    Add a request filter." +
    "\n  [-responsefilter <filter>]   Add a response filter." +
    "\n  [-localhost <host name/ip>]  Default is localhost." +
    "\n  [-localport <port>]          Default is 8001." +
    "\n  [-keystore <file>]           Key store details for" +
    "\n  [-keystorepassword <pass>]   SSL certificates." +
    "\n  [-keystoretype <type>]       Default is JSSE dependent." +
    "\n\n" +
    "Other options:" +
    "\n  [-properties <file>]         Properties to pass to the filters." +
    "\n  [-remotehost <host name>]    Default is localhost." +
    "\n  [-remoteport <port>]         Default is 7001." +
    "\n  [-timeout <seconds>]         Proxy engine timeout." +
    "\n  [-httpproxy <host> <port>]   Route via HTTP/HTTPS proxy." +
    "\n  [-httpsproxy <host> <port>]  Override -httpproxy settings for" +
    "\n                               HTTPS." +
    "\n  [-ssl]                       Use SSL when port forwarding." +
    "\n  [-colour]                    Be pretty on ANSI terminals." +
    "\n  [-component <class>]         Register a component class with" +
    "\n                               the filter PicoContainer." +
    "\n  [-debug]                     Make PicoContainer chatty." +
    "\n\n" +
    "<filter> is the name of a class that implements " +
    TCPProxyFilter.class.getName() + " or one of NONE, ECHO. The default " +
    "is ECHO. Multiple filters can be specified for each stream." +
    "\n\n" +
    "By default, the TCPProxy listens as an HTTP/HTTPS Proxy on " +
    "<localhost:localport>." +
    "\n\n" +
    "If either -remotehost or -remoteport is specified, the TCPProxy " +
    "acts a simple port forwarder between <localhost:localport> and " +
    "<remotehost:remoteport>. Specify -ssl for SSL support." +
    "\n\n" +
    "-http sets up request and response filters to produce a test script " +
    "suitable for use with the HTTP plugin. The output can be customised " +
    "by specifying the file name of an alternative XSLT style sheet." +
    "\n\n" +
    "-timeout is how long the TCPProxy will wait for a request " +
    "before timing out and freeing the local port. The TCPProxy will " +
    "not time out if there are active connections." +
    "\n\n" +
    "-console displays a simple control window that allows the TCPProxy " +
    "to be shutdown cleanly. This is needed because some shells, e.g. " +
    "Cygwin bash, do not allow Java processes to be interrupted cleanly, " +
    "so filters cannot rely on standard shutdown hooks. " +
    "\n\n" +
    "-httpproxy and -httpsproxy allow output to be directed through " +
    "another HTTP/HTTPS proxy; this may help you reach the Internet. " +
    "These options are not supported in port forwarding mode." +
    "\n\n" +
    "Typical usage: " +
    "\n  java " + TCPProxy.class + " -http -console > grinder.py" +
    "\n\n";

  /**
   * Entry point.
   *
   * @param args Command line arguments.
   */
  public static void main(String[] args) {
    final Logger logger =
      new SimpleLogger("tcpproxy",
                       new PrintWriter(System.out),
                       new PrintWriter(System.err),
                       new FixedWidthFormatter(
                         FixedWidthFormatter.Align.LEFT,
                         FixedWidthFormatter.Flow.WORD_WRAP,
                         80));

    try {
      final TCPProxy tcpProxy = new TCPProxy(args, logger);
      tcpProxy.run();
    }
    catch (LoggedInitialisationException e) {
      System.exit(1);
    }
    catch (Throwable e) {
      logger.error("Could not initialise:");
      final PrintWriter errorWriter = logger.getErrorLogWriter();
      e.printStackTrace(errorWriter);
      errorWriter.flush();
      System.exit(2);
    }

    System.exit(0);
  }

  private final DefaultPicoContainer m_filterContainer =
    new DefaultPicoContainer();

  private final TCPProxyEngine m_proxyEngine;

  private TCPProxy(String[] args, Logger logger) throws Exception {
    super(logger, USAGE);

    m_filterContainer.registerComponentInstance(logger);

    final UpdatableCommentSource commentSource =
      new CommentSourceImplementation();
    m_filterContainer.registerComponentInstance(commentSource);

    // Default values.
    int localPort = 8001;
    String remoteHost = "localhost";
    String localHost = "localhost";
    int remotePort = 7001;
    boolean useSSLPortForwarding = false;
    File keyStoreFile = null;
    char[] keyStorePassword = null;
    String keyStoreType = null;
    boolean isHTTPProxy = true;
    boolean console = false;
    EndPoint chainedHTTPProxy = null;
    EndPoint chainedHTTPSProxy = null;
    int timeout = 0;
    boolean useColour = false;

    final FilterChain requestFilterChain = new FilterChain("request");
    final FilterChain responseFilterChain = new FilterChain("response");

    try {
      // Parse 1.
      for (int i = 0; i < args.length; i++) {
        if (args[i].equalsIgnoreCase("-properties")) {
          final Properties properties = new Properties();
          final FileInputStream in = new FileInputStream(new File(args[++i]));
          try {
            properties.load(in);
          }
          finally {
            in.close();
          }
          System.getProperties().putAll(properties);
        }
      }

      // Parse 2.
      for (int i = 0; i < args.length; i++) {
        if (args[i].equalsIgnoreCase("-requestfilter")) {
          requestFilterChain.add(args[++i]);
        }
        else if (args[i].equalsIgnoreCase("-responsefilter")) {
          responseFilterChain.add(args[++i]);
        }
        else if (args[i].equalsIgnoreCase("-component")) {
          final Class<?> componentClass;

          try {
            componentClass = Class.forName(args[++i]);
          }
          catch (ClassNotFoundException e) {
            throw barfError("class '" + args[i] + "' not found.");
          }
          m_filterContainer.registerComponentImplementation(componentClass);
        }
        else if (args[i].equalsIgnoreCase("-http")) {
          requestFilterChain.add(HTTPRequestFilter.class);
          responseFilterChain.add(HTTPResponseFilter.class);
          m_filterContainer.registerComponentImplementation(
            AttributeStringParserImplementation.class);
          m_filterContainer.registerComponentImplementation(
            ConnectionCache.class);
          m_filterContainer.registerComponentImplementation(
            ConnectionHandlerFactoryImplementation.class);
          m_filterContainer.registerComponentImplementation(
            HTTPRecordingImplementation.class);
          m_filterContainer.registerComponentImplementation(
            ProcessHTTPRecordingWithXSLT.class);
          m_filterContainer.registerComponentImplementation(
            RegularExpressionsImplementation.class);
          m_filterContainer.registerComponentImplementation(
            URIParserImplementation.class);
          m_filterContainer.registerComponentImplementation(
            SimpleStringEscaper.class);

          if (i + 1 < args.length && !args[i + 1].startsWith("-")) {
            m_filterContainer.registerComponentInstance(
              new ProcessHTTPRecordingWithXSLT.StyleSheetInputStream(
                new File(args[++i])));
          }
        }
        else if (args[i].equalsIgnoreCase("-localhost")) {
          localHost = args[++i];
        }
        else if (args[i].equalsIgnoreCase("-localport")) {
          localPort = Integer.parseInt(args[++i]);
        }
        else if (args[i].equalsIgnoreCase("-remotehost")) {
          remoteHost = args[++i];
          isHTTPProxy = false;
        }
        else if (args[i].equalsIgnoreCase("-remoteport")) {
          remotePort = Integer.parseInt(args[++i]);
          isHTTPProxy = false;
        }
        else if (args[i].equalsIgnoreCase("-ssl")) {
          useSSLPortForwarding = true;
        }
        else if (args[i].equalsIgnoreCase("-keystore")) {
          keyStoreFile = new File(args[++i]);
        }
        else if (args[i].equalsIgnoreCase("-keystorepassword") ||
                 args[i].equalsIgnoreCase("-storepass")) {
          keyStorePassword = args[++i].toCharArray();
        }
        else if (args[i].equalsIgnoreCase("-keystoretype") ||
                 args[i].equalsIgnoreCase("-storetype")) {
          keyStoreType = args[++i];
        }
        else if (args[i].equalsIgnoreCase("-timeout")) {
          timeout = Integer.parseInt(args[++i]) * 1000;
        }
        else if (args[i].equalsIgnoreCase("-console")) {
          console = true;
        }
        else if (args[i].equalsIgnoreCase("-colour") ||
                 args[i].equalsIgnoreCase("-color")) {
          useColour = true;
        }
        else if (args[i].equalsIgnoreCase("-properties")) {
          /* Already handled */
          ++i;
        }
        else if (args[i].equalsIgnoreCase("-httpproxy")) {
          chainedHTTPProxy =
            new EndPoint(args[++i], Integer.parseInt(args[++i]));
        }
        else if (args[i].equalsIgnoreCase("-httpsproxy")) {
          chainedHTTPSProxy =
            new EndPoint(args[++i], Integer.parseInt(args[++i]));
        }
        else if (args[i].equalsIgnoreCase("-debug")) {
          m_filterContainer.changeMonitor(
            new WriterComponentMonitor(logger.getErrorLogWriter()));
        }
        else if (args[i].equalsIgnoreCase("-initialtest")) {
          final String argument = i + 1 < args.length ? args[++i] : "123";
          throw barfError("-initialTest is no longer supported. " +
                          "Use -DHTTPPlugin.initialTest=" + argument +
                          " or the -properties option instead.");
        }
        else {
          throw barfUsage();
        }
      }
    }
    catch (FileNotFoundException fnfe) {
      throw barfError(fnfe.getMessage());
    }
    catch (IndexOutOfBoundsException e) {
      throw barfUsage();
    }
    catch (NumberFormatException e) {
      throw barfUsage();
    }

    if (timeout < 0) {
      throw barfError("timeout must be non-negative.");
    }

    final EndPoint localEndPoint = new EndPoint(localHost, localPort);
    final EndPoint remoteEndPoint = new EndPoint(remoteHost, remotePort);

    if (chainedHTTPSProxy == null && chainedHTTPProxy != null) {
      chainedHTTPSProxy = chainedHTTPProxy;
    }

    if (chainedHTTPSProxy != null && !isHTTPProxy) {
      throw barfError("routing through a HTTP/HTTPS proxy is not supported " +
                      "in port forwarding mode.");
    }

    final TCPProxyFilter requestFilter = requestFilterChain.resolveFilter();
    final TCPProxyFilter responseFilter = responseFilterChain.resolveFilter();

    final StringBuffer startMessage = new StringBuffer();

    startMessage.append("Initialising as ");

    if (isHTTPProxy) {
      startMessage.append("an HTTP/HTTPS proxy");
    }
    else {
      if (useSSLPortForwarding) {
        startMessage.append("an SSL port forwarder");
      }
      else {
        startMessage.append("a TCP port forwarder");
      }
    }

    startMessage.append(" with the parameters:");
    startMessage.append("\n   Request filters:    ");
    startMessage.append(requestFilter);
    startMessage.append("\n   Response filters:   ");
    startMessage.append(responseFilter);
    startMessage.append("\n   Local address:      " + localEndPoint);

    if (!isHTTPProxy) {
      startMessage.append("\n   Remote address:     " + remoteEndPoint);
    }

    if (chainedHTTPProxy != null) {
      startMessage.append("\n   HTTP proxy:         " + chainedHTTPProxy);
    }

    if (chainedHTTPSProxy != null) {
      startMessage.append("\n   HTTPS proxy:        " + chainedHTTPSProxy);
    }

    if (keyStoreFile != null) {
      startMessage.append("\n   Key store:          ");
      startMessage.append(keyStoreFile.toString());

      // Key store password is optional.
      if (keyStorePassword != null) {
        startMessage.append("\n   Key store password: ");
        for (int i = 0; i < keyStorePassword.length; ++i) {
          startMessage.append('*');
        }
      }

      // Key store type can be null => use whatever
      // KeyStore.getDefaultType() says (we can't print the default
      // here without loading the JSSE).
      if (keyStoreType != null) {
        startMessage.append("\n   Key store type:     " + keyStoreType);
      }
    }

    logger.error(startMessage.toString());

    final TCPProxySSLSocketFactory sslSocketFactory =
      keyStoreFile != null ?
      new TCPProxySSLSocketFactoryImplementation(keyStoreFile,
                                                 keyStorePassword,
                                                 keyStoreType) :
      new TCPProxySSLSocketFactoryImplementation();

    m_filterContainer.start();

    if (isHTTPProxy) {
      m_proxyEngine =
        new HTTPProxyTCPProxyEngine(
          sslSocketFactory,
          requestFilter, responseFilter,
          logger,
          localEndPoint,
          useColour,
          timeout,
          chainedHTTPProxy, chainedHTTPSProxy);
    }
    else {
      if (useSSLPortForwarding) {
        m_proxyEngine =
          new PortForwarderTCPProxyEngine(
            sslSocketFactory,
            requestFilter, responseFilter,
            logger,
            new ConnectionDetails(localEndPoint, remoteEndPoint, true),
            useColour,
            timeout);
      }
      else {
        m_proxyEngine =
          new PortForwarderTCPProxyEngine(
            requestFilter, responseFilter,
            logger,
            new ConnectionDetails(localEndPoint, remoteEndPoint, false),
            useColour,
            timeout);
      }
    }

    if (console) {
      new TCPProxyConsole(m_proxyEngine, commentSource);
    }

    logger.error("Engine initialised, listening on port " + localPort);
  }

  private void run() {
    final Runnable shutdown = new Runnable() {
      private boolean m_stopped = false;

      public synchronized void run() {
        if (!m_stopped) {
          m_stopped = true;
          m_proxyEngine.stop();
          m_filterContainer.stop();
          m_filterContainer.dispose();
        }
      }
    };

    Runtime.getRuntime().addShutdownHook(new Thread(shutdown));

    m_proxyEngine.run();
    shutdown.run();

    // Write to stderr - only filter output should go to stdout.
    getLogger().error("Engine exited");
  }

  private final class FilterChain {
    private final String m_type;
    private final List<ComponentAdapter> m_adapterList =
      new ArrayList<ComponentAdapter>();
    private int m_value;

    public FilterChain(String type) {
      m_type = type;
    }

    public void add(Class<?> theClass) {
      m_adapterList.add(
        m_filterContainer.registerComponentImplementation(
          m_type + ++m_value, theClass));
    }

    public void add(String filterClassName)
      throws LoggedInitialisationException {

      if (filterClassName.equals("NONE")) {
        add(NullFilter.class);
      }
      else if (filterClassName.equals("ECHO")) {
        add(EchoFilter.class);
      }
      else {
        final Class<?> filterClass;

        try {
          filterClass = Class.forName(filterClassName);
        }
        catch (ClassNotFoundException e) {
          throw barfError("class '" + filterClassName + "' not found.");
        }

        if (!TCPProxyFilter.class.isAssignableFrom(filterClass)) {
          throw barfError("the class '" + filterClass.getName() +
                          "' does not implement the interface: '" +
                          TCPProxyFilter.class.getName() + "'.");
        }

        add(filterClass);
      }
    }

    public TCPProxyFilter resolveFilter() {
      if (m_adapterList.size() == 0) {
        add(EchoFilter.class);
      }

      final CompositeFilter result = new CompositeFilter();

      for (ComponentAdapter adapter : m_adapterList) {
        result.add(
          (TCPProxyFilter)adapter.getComponentInstance(m_filterContainer));
      }

      return result;
    }
  }
}
