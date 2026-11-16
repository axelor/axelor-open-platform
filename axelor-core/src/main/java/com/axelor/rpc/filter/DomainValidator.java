/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2026 Axelor (<http://axelor.com>).
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
package com.axelor.rpc.filter;

import antlr.Token;
import antlr.TokenStreamException;
import com.axelor.app.AppSettings;
import com.axelor.app.AvailableAppSettings;
import java.io.StringReader;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.regex.Pattern;
import org.hibernate.hql.internal.antlr.HqlBaseLexer;
import org.hibernate.hql.internal.antlr.HqlTokenTypes;

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
   *   <li>a string literal left open, which would otherwise swallow whatever text follows the
   *       expression, and any character the HQL lexer does not accept;
   *   <li>a string literal with a backslash before a quote. Hibernate copies string literals as
   *       they are into the SQL, where a database that treats backslash as an escape character
   *       (MySQL by default) reads it as an escaped quote, so the literal ends elsewhere than where
   *       Hibernate ended it.
   * </ul>
   *
   * <p>Tokenizes with Hibernate's own HQL lexer, so a bracket inside a string literal is skipped
   * exactly as the query parser skips it.
   */
  private static void checkSelfContained(String domain) {
    final HqlBaseLexer lexer = new HqlBaseLexer(new StringReader(domain));
    final Deque<Integer> open = new ArrayDeque<>();
    try {
      for (Token token = lexer.nextToken();
          token.getType() != Token.EOF_TYPE;
          token = lexer.nextToken()) {
        switch (token.getType()) {
          case HqlTokenTypes.QUOTED_STRING:
            if (token.getText().contains("\\'")) {
              throw unbalanced();
            }
            break;
          case HqlTokenTypes.OPEN:
          case HqlTokenTypes.OPEN_BRACKET:
            open.push(token.getType());
            break;
          case HqlTokenTypes.CLOSE:
            close(open, HqlTokenTypes.OPEN);
            break;
          case HqlTokenTypes.CLOSE_BRACKET:
            close(open, HqlTokenTypes.OPEN_BRACKET);
            break;
          default:
            break;
        }
      }
    } catch (TokenStreamException e) {
      throw new IllegalArgumentException("Invalid domain, " + e.getMessage(), e);
    }
    if (!open.isEmpty()) {
      throw unbalanced();
    }
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
