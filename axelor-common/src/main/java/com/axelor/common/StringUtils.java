/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2024 Axelor (<http://axelor.com>).
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.axelor.common;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** This class provides static helper methods for {@link CharSequence}. */
public final class StringUtils {

  /**
   * Check whether the given string value is empty. The value is empty if null or length is 0.
   *
   * @param value the string value to test
   * @return true if empty false otherwise
   */
  public static boolean isEmpty(CharSequence value) {
    return value == null || value.length() == 0;
  }

  /**
   * Check whether the given string value is not empty. The value is empty if null or length is 0.
   *
   * @param value the string value to test
   * @return true if not empty false otherwise
   */
  public static boolean notEmpty(CharSequence value) {
    return !isEmpty(value);
  }

  /**
   * Check whether the given string value is blank. The value is blank if null or contains white
   * spaces only.
   *
   * @param value the string value to test
   * @return true if empty false otherwise
   */
  public static boolean isBlank(CharSequence value) {
    if (isEmpty(value)) {
      return true;
    }
    for (int i = 0; i < value.length(); i++) {
      if (!Character.isWhitespace(value.charAt(i))) {
        return false;
      }
    }
    return true;
  }

  /**
   * Check whether the given string value is not blank. The value is blank if null or contains white
   * spaces only.
   *
   * @param value the string value to test
   * @return true if not empty false otherwise
   */
  public static boolean notBlank(CharSequence value) {
    return !isBlank(value);
  }

  /**
   * Remove diacritics (accents) from a {@link CharSequence}.
   *
   * @param value the string to be stripped
   * @return text with diacritics removed
   */
  public static String stripAccent(CharSequence value) {
    if (value == null) {
      return null;
    }
    if (isEmpty(value)) {
      return value.toString();
    }
    return Normalizer.normalize(value, Normalizer.Form.NFD)
        .replace('\u0141', 'L')
        .replace('\u0142', 'l')
        .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
  }

  /**
   * Strip the leading indentation from the given text.
   *
   * <p>The line with the least number of leading spaces determines the number to remove. Lines only
   * containing whitespace are ignored when calculating the number of leading spaces to strip.
   *
   * @param text the text to strip indent from
   * @return stripped text
   */
  public static String stripIndent(CharSequence text) {
    if (text == null) {
      return null;
    }
    if (isBlank(text)) {
      return text.toString();
    }

    final String[] lines = text.toString().split("\\n");
    final StringBuilder builder = new StringBuilder();

    int leading = -1;
    for (String line : lines) {
      if (isBlank(line)) {
        continue;
      }
      int index = 0;
      int length = line.length();
      if (leading == -1) {
        leading = length;
      }
      while (index < length && index < leading && Character.isWhitespace(line.charAt(index))) {
        index++;
      }
      if (leading > index) {
        leading = index;
      }
    }

    for (String line : lines) {
      if (!isBlank(line)) {
        builder.append(leading <= line.length() ? line.substring(leading) : "");
      }
      if (lines.length > 1) {
        builder.append("\n");
      }
    }

    return builder.toString();
  }

  /**
   * Strip leading whitespace/control characters followed by '|' from every line in the given text.
   *
   * @param text the text to strip the margin from
   * @return the stripped String
   */
  public static String stripMargin(CharSequence text) {
    if (text == null) {
      return null;
    }
    return Stream.of(text.toString().split("\n"))
        .map(line -> line.replaceFirst("^\\s+\\|", ""))
        .collect(Collectors.joining("\n"));
  }

  /**
   * Returns <code>true</code> if the text is in the ASCII charset.
   *
   * @param text the text to check
   * @return <code>true</code> if the text is in the ASCII charset; <code>false</code> otherwise
   */
  public static boolean isAscii(CharSequence text) {
    if (text == null) {
      return false;
    }

    boolean ascii = true;

    for (int i = 0; i < text.length(); i++) {
      if (!isAscii(text.charAt(i))) {
        ascii = false;
        break;
      }
    }
    return ascii;
  }

  /**
   * Returns <code>true</code> if the character is in the ASCII charset.
   *
   * @param c the character to check
   * @return <code>true</code> if the character is in the ASCII charset; <code>false</code>
   *     otherwise
   */
  public static boolean isAscii(char c) {
    int i = c;

    if ((i >= 32) && (i <= 126)) {
      return true;
    }

    return false;
  }

  /**
   * Split the given text into an array using the given delimiter.
   *
   * <p>The result parts are trimmed and empty part omitted.
   *
   * @param text the text to slip
   * @param delimiter the delimiter
   * @return array of string
   */
  public static String[] splitToArray(String text, String delimiter) {
    return splitToArray(text, delimiter, true, true);
  }

  /**
   * Split the given text into an array using the given delimiter.
   *
   * @param text the text to slip
   * @param delimiter the delimiter
   * @param trimResult whether to trim the result parts
   * @param ignoreEmpty whether to omit empty part
   * @return array of string
   */
  public static String[] splitToArray(
      String text, String delimiter, boolean trimResult, boolean ignoreEmpty) {
    if (text == null) {
      return new String[0];
    }

    StringTokenizer tokenizer = new StringTokenizer(text, delimiter);
    List<String> parts = new ArrayList<>();
    while (tokenizer.hasMoreTokens()) {
      String token = tokenizer.nextToken();
      if (trimResult) {
        token = token.trim();
      }
      if (!ignoreEmpty || notBlank(token)) {
        parts.add(token);
      }
    }
    return parts.toArray(new String[0]);
  }
}
