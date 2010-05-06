// Copyright (C) 2000 - 2009 Philip Aston
// Copyright (C) 2000, 2001 Phil Dawes
// Copyright (C) 2001 Paddy Spencer
// Copyright (C) 2003 Bertrand Ave
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

package net.grinder.tools.tcpproxy;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import net.grinder.common.GrinderBuild;
import net.grinder.common.Logger;
import net.grinder.common.UncheckedInterruptedException;
import net.grinder.util.StreamCopier;
import net.grinder.util.html.HTMLElement;
import net.grinder.util.thread.InterruptibleRunnable;


/**
 * HTTP/HTTPS proxy implementation.
 *
 * <p>A HTTPS proxy client first send a CONNECT message to the proxy
 * port. The proxy accepts the connection responds with a 200 OK,
 * which is the client's queue to send SSL data to the proxy. The
 * proxy just forwards it on to the server identified by the CONNECT
 * message.</p>
 *
 * <p>The Java API presents a particular challenge: it allows sockets
 * to be either SSL or not SSL, but doesn't let them change their
 * stripes midstream. (In fact, if the JSSE support was stream
 * oriented rather than socket oriented, a lot of problems would go
 * away). To hack around this, we accept the CONNECT then blindly
 * proxy the rest of the stream through a special
 * TCPProxyEngineImplementation which instantiated to handle SSL.</p>
 *
 * @author Paddy Spencer
 * @author Philip Aston
 * @author Bertrand Ave
 * @version $Revision: 4003 $
 */
public final class HTTPProxyTCPProxyEngine extends AbstractTCPProxyEngine {

  private static final long s_connectTimeout =
    Long.getLong("tcpproxy.connecttimeout", 5000).longValue();

  private final Pattern m_httpConnectPattern;
  private final Pattern m_httpsConnectPattern;
  private final Pattern m_httpsProxyResponsePattern;
  private final ProxySSLEngine m_proxySSLEngine;
  private final Thread m_proxySSLEngineThread;
  private final EndPoint m_chainedHTTPProxy;
  private final HTTPSProxySocketFactory m_httpsProxySocketFactory;
  private final EndPoint m_proxyAddress;

  /**
   * Constructor.
   *
   * @param sslSocketFactory Factory for SSL sockets.
   * @param requestFilter Request filter.
   * @param responseFilter Response filter.
   * @param logger Logger.
   * @param localEndPoint Local host and port.
   * @param useColour Whether to use colour.
   * @param timeout Timeout for server socket in milliseconds.
   * @param chainedHTTPProxy HTTP proxy which output should be routed
   * through, or <code>null</code> for no proxy.
   * @param chainedHTTPSProxy HTTP proxy which output should be routed
   * through, or <code>null</code> for no proxy.
   *
   * @exception IOException If an I/O error occurs
   * @exception PatternSyntaxException If a regular expression
   * error occurs.
   */
  public HTTPProxyTCPProxyEngine(TCPProxySSLSocketFactory sslSocketFactory,
                                 TCPProxyFilter requestFilter,
                                 TCPProxyFilter responseFilter,
                                 Logger logger,
                                 EndPoint localEndPoint,
                                 boolean useColour,
                                 int timeout,
                                 EndPoint chainedHTTPProxy,
                                 EndPoint chainedHTTPSProxy)
    throws IOException, PatternSyntaxException {

    // We set this engine up for handling plain connections. We
    // delegate HTTPS to a proxy engine.
    super(new TCPProxySocketFactoryImplementation(), requestFilter,
          responseFilter, logger, localEndPoint, useColour, timeout);

    m_proxyAddress = localEndPoint;
    m_chainedHTTPProxy = chainedHTTPProxy;

    m_httpConnectPattern =
      Pattern.compile("^([A-Z]+)[ \\t]+http://([^/:]+):?(\\d*)/.*\r\n\r\n",
                      Pattern.DOTALL);

    m_httpsConnectPattern =
      Pattern.compile("^CONNECT[ \\t]+([^:]+):(\\d+).*\r\n\r\n",
                      Pattern.DOTALL);

    m_httpsProxyResponsePattern =
      Pattern.compile("^HTTP.*? (\\d+) .*", Pattern.DOTALL);

    // When handling HTTPS proxies, we use our plain socket to accept
    // connections on. We suck the bit we understand off the front and
    // forward the rest through our proxy engine. The proxy engine
    // listens for connection attempts (which come from us), then sets
    // up a thread pair which pushes data back and forth until either
    // the server closes the connection, or we do (in response to our
    // client closing the connection). The engine handles multiple
    // connections by spawning multiple thread pairs.
    if (chainedHTTPSProxy != null) {
      m_httpsProxySocketFactory =
        new HTTPSProxySocketFactory(sslSocketFactory, chainedHTTPSProxy);

      m_proxySSLEngine =
        new ProxySSLEngine(m_httpsProxySocketFactory, getRequestFilter(),
                           getResponseFilter(), logger, useColour);
    }
    else {
      m_httpsProxySocketFactory = null;

      m_proxySSLEngine =
        new ProxySSLEngine(sslSocketFactory, getRequestFilter(),
                           getResponseFilter(), logger, useColour);
    }

    m_proxySSLEngineThread =
      new Thread(m_proxySSLEngine, "HTTPS proxy SSL engine");
    m_proxySSLEngineThread.start();
  }

