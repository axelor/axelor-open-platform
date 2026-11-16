/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.rpc.filter;

import com.axelor.app.AppSettings;
import com.axelor.app.AvailableAppSettings;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.regex.Pattern;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.Token;
import org.hibernate.grammars.hql.HqlLexer;

/**
 * Validates a domain (a JPQL where-clause a client or admin may have written) before it is joined,
 * as text, with other filters such as the row-level permission filter.
 *
 * <p>Two checks: an optional, app-configured keyword blocklist ({@code
 * application.domain-blocklist-pattern}), and a structural check that the domain is self-contained,
 * so it cannot escape the parentheses it is wrapped in and change how it combines with what
 * follows.
 */
public final class DomainValidator {

  private static final Pattern BLOCKLIST_PATTERN =
      AppSettings.get().get(AvailableAppSettings.APPLICATION_DOMAIN_BLOCKLIST_PATTERN, null) == null
          ? null
          : Pattern.compile(
              AppSettings.get()
                  .get(AvailableAppSettings.APPLICATION_DOMAIN_BLOCKLIST_PATTERN)
                  .trim(),
              Pattern.CASE_INSENSITIVE);

  private static final BaseErrorListener REJECT_ON_ERROR =
      new BaseErrorListener() {
        @Override
        public void syntaxError(
            Recognizer<?, ?> recognizer,
            Object offendingSymbol,
            int line,
            int charPositionInLine,
            String msg,
            RecognitionException e) {
          throw new IllegalArgumentException("Invalid domain, " + msg);
        }
      };

  private DomainValidator() {}

  /**
   * Validates a domain, throwing {@link IllegalArgumentException} if it uses blocklisted keywords
   * or is not self-contained. A {@code null} domain is accepted (no-op).
   */
  public static void validate(String domain) {
    if (domain == null) {
      return;
    }
    checkBlocklist(domain);
    checkSelfContained(domain);
  }

  private static void checkBlocklist(String domain) {
    if (BLOCKLIST_PATTERN != null && BLOCKLIST_PATTERN.matcher(domain).find()) {
      throw new IllegalArgumentException("Invalid domain, filter uses blacklisted keywords.");
    }
  }

  /**
   * Checks that a JPQL filter expression is self-contained, so that when it is concatenated with
   * other text it cannot alter or escape its surroundings. It rejects:
   *
   * <ul>
   *   <li>brackets that don't pair up or nest — a closing bracket with no matching opening one, or
   *       an opening bracket left unclosed;
   *   <li>a string literal, quoted identifier or comment left open, which would otherwise swallow
   *       whatever text follows the expression;
   *   <li>a stray <code>*&#47;</code>, which would close a comment opened before the expression;
   *   <li>a string literal or quoted identifier with a backslash before its closing quote, which
   *       closes cleanly on its own, but when followed by more text is read as an escaped quote so
   *       the literal runs past it;
   *   <li>a <code>j"..."</code> or <code>J"..."</code> literal, which the lexer does not always
   *       close at the next double quote when more text follows.
   * </ul>
   *
   * <p>Tokenizes with Hibernate's own HQL lexer, so a bracket inside a string literal, quoted
   * identifier or comment is skipped exactly as the query parser skips it.
   */
  private static void checkSelfContained(String domain) {
    var lexer = new HqlLexer(CharStreams.fromString(domain));
    lexer.removeErrorListeners();
    lexer.addErrorListener(REJECT_ON_ERROR);

    Deque<Integer> open = new ArrayDeque<>();
    Token previous = null;
    for (var token = lexer.nextToken(); token.getType() != Token.EOF; token = lexer.nextToken()) {
      if (isCommentDelimiter(previous, token)) {
        throw unbalanced();
      }
      previous = token;
      switch (token.getType()) {
        // Reject trailing backslash before closing quotes and j"..."/J"..." literals
        case HqlLexer.JAVA_STRING_LITERAL, HqlLexer.QUOTED_IDENTIFIER -> {
          String text = token.getText();
          if (text.charAt(text.length() - 2) == '\\'
              || text.startsWith("j\"")
              || text.startsWith("J\"")) {
            throw unbalanced();
          }
        }
        case HqlLexer.LEFT_PAREN, HqlLexer.LEFT_BRACKET, HqlLexer.LEFT_BRACE ->
            open.push(token.getType());
        // JDBC escape literals open with '{ts', '{d' or '{t' tokens and close with '}'
        case HqlLexer.TIMESTAMP_ESCAPE_START,
            HqlLexer.DATE_ESCAPE_START,
            HqlLexer.TIME_ESCAPE_START ->
            open.push(HqlLexer.LEFT_BRACE);
        case HqlLexer.RIGHT_PAREN -> close(open, HqlLexer.LEFT_PAREN);
        case HqlLexer.RIGHT_BRACKET -> close(open, HqlLexer.LEFT_BRACKET);
        case HqlLexer.RIGHT_BRACE -> close(open, HqlLexer.LEFT_BRACE);
        default -> {}
      }
    }
    if (!open.isEmpty()) {
      throw unbalanced();
    }
  }

  /**
   * Returns whether two adjacent tokens form <code>/*</code> or <code>*&#47;</code> with no
   * characters between them.
   *
   * <p>HqlLexer skips complete comments, so any remaining adjacent <code>/</code> and <code>*
   * </code> tokens are stray comment delimiters.
   */
  private static boolean isCommentDelimiter(Token previous, Token token) {
    if (previous == null || token.getStartIndex() != previous.getStopIndex() + 1) {
      return false;
    }
    int first = previous.getType();
    int second = token.getType();
    return (first == HqlLexer.SLASH && second == HqlLexer.ASTERISK)
        || (first == HqlLexer.ASTERISK && second == HqlLexer.SLASH);
  }

  private static void close(Deque<Integer> open, int expected) {
    if (open.isEmpty() || open.pop() != expected) {
      throw unbalanced();
    }
  }

  private static IllegalArgumentException unbalanced() {
    return new IllegalArgumentException("Invalid domain, unbalanced delimiters.");
  }
}
