/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
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
        "self.name LIKE '%)%'",
        "self.name = 'it''s (open'",
        "self.name = \"closed)\"",
        "self.id IN (SELECT c.id FROM Contact c WHERE (c.name = ')'))",
        "self.`odd)col` IS NOT NULL",
        "self.emails.email = :email",
        "self.emails[].email = :email",
        "self.id > 0 /* note ) */",
        "self.total */* note */ 2 > 0",
        "self.id > 0 /**/",
        "self.name = '/* not a comment'",
        "self.name = '*/'",
        "self.total * 2 / 3 > 0",
        "self.total / (self.qty * 2) > 0",
        "self.createdOn > {ts '2024-01-01 00:00:00'}",
        "self.birthDate = {d '2024-01-01'} OR self.startTime = {t '10:00:00'}",
        "self.name = \"a\\\"b\"",
        "self.name = j'a\\'b'",
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
        "self.name = \"abc",
        "self.name = \"abc\\\"",
        "self.name = \"abc\\\\\"",
        "self.name = j'abc\\'",
        "self.name = j\"\"",
        "self.name = j\"abc\"",
        "self.name = J\"abc\"",
        "self.`abc = 1",
        "self.`abc\\` = 1",
        "1=1 /*",
        "*/ 1=1",
        "1=1 /* note",
        "1=1 /* 'text' (",
        "1=1 /* closed */ AND 1=1 /* open",
        "self.createdOn > {ts '2024-01-01 00:00:00'",
        "self.createdOn > '2024-01-01'}",
      })
  void rejectsDomainThatEscapesItsParentheses(String domain) {
    assertThrows(IllegalArgumentException.class, () -> DomainValidator.validate(domain));
  }

  @Test
  void acceptsNull() {
    assertDoesNotThrow(() -> DomainValidator.validate(null));
  }
}