  /**
   * Main event loop.
   */
  public void run() {

    // I've seen pathological messages with huge tracking cookies that are
    // bigger than 4K. Let's super-size this.
    final byte[] buffer = new byte[40960];

    while (!isStopped()) {
      final Socket localSocket;

      try {
        localSocket = accept();
      }
      catch (IOException e) {
        UncheckedInterruptedException.ioException(e);
        logIOException(e);
        continue;
      }

      try {
        final BufferedInputStream in =
          new BufferedInputStream(localSocket.getInputStream(), buffer.length);

        in.mark(buffer.length);

        int time = 0;

        // Read data into buffer until we time out or one of the handlers
        // matches.
        while (true) {
          while (time < s_connectTimeout && in.available() == 0) {
            sleep(10);
            time += 10;
          }

          final boolean timeout = in.available() == 0;

          // Rewind our buffered stream: easier than maintaining a cursor.
          in.reset();

          final int bytesRead;

          if (in.available() > 0) {
            bytesRead = in.read(buffer);
          }
          else {
            bytesRead = 0;
          }

          final String bufferAsString =
            new String(buffer, 0, bytesRead, "US-ASCII");

          if (timeout) {
            // Time out without matching a handler.
            final HTMLElement message = new HTMLElement();

            message.addElement("p").addText(
              "Failed to determine proxy destination.");

            if (bufferAsString.length() > 0) {
              final HTMLElement paragraph1 = message.addElement("p");
              paragraph1.addText(
                "Do not type TCPProxy address into your browser. ");
              paragraph1.addText("The browser proxy settings should be set " +
                                 "to the TCPProxy address (");
              paragraph1.addElement("code").addText(m_proxyAddress.toString());
              paragraph1.addText("), and you should type the address of the " +
                               "target server into the browser.");
              message.addElement("p").addText(
                "Text of received message follows:");
              message.addElement("p").addElement("pre")
                .addElement("blockquote").addText(bufferAsString);
            }
            else {
              message.addElement("p").addText(
                "Client opened connection but sent no bytes.");
            }

            sendHTTPErrorResponse(message, "400 Bad Request",
                                  localSocket.getOutputStream());

            localSocket.close();

            break;
          }

          final Matcher httpConnectMatcher =
              m_httpConnectPattern.matcher(bufferAsString);

          final Matcher httpsConnectMatcher =
            m_httpsConnectPattern.matcher(bufferAsString);

          if (httpConnectMatcher.find()) {
            // HTTP proxy request.

            // Reset stream to beginning of request.
            in.reset();

            new StreamThread(
              new HTTPProxyStreamDemultiplexer(
                in, localSocket, EndPoint.clientEndPoint(localSocket)),
              "HTTPProxyStreamDemultiplexer for " + localSocket,
              in).start();

            break;
          }
          else if (httpsConnectMatcher.find()) {
            // HTTPS proxy request.

            // group(2) must be a port number by specification.
            final EndPoint remoteEndPoint =
              new EndPoint(httpsConnectMatcher.group(1),
                           Integer.parseInt(httpsConnectMatcher.group(2)));

            final OutputStream out = localSocket.getOutputStream();

            byte[] proxyResponse = null;

            if (m_httpsProxySocketFactory != null) {
              // Set up our chained proxy connection.
              in.reset();
              proxyResponse = m_httpsProxySocketFactory.negotiate(in, out);
            }

            final Socket sslProxySocket;
            synchronized (m_proxySSLEngine) {
              // Set our proxy engine up to create connections to the
              // remoteEndPoint.
              m_proxySSLEngine.setConnectionDetails(
                EndPoint.clientEndPoint(localSocket), remoteEndPoint);

              // Create a new proxy connection to the proxy engine.
              sslProxySocket =
                getSocketFactory().createClientSocket(
                  m_proxySSLEngine.getListenEndPoint());
            }

            // Set up a couple of threads to punt everything we receive
            // over localSocket to sslProxySocket, and vice versa.
            new StreamThread(
              new StreamCopier(4096, true)
                .getInterruptibleRunnable(in, sslProxySocket.getOutputStream()),
              "Copy to proxy engine for " + remoteEndPoint,
              in).start();

            new StreamThread(
              new StreamCopier(4096, true)
                .getInterruptibleRunnable(sslProxySocket.getInputStream(), out),
              "Copy from proxy engine for " + remoteEndPoint,
              sslProxySocket.getInputStream()).start();

            if (proxyResponse != null) {
              // Chuck the chained proxy's final response back as our own.
              out.write(proxyResponse);
              out.flush();
            }
            else {
              // Send a 200 response to send to client. Client
              // will now start sending SSL data to localSocket.
              final StringBuffer response = new StringBuffer();
              response.append("HTTP/1.0 200 OK\r\n");
              response.append("Proxy-agent: The Grinder/");
              response.append(GrinderBuild.getVersionString());
              response.append("\r\n");
              response.append("\r\n");

              out.write(response.toString().getBytes());
              out.flush();
            }

            break;
          }

          if (bytesRead == buffer.length) {
            while (in.available() > 0) {
              // Drain.
              in.read(buffer);
            }

            final HTMLElement message = new HTMLElement();
            message.addElement("p").addText(
              "Buffer overflow - failed to match HTTP message after " +
              buffer.length + " bytes");

            sendHTTPErrorResponse(message, "400 Bad Request",
              localSocket.getOutputStream());

            break;
          }
        }
      }
      catch (IOException e) {
        UncheckedInterruptedException.ioException(e);
        logIOException(e);

        try {
          localSocket.close();
        }
        catch (IOException closeException) {
          throw new AssertionError(closeException);
        }
      }
    }
  }

