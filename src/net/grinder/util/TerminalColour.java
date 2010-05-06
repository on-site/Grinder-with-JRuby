// Copyright (C) 2000 - 2006 Philip Aston
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

/**
 * Creates ANSI colour control strings.
 *
 * @author Philip Aston
 * @version $Revision: 3762 $
 */
public final class TerminalColour {

  private TerminalColour() {
  }

  /**
   * Constant control string for colour.
   */
  public static final String BLACK = controlString("30");

  /**
   * Constant control string for colour.
   */
  public static final String RED = controlString("31");

  /**
   * Constant control string for colour.
   */
  public static final String GREEN = controlString("32");

  /**
   * Constant control string for colour.
   */
  public static final String YELLOW = controlString("33");

  /**
   * Constant control string for colour.
   */
  public static final String BLUE = controlString("34");

  /**
   * Constant control string for colour.
   */
  public static final String MAGENTA = controlString("35");

  /**
   * Constant control string for colour.
   */
  public static final String CYAN = controlString("36");

  /**
   * Constant control string for colour.
   */
  public static final String WHITE = controlString("37");

  /**
   * Constant control string for colour.
   */
  public static final String BLACK_BACKGROUND = controlString("40");

  /**
   * Constant control string for colour.
   */
  public static final String RED_BACKGROUND = controlString("41");

  /**
   * Constant control string for colour.
   */
  public static final String GREEN_BACKGROUND = controlString("42");

  /**
   * Constant control string for colour.
   */
  public static final String YELLOW_BACKGROUND = controlString("43");

  /**
   * Constant control string for colour.
   */
  public static final String BLUE_BACKGROUND = controlString("44");

  /**
   * Constant control string for colour.
   */
  public static final String MAGENTA_BACKGROUND = controlString("45");

  /**
   * Constant control string for colour.
   */
  public static final String CYAN_BACKGROUND = controlString("46");

  /**
   * Constant control string for colour.
   */
  public static final String WHITE_BACKGROUND = controlString("47");

  /**
   * Control string for no formatting.
   *
   * <p>Quoting from Thomas E. Dickey's vttest: "Some terminals will *
   * reset colors with SGR-0; I've added the 39, 49 codes for those *
   * that are ISO compliant. (The black/white codes are for emulators
   * written by people who don't bother reading standards)."</p>
   */
  public static final String NONE = controlString("0;40;37;39;49");

  private static String controlString(String body) {
    return (char)0033 + "[" + body + 'm';
  }
}
