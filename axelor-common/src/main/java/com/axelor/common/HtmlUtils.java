/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2021 Axelor (<http://axelor.com>).
 *
 * This program is free software: you can redistribute it and/or  modify
 * it under the terms of the GNU Affero General Public License, version 3,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.axelor.common;

/** This class provides some helper methods escaping, rendering, replacing HTML text */
public class HtmlUtils {

  /**
   * Escapes the text. It is safe to use in an HTML context.
   *
   * @param text the text to escape
   * @return the escaped HTML text, or <code>null</code> if the text is <code>null</code>
   */
  public static String escape(String text) {
    if (text == null) {
      return null;
    }

    if (text.length() == 0) {
      return "";
    }

    StringBuilder sb = null;

    int lastReplacementIndex = 0;

    for (int i = 0; i < text.length(); i++) {
      char c = text.charAt(i);

      if ((c < 256) && ((c >= 128) || _VALID_CHARS[c])) {
        continue;
      }

      String replacement = null;

      if (c == '<') {
        replacement = "&lt;";
      } else if (c == '>') {
        replacement = "&gt;";
      } else if (c == '&') {
        replacement = "&amp;";
      } else if (c == '"') {
        replacement = "&#34;";
      } else if (c == '\'') {
        replacement = "&#39;";
      } else if (c == '\u00bb') {
        replacement = "&#187;";
      } else if (c == '\u2013') {
        replacement = "&#8211;";
      } else if (c == '\u2014') {
        replacement = "&#8212;";
      } else if (c == '\u2028') {
        replacement = "&#8232;";
      } else if (!_isValidXmlCharacter(c) || _isUnicodeCompatibilityCharacter(c)) {

        replacement = " ";
      } else {
        continue;
      }

      if (sb == null) {
        sb = new StringBuilder();
      }

      if (i > lastReplacementIndex) {
        sb.append(text.substring(lastReplacementIndex, i));
      }

      sb.append(replacement);

      lastReplacementIndex = i + 1;
    }

    if (sb == null) {
      return text;
    }

    if (lastReplacementIndex < text.length()) {
      sb.append(text.substring(lastReplacementIndex));
    }

    return sb.toString();
  }

  /**
   * Escapes the attribute value. It is safe to be use in an attribute value.
   *
   * @param attribute the attribute to escape
   * @return the escaped attribute value, or <code>null</code> if the attribute value is <code>null
   *     </code>
   */
  public static String escapeAttribute(String attribute) {
    if (attribute == null) {
      return null;
    }

    if (attribute.length() == 0) {
      return "";
    }

    StringBuilder sb = null;
    int lastReplacementIndex = 0;

    for (int i = 0; i < attribute.length(); i++) {
      char c = attribute.charAt(i);

      if (c < _ATTRIBUTE_ESCAPES.length) {
        String replacement = _ATTRIBUTE_ESCAPES[c];

        if (replacement == null) {
          continue;
        }

        if (sb == null) {
          sb = new StringBuilder(attribute.length() + 64);
        }

        if (i > lastReplacementIndex) {
          sb.append(attribute, lastReplacementIndex, i);
        }

        sb.append(replacement);

        lastReplacementIndex = i + 1;
      } else if (!_isValidXmlCharacter(c) || _isUnicodeCompatibilityCharacter(c)) {

        if (sb == null) {
          sb = new StringBuilder(attribute.length() + 64);
        }

        if (i > lastReplacementIndex) {
          sb.append(attribute, lastReplacementIndex, i);
        }

        sb.append(Chars.SPACE);

        lastReplacementIndex = i + 1;
      }
    }

    if (sb == null) {
      return attribute;
    }

    if (lastReplacementIndex < attribute.length()) {
      sb.append(attribute, lastReplacementIndex, attribute.length());
    }

    return sb.toString();
  }

  private static boolean _isValidXmlCharacter(char c) {
    if (((c >= Chars.SPACE) && (c <= '\ud7ff'))
        || ((c >= '\ue000') && (c <= '\ufffd'))
        || Character.isSurrogate(c)
        || (c == Chars.TAB)
        || (c == Chars.NEW_LINE)
        || (c == Chars.RETURN)) {

      return true;
    }

    return false;
  }

  private static boolean _isUnicodeCompatibilityCharacter(char c) {
    if (((c >= '\u007f') && (c <= '\u0084'))
        || ((c >= '\u0086') && (c <= '\u009f'))
        || ((c >= '\ufdd0') && (c <= '\ufdef'))) {

      return true;
    }

    return false;
  }

  private static final String[] _ATTRIBUTE_ESCAPES = new String[256];

  private static final boolean[] _VALID_CHARS = new boolean[256];

  static {
    for (int i = 0; i < 256; i++) {
      char c = (char) i;

      if (!_isValidXmlCharacter(c)) {
        _ATTRIBUTE_ESCAPES[i] = " ";
      }

      _ATTRIBUTE_ESCAPES[Chars.AMPERSAND] = "&amp;";
      _ATTRIBUTE_ESCAPES[Chars.APOSTROPHE] = "&#39;";
      _ATTRIBUTE_ESCAPES[Chars.GREATER_THAN] = "&gt;";
      _ATTRIBUTE_ESCAPES[Chars.LESS_THAN] = "&lt;";
      _ATTRIBUTE_ESCAPES[Chars.QUOTE] = "&quot;";

      if (Character.isLetterOrDigit(c)) {
        _VALID_CHARS[i] = true;
      }
    }

    _VALID_CHARS['-'] = true;
    _VALID_CHARS['_'] = true;
  }

  static class Chars {
    public static final char AMPERSAND = '&';

    public static final char APOSTROPHE = '\'';

    public static final char DELETE = '\u007f';

    public static final char GREATER_THAN = '>';

    public static final char LESS_THAN = '<';

    public static final char SPACE = ' ';

    public static final char QUOTE = '\"';

    public static final char RETURN = '\r';

    public static final char NEW_LINE = '\n';

    public static final char TAB = '\t';
  }
}