  /**
   * Override to also stop our proxy SSL engine.
   */
  public void stop() {
    super.stop();
    m_proxySSLEngine.stop();

    try {
      m_proxySSLEngineThread.join();
    }
    catch (InterruptedException e) {
      throw new UncheckedInterruptedException(e);
    }
  }

  private void sendHTTPErrorResponse(HTMLElement message, String status,
                                     OutputStream outputStream)
    throws IOException {
    getLogger().error(message.toText());

    final HTTPResponse response = new HTTPResponse();
    response.setStatus(status);
    response.setMessage(status, message);

    outputStream.write(response.toString().getBytes("US-ASCII"));
  }

  private void sleep(int milliseconds) {
    try {
      Thread.sleep(milliseconds);
    }
    catch (InterruptedException e) {
      throw new UncheckedInterruptedException(e);
    }
  }

  /**
   * Runnable that actively reads from an Input stream, greps every
   * outgoing packet, and directs appropriately. This is necessary to
   * support HTTP/1.1 between the browser and TCPProxy.
   */
  private final class HTTPProxyStreamDemultiplexer
    implements InterruptibleRunnable {

    private final InputStream m_in;
    private final Socket m_localSocket;
    private final EndPoint m_clientEndPoint;
    private final Map<String, OutputStreamFilterTee> m_remoteStreamMap =
      new HashMap<String, OutputStreamFilterTee>();
    private OutputStreamFilterTee m_lastRemoteStream;

    HTTPProxyStreamDemultiplexer(InputStream in, Socket localSocket,
                                 EndPoint clientEndPoint) {
      m_in = in;
      m_localSocket = localSocket;
      m_clientEndPoint = clientEndPoint;
    }

    public void interruptibleRun() {

      // Needs to hold the largest reasonable set of HTTP headers - see
      // comment in HTTPProxyTCPProxyEngine.run().
      final byte[] buffer = new byte[40960];

      try {
        while (true) {
          // Read a buffer full. We're not as robust as we should be here. We
          // rely on the World conspiring to place request at start of buffer,
          // the request headers fitting in our buffer, and the request headers
          // not being fragmented.
          final int bytesRead = m_in.read(buffer);

          if (bytesRead == -1) {
            break;
          }

          final String bytesReadAsString =
            new String(buffer, 0, bytesRead, "US-ASCII");

          final Matcher matcher =
            m_httpConnectPattern.matcher(bytesReadAsString);

          if (matcher.find()) {

            final String remoteHost = matcher.group(2);

            int remotePort = 80;

            try {
              remotePort = Integer.parseInt(matcher.group(3));
            }
            catch (NumberFormatException e) {
              // remotePort = 80;
            }

            final EndPoint remoteEndPoint =
              new EndPoint(remoteHost, remotePort);

            final String key = remoteEndPoint.toString();

            m_lastRemoteStream = m_remoteStreamMap.get(key);

            if (m_lastRemoteStream == null) {

              // New connection.

              final Socket remoteSocket;
              final TCPProxyFilter requestFilter;

              if (m_chainedHTTPProxy != null) {
                // When running through a chained HTTP proxy, we still
                // create a new thread pair to handle each target
                // server. This allows us to reuse
                // FilteredStreamThread and OutputStreamFilterTee to
                // log the correct connection details. It may also be
                // beneficial for performance.
                remoteSocket =
                  getSocketFactory().createClientSocket(m_chainedHTTPProxy);

                requestFilter =
                  new HTTPMethodAbsoluteURIFilterDecorator(
                    new HTTPMethodRelativeURIFilterDecorator(
                      getRequestFilter()), remoteEndPoint);
              }
              else {
                remoteSocket =
                  getSocketFactory().createClientSocket(remoteEndPoint);

                requestFilter =
                  new HTTPMethodRelativeURIFilterDecorator(getRequestFilter());
              }

              final ConnectionDetails connectionDetails =
                new ConnectionDetails(m_clientEndPoint, remoteEndPoint, false);

              m_lastRemoteStream =
                new OutputStreamFilterTee(connectionDetails,
                                          remoteSocket.getOutputStream(),
                                          requestFilter,
                                          getRequestColour());

              m_lastRemoteStream.connectionOpened();

              m_remoteStreamMap.put(key, m_lastRemoteStream);

              // Spawn a thread to handle everything coming back from
              // the remote server.
              new FilteredStreamThread(
                remoteSocket.getInputStream(),
                new OutputStreamFilterTee(connectionDetails.getOtherEnd(),
                                          m_localSocket.getOutputStream(),
                                          getResponseFilter(),
                                          getResponseColour()));
            }
          }
          else if (m_lastRemoteStream == null) {
            throw new AssertionError("No last stream");
          }

          // Should do filtering etc.
          m_lastRemoteStream.handle(buffer, bytesRead);
        }
      }
      catch (IOException e) {
        // Perhaps we should decorate the OutputStreamFilterTee's so
        // that we can return exceptions as some simple HTTP error
        // page?
        UncheckedInterruptedException.ioException(e);
        final String description = logIOException(e);

        final HTMLElement message = new HTMLElement();
        message.addElement("p").addText(description);

        try {
          // Should probably return other types of status code.
          sendHTTPErrorResponse(
            message, "502 Bad Gateway", m_localSocket.getOutputStream());
        }
        catch (IOException e2) {
          // Ignore.
          UncheckedInterruptedException.ioException(e2);
        }
      }
      finally {
        // When exiting, close all our outgoing streams. This will
        // force all the FilteredStreamThreads we've launched to
        // handle the paired streams to shut down.
        for (OutputStreamFilterTee s : m_remoteStreamMap.values()) {
          s.connectionClosed();
        }

        // We may not have any FilteredStreamThreads, so ensure the
        // local socket is closed. The local socket is shutdown on any
        // error, any browser using us will open up a new connection
        // for new work.
        try {
          m_localSocket.close();
        }
        catch (IOException e) {
          // Ignore.
          UncheckedInterruptedException.ioException(e);
        }
      }
    }
  }

