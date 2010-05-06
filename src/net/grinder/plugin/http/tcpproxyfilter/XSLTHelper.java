// Copyright (C) 2005 - 2008 Philip Aston
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

package net.grinder.plugin.http.tcpproxyfilter;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import HTTPClient.Codecs;


/**
 * Helper functions for style sheets.
 *
 * <p>
 * When calling methods that don't have parameters from a style sheet, don't
 * forget the call braces or you'll end up with a no-op.
 * </p>
 *
 * <p>
 * This class has static methods for consistent behaviour between JDK versions.
 * With instance methods the XSLTC implementation in Java 5.0 needs the instance
 * to be passed as the first argument, whereas the Xalan implementation in Java
 * 1.5 does not.
 * </p>
 *
 * @author Philip Aston
 * @version $Revision: 3880 $
 */
public final class XSLTHelper {
  private static final Utils PYTHON_UTILS = new PythonUtils();
  private static final Utils RUBY_UTILS = new RubyUtils();
  private static int s_indentLevel;

  private XSLTHelper() {
  }

  /**
   * Convert an ISO 8601 date/time string to a more friendly, locale specific
   * string.
   *
   * @param iso8601
   *          An extended format ISO 8601 date/time string
   * @return The formated date/time.
   * @throws ParseException
   *           If the date could not be parsed.
   */
  public static String formatTime(String iso8601) throws ParseException {
    final Date date =
      new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").parse(iso8601);
    return DateFormat.getDateTimeInstance().format(date);
  }

  /**
   * Wrap string in appropriate quotes for Python.
   *
   * @param value The string.
   * @return The quoted string.
   */
  public static String quoteForPython(String value) {
    return PYTHON_UTILS.quote(value);
  }

  public static String quoteForRuby(String value) {
    return RUBY_UTILS.quote(value);
  }

  public static String escapeForPython(String value) {
    return PYTHON_UTILS.escape(value);
  }

  public static String escapeForRuby(String value) {
    return RUBY_UTILS.escape(value);
  }

  /**
   * Wrap string in appropriate quotes for Python, passing through existing EOL
   * escapes {"\n", "\r"}, and quoting real new lines.
   *
   * @param value
   *          The string.
   * @return The quoted string.
   * @see net.grinder.util.SimpleStringEscaper
   */
  public static String quoteEOLEscapedStringForPython(String value) {
    return PYTHON_UTILS.quoteEOLEscapedString(value);
  }

  public static String quoteEOLEscapedStringForRuby(String value) {
    return RUBY_UTILS.quoteEOLEscapedString(value);
  }

  /**
   * Transform new line characters and other control characters to a printable
   * representation. Truncate string if longer than
   * <code>maximumCharacters</code>. If the string is truncated,
   * add ellipses.
   *
   * @param value
   *          The input string.
   * @param maximumCharacters
   *          Truncate at this number of characters if result would otherwise be
   *          longer.
   * @return The result.
   */
  public static String summariseAsLine(String value, int maximumCharacters) {

    final StringBuffer result = new StringBuffer(value.length());

    if (value.length() > maximumCharacters) {
      result.append(value.substring(0, maximumCharacters));
      result.append("...");
    }
    else {
      result.append(value);
    }

    for (int i = 0; i < result.length(); ++i) {
      final char c = result.charAt(i);

      if (c == '\t') {
        result.replace(i, i + 1, "\\t");
      }
      else if (c == '\r') {
        result.replace(i, i + 1, "\\r");
      }
      else if (c == '\n') {
        result.replace(i, i + 1, "\\n");
      }
      else if (Character.isISOControl(c)) {
        result.setCharAt(i, '.');
      }
    }

    return result.toString();
  }

  /**
   * Return an appropriately indent string.
   *
   * @return The string.
   * @see #changeIndent
   * @see #resetIndent
   */
  public static String indent() {
    return "                ".substring(0, s_indentLevel * 2);
  }

  /**
   * Return a new line string.
   *
   * @return The string.
   */
  public static String newLine() {
    return "\n";
  }

  /**
   * Equivalent to {@link #newLine()} followed by {@link #indent()}.
   *
   * @return The string.
   */
  public static String newLineAndIndent() {
    return newLine() + indent();
  }

  /**
   * Change the indent level.
   *
   * @param indentChange Offset to indent level, positive or negative.
   * @return An empty string.
   */
  public static String changeIndent(int indentChange) {
    s_indentLevel += indentChange;
    return "";
  }

  /**
   * Reset the indent level.
   *
   * @return An empty string.
   */
  public static String resetIndent() {
    s_indentLevel = 0;
    return "";
  }

  /**
   * Convert a base64 string of binary data to an array of bytes scriptlet.
   *
   *
   * @param base64String The binary data.
   * @return The scriptlet.
   */
  public static String base64ToPython(String base64String) {
    return PYTHON_UTILS.fromBase64(base64String);
  }

