// Copyright (C) 2000 Paco Gomez
// Copyright (C) 2000 - 2009 Philip Aston
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

import java.io.PrintWriter;

import net.grinder.common.GrinderException;
import net.grinder.common.Logger;
import net.grinder.console.ConsoleFoundation;
import net.grinder.console.common.Resources;
import net.grinder.console.common.ResourcesImplementation;
import net.grinder.console.swingui.ConsoleUI;
import net.grinder.console.textui.TextUI;
import net.grinder.util.AbstractMainClass;
import net.grinder.util.SimpleLogger;


/**
 * This is the entry point of The Grinder console.
 *
 * @author Philip Aston
 * @version $Revision: 4003 $
 */
public final class Console extends AbstractMainClass {

  private static final String USAGE =
    "\n  java " + TCPProxy.class + " [-headless]" +
    "\n" +
    "\n  -headless                    Don't use a graphical user interface.";

  private final ConsoleFoundation m_consoleFoundation;

  private Console(String[] args, Resources resources, Logger logger)
    throws GrinderException {

    super(logger, USAGE);

    Class<?> ui = ConsoleUI.class;

    for (int i = 0; i < args.length; i++) {
      if (args[i].equalsIgnoreCase("-headless")) {
        ui = TextUI.class;
      }
      else {
        throw barfUsage();
      }
    }

    m_consoleFoundation = new ConsoleFoundation(resources, logger);
    m_consoleFoundation.createUI(ui);
  }

  private void run() {
    m_consoleFoundation.run();
  }

  /**
   * Entry point.
   *
   * @param args Command line arguments.
   */
  public static void main(String[] args)  {
    final Resources resources = new ResourcesImplementation(
      "net.grinder.console.common.resources.Console");

    final Logger logger =
      new SimpleLogger(resources.getString("shortTitle"),
                       new PrintWriter(System.out),
                       new PrintWriter(System.err));

    try {
      final Console console = new Console(args, resources, logger);
      console.run();
    }
    catch (LoggedInitialisationException e) {
      System.exit(1);
    }
    catch (GrinderException e) {
      logger.error("Could not initialise:");
      final PrintWriter errorWriter = logger.getErrorLogWriter();
      e.printStackTrace(errorWriter);
      errorWriter.flush();
      System.exit(2);
    }

    System.exit(0);
  }
}