  private static final class ProxySSLEngine extends AbstractTCPProxyEngine {

    private EndPoint m_clientEndPoint;
    private EndPoint m_remoteEndPoint;

    ProxySSLEngine(TCPProxySocketFactory socketFactory,
                   TCPProxyFilter requestFilter,
                   TCPProxyFilter responseFilter,
                   Logger logger,
                   boolean useColour)
    throws IOException {
      super(socketFactory, requestFilter, responseFilter, logger,
            new EndPoint(InetAddress.getByName(null), 0), useColour, 0);
    }

    public void run() {

      while (true) {
        final Socket localSocket;

        try {
          localSocket = accept();
        }
        catch (IOException e) {
          UncheckedInterruptedException.ioException(e);

          if (isStopped()) {
            break;
          }

          logIOException(e);

          continue;
        }

        try {
          launchThreadPair(
            localSocket, m_remoteEndPoint, m_clientEndPoint, true);
        }
        catch (IOException e) {
          UncheckedInterruptedException.ioException(e);

          if (isStopped()) {
            break;
          }

          logIOException(e);

          try {
            localSocket.close();
          }
          catch (IOException closeException) {
            throw new AssertionError(closeException);
          }
        }
      }
    }

    /**
     * Set the ProxySSLEngine up so that the next connection will be
     * wired through to <code>remoteHost:remotePort</code>.
     */
    public void setConnectionDetails(EndPoint clientEndPoint,
                                     EndPoint remoteEndPoint) {
      m_clientEndPoint = clientEndPoint;
      m_remoteEndPoint = remoteEndPoint;
    }
  }