  public static String base64ToRuby(String base64String) {
    return RUBY_UTILS.fromBase64(base64String);
  }

  public static abstract class Utils {
    public String quote(String value) {
      if (value == null) {
        return nullValue();
      }

      final StringBuffer result = new StringBuffer();

      final String quotes = quotes(value);

      result.append(quotes).append(escape(value, false)).append(quotes);

      return result.toString();
    }

    public String quoteEOLEscapedString(String value) {
      if (value == null) {
        return nullValue();
      }

      final StringBuffer result = new StringBuffer();

      final String quotes = quotes(value);

      result.append(quotes).append(escape(value, true)).append(quotes);

      return result.toString();
    }

    public static String fromBase64(String base64String) {
      final byte[] base64 = base64String.getBytes();

      final StringBuffer result = new StringBuffer(base64.length * 2);

      result.append('"');

      if (base64.length > 0) {
        final byte[] bytes = Codecs.base64Decode(base64);

        for (int i = 0; i < bytes.length; ++i) {
          if (i > 0 && i % 16 == 0) {
            result.append('"');
            result.append(newLineAndIndent());
            result.append('"');
          }

          final int b = bytes[i] < 0 ? 0x100 + bytes[i] : bytes[i];

          if (b <= 0xF) {
            result.append("\\x0");
          }
          else {
            result.append("\\x");
          }

          result.append(Integer.toHexString(b).toUpperCase());
        }
      }

      result.append('"');

      return result.toString();
    }

    public abstract String nullValue();
    public abstract String quotes(String s);
    public abstract String escape(String value);
    public abstract String escape(String value, boolean preserveEOLQuotes);
  }

  public static class PythonUtils extends Utils {
    public String nullValue() {
      return "None";
    }

    /**
     * Return appropriate Python quotes for the string based on whether
     * it contains any line separates.
     *
     * @param s The string to quote.
     * @return The quotes.
     */
    public String quotes(String s) {
      return s.indexOf("\n") > -1 || s.indexOf("\r") > -1 ? "'''" : "'";
    }

    /**
     * Escape quotes and back slashes for Python. One day, this might escape
     * white space and non-printable characters too.
     *
     * @param value The string.
     * @return The escaped string.
     */
    public String escape(String value) {
      return escape(value, false);
    }

    /**
     * Escape quotes and back slashes for Python.
     *
     * @param value
     *            The string.
     * @param preserveEOLQuotes
     *            <code>true</code> => existing \n and \r quotes should be
     *            preserved, and literal \n, \r should be removed. (This is for
     *            strings that have been pre-escaped with
     *            {@link net.grinder.util.SimpleStringEscaper}).
     * @return The escaped string.
     */
    public String escape(String value, boolean preserveEOLQuotes) {
      final int valueLength = value.length();

      final StringBuffer result = new StringBuffer(valueLength);

      for (int i = 0; i < valueLength; ++i) {
        final char c = value.charAt(i);

        switch (c) {
        case '\\':
          if (preserveEOLQuotes && i + 1 < valueLength) {
            final char nextCharacter = value.charAt(i + 1);

            if (nextCharacter == 'n' ||
                nextCharacter == 'r') {
              result.append(c);
              break;
            }
          }

          result.append('\\');
          result.append(c);
          break;

        case '\'':
        case '"':
          result.append('\\');
          result.append(c);
          break;

        case '\n':
          if (!preserveEOLQuotes) {
            result.append(c);
          }

          break;

        case '\r':
          if (!preserveEOLQuotes) {
            // We quote line feeds since the Jython parser translates them to
            // carriage returns (or perhaps the platform line ending?).
            result.append("\\r");
          }
          break;

        default:
          result.append(c);
          break;
        }
      }

      return result.toString();
    }
  }

  public static class RubyUtils extends Utils {
    public String nullValue() {
      return "nil";
    }

    public String quotes(String s) {
      return "\"";
    }

    public String escape(String value) {
      return escape(value, false);
    }

    public String escape(String value, boolean preserveEOLQuotes) {
      final int valueLength = value.length();

      final StringBuffer result = new StringBuffer(valueLength);

      for (int i = 0; i < valueLength; ++i) {
        final char c = value.charAt(i);

        switch (c) {
        case '\\':
          if (preserveEOLQuotes && i + 1 < valueLength) {
            final char nextCharacter = value.charAt(i + 1);

            if (nextCharacter == 'n' ||
                nextCharacter == 'r') {
              result.append(c);
              break;
            }
          }

          result.append('\\');
          result.append(c);
          break;

        case '\'':
        case '"':
        case '#':
          result.append('\\');
          result.append(c);
          break;

        case '\n':
        case '\r':
          if (!preserveEOLQuotes) {
            result.append(c);
          }

          break;

        default:
          result.append(c);
          break;
        }
      }

      return result.toString();
    }
  }
}
