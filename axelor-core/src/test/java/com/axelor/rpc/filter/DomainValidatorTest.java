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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class DomainValidatorTest {

  @ParameterizedTest
  @ValueSource(
      strings = {
        "self.name = 'a'",
        "self.id IN (1, 2) AND (self.code IS NULL OR UPPER(self.name) = :name)",
        "self.id IN :ids",
        "self.id = ?1 OR self.id = ?",
        "self.name LIKE '%)%'",
        "self.name = 'it''s (open'",
        "self.id IN (SELECT c.id FROM Contact c WHERE (c.name = ')'))",
        "self.emails.email = :email",
        "self.emails[].email = :email",
        "self.name = '/* not a comment'",
        "self.name = '*/'",
        "self.total * 2 / 3 > 0",
        "self.total / (self.qty * 2) > 0",
        "self.name LIKE '%\\_%'",
        "self.name = 'é' AND self.code <> 'ü'",
      })
  void acceptsSelfContainedDomain(String domain) {
    assertDoesNotThrow(() -> DomainValidator.validate(domain));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "1=1)) OR ((1=1",
        "1=1) OR (1=1",
        "self.code = 'x') OR self.id IN (1",
        "(1=1",
        "1=1)",
        "(1=1]",
        "self.name = 'abc",
        "self.name = 'abc''",
        // a backslash before a quote ends the literal elsewhere in the SQL on MySQL
        "self.name = 'abc\\'",
        "self.name = 'x\\'' OR 1=1 #'",
        "self.name = 'a\\'') OR (''='",
        // not HQL in Hibernate 5, rejected by its lexer
        "self.name = \"abc\"",
        "self.`abc` = 1",
        "self.createdOn > {ts '2024-01-01 00:00:00'}",
        "1=1; DELETE FROM Contact",
      })
  void rejectsDomainThatEscapesItsParentheses(String domain) {
    assertThrows(IllegalArgumentException.class, () -> DomainValidator.validate(domain));
  }

  @Test
  void acceptsNull() {
    assertDoesNotThrow(() -> DomainValidator.validate(null));
  }
}