  /**
   * SocketFactory decorator that sets up HTTPS proxy connections on
   * client sockets it returns.
   */
  private final class HTTPSProxySocketFactory
    implements TCPProxySocketFactory {

    private final TCPProxySSLSocketFactory m_delegate;
    private final EndPoint m_httpsProxy;

    // Guarded by this.
    private Socket m_nextRawClientSocket;

    /**
     * Constructor.
     *
     * @param delegate Socket factory to decorate.
     * @param chainedHTTPSProxy HTTPS proxy to direct connections
     * through.
     */
    public HTTPSProxySocketFactory(TCPProxySSLSocketFactory delegate,
                                   EndPoint chainedHTTPSProxy) {
      m_delegate = delegate;
      m_httpsProxy = chainedHTTPSProxy;
    }

    /**
     * Factory method for server sockets. We do nothing special here.
     *
     * @param localEndPoint Local host and port.
     * @param timeout Socket timeout.
     * @return A new <code>ServerSocket</code>.
     * @exception IOException If an error occurs.
     */
    public ServerSocket createServerSocket(EndPoint localEndPoint, int timeout)
      throws IOException {
      return m_delegate.createServerSocket(localEndPoint, timeout);
    }

    /**
     * Establish a connection to the remote proxy and pass requests and
     * responses between the browser and the remote proxy until the proxy
     * returns a 200 or closes the connection. This should handle multistage
     * proxy authentication protocols such as NTLM and Negotiate.
     *
     * <p>
     * If negotiation was successful, the client socket is cached and will be
     * returned for the next call to {@link #createClientSocket(EndPoint)}, and
     * this method will return the final 200 response from the proxy. The final
     * 200 response is not sent to the browser by this method; instead, the
     * engine sets up the SSL engine and does so itself.
     * </p>
     *
     * <p>
     * If the negotiation was unsuccessful, one of the parties will close
     * a connection and we'll throw an {@link IOException}
     * </p>
     *
     * @param request
     *          Stream from the browser.
     * @param response
     *          Stream to the browser.
     * @return The final 200 response from the remote proxy, or {@code null} if
     *         authentication failed.
     * @exception IOException If an error occurs.
     */
    public byte[] negotiate(InputStream request, OutputStream response)
      throws IOException {

      synchronized (this) {
        m_nextRawClientSocket = null;
      }

      final Socket socket;

      try {
        socket = new Socket(m_httpsProxy.getHost(), m_httpsProxy.getPort());
      }
      catch (ConnectException e) {
        throw new VerboseConnectException(e, "HTTPS proxy " + m_httpsProxy);
      }

      while (true) {
        // Use a buffered output stream so the output is flushed as one.
        final OutputStream proxyRequest =
          new BufferedOutputStream(socket.getOutputStream());

        final byte[] buffer = new byte[1024];

        final InputStream proxyResponse = socket.getInputStream();

        // Non-blocking read.
        while (request.available() > 0) {
          final int n = request.read(buffer);

          if (n > 0) {
            proxyRequest.write(buffer, 0, n);
          }
        }

        proxyRequest.flush();

        // Wait for response.

        for (int i = 0;
             i < s_connectTimeout && proxyResponse.available() == 0;
             i += 10) {
          sleep(10);
        }

        if (proxyResponse.available() == 0) {
          throw new IOException(
            "HTTPS proxy " + m_httpsProxy + " failed to respond after " +
            s_connectTimeout + " ms");
        }

        // We've got a live one. Parse its response.
        final ByteArrayOutputStream responseBytes = new ByteArrayOutputStream();

        // Non-blocking read.
        while (proxyResponse.available() > 0) {
          final int n = proxyResponse.read(buffer);

          if (n > 0) {
            responseBytes.write(buffer, 0, n);
          }
        }

        final byte[] bytesRead = responseBytes.toByteArray();

        final String bufferAsString = new String(bytesRead, "US-ASCII");

        final Matcher statusCodeMatcher =
          m_httpsProxyResponsePattern.matcher(bufferAsString);

        if (statusCodeMatcher.find()) {
          final String statusCode = statusCodeMatcher.group(1);

          if (statusCode.equals("200")) {
            synchronized (this) {
              m_nextRawClientSocket = socket;
            }

            return bytesRead;
          }
        }

        // Not a 200, flush directly back to the browser.
        response.write(bytesRead);
        response.flush();

        // Wait for request.

        for (int i = 0;
             i < s_connectTimeout && request.available() == 0;
             i += 10) {
          sleep(10);
        }

        if (request.available() == 0) {
          throw new IOException(
            "Timed out waiting for browser after " +
            s_connectTimeout + " ms");
        }
      }
    }

    /**
     * Factory method for client sockets.
     *
     * @param remoteEndPoint Remote host and port.
     * @return A new <code>Socket</code>.
     * @exception IOException If an error occurs.
     */
    public Socket createClientSocket(EndPoint remoteEndPoint)
      throws IOException {

      try {
        return m_delegate.createClientSocket(m_nextRawClientSocket,
                                             remoteEndPoint);
      }
      finally {
        m_nextRawClientSocket = null;
      }
    }
  }
}
