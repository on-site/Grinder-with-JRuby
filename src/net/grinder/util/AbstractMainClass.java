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


/**
 * Basic functionality for a main class. Crudely extracted for now.
 *
 * @author Philip Aston
 * @version $Revision: 3995 $
 */
public abstract class AbstractMainClass {

  private final String m_usage;
  private final Logger m_logger;

  /**
   * Constructor.
   *
   * @param logger Logger to which output should be directed.
   * @param usage Usage message.
   * @throws GrinderException If a problem occurred.
   */
  protected AbstractMainClass(Logger logger, String usage)
    throws GrinderException {

    m_usage = usage;
    m_logger = logger;

    if (!JVM.getInstance().haveRequisites(m_logger)) {
      throw new LoggedInitialisationException("Unsupported JVM");
    }
  }

  /**
   * Return our logger.
   *
   * @return The logger.
   */
  protected final Logger getLogger() {
    return m_logger;
  }

  /**
   * Log an error and return a {@link LoggedInitialisationException} that
   * can be thrown.
   *
   * @param message The message to throw.
   * @return An exception for the caller to throw.
   */
  protected final LoggedInitialisationException barfError(String message) {
    m_logger.error("Error: " + message);
    return new LoggedInitialisationException(message);
  }

  /**
   * Log a usage message and return a {@link LoggedInitialisationException} that
   * can be thrown.
   *
   * @return An exception for the caller to throw.
   */
  protected final LoggedInitialisationException barfUsage() {
    return barfError(
      "unrecognised or invalid option." +
      "\n\n" +
      "Usage: " +
      m_usage);
  }

  /**
   * Exception indicating that an error message has already been logged.
   */
  protected static class LoggedInitialisationException
    extends GrinderException {

    /**
     * Constructor.
     *
     * @param message The error message.
     */
    public LoggedInitialisationException(String message) {
      super(message);
    }
  }
}
