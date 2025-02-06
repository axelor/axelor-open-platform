/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2025 Axelor (<http://axelor.com>).
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
package com.axelor.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.axelor.JpaTest;
import com.axelor.db.internal.DBHelper;
import com.axelor.test.db.Contact;
import com.google.inject.persist.Transactional;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;

@ExtendWith(JsonFunctionsTest.SupportCondition.class)
@TestMethodOrder(OrderAnnotation.class)
class JsonFunctionsTest extends JpaTest {

  private static final String EXPECTED_STRING = "Àbçđœ 日 👍 $1,234.56 € '; -- \"` %_*{}[]()\\";
  private static final int EXPECTED_INTEGER = 1_234_567_890;

  private Contact getContact() {
    var contact =
        all(Contact.class)
            .filter("self.email = :email")
            .bind("email", "jsmith@gmail.com")
            .fetchOne();
    assertNotNull(contact);
    return contact;
  }

  @Test
  @Order(0)
  @Transactional
  void initJson() {
    var contact = getContact();
    var qlString = "UPDATE Contact self SET self.attrs = '{\"myObject\": {}}' WHERE self.id = :id";
    var query = JPA.em().createQuery(qlString).setParameter("id", contact.getId());
    var updated = query.executeUpdate();

    assertEquals(1, updated);
  }

  @Test
  @Order(1)
  @Transactional
  void setJsonString() {
    var contact = getContact();
    var qlString =
        """
        UPDATE Contact self SET self.attrs = json_set(self.attrs, 'myObject.myString', :myString) \
        WHERE self.id = :id""";
    var query =
        JPA.em()
            .createQuery(qlString)
            .setParameter("id", contact.getId())
            .setParameter("myString", EXPECTED_STRING);
    var updated = query.executeUpdate();

    assertEquals(1, updated);
  }

  @Test
  @Order(2)
  @Transactional
  void getJsonString() {
    var contact = getContact();
    var qlString =
        """
        SELECT json_extract_text(self.attrs, 'myObject', 'myString') FROM Contact self \
        WHERE self.id = :id""";
    var query = JPA.em().createQuery(qlString).setParameter("id", contact.getId());
    var result = query.getSingleResult();

    assertEquals(EXPECTED_STRING, result);
  }

  @Test
  @Order(1)
  @Transactional
  void setJsonInteger() {
    var contact = getContact();
    var qlString =
        """
        UPDATE Contact self \
        SET self.attrs = json_set(self.attrs, 'myObject.myInteger', :myInteger) \
        WHERE self.id = :id""";
    var query =
        JPA.em()
            .createQuery(qlString)
            .setParameter("id", contact.getId())
            .setParameter("myInteger", EXPECTED_INTEGER);
    var updated = query.executeUpdate();

    assertEquals(1, updated);
  }

  @Test
  @Order(2)
  @Transactional
  void getJsonInteger() {
    var contact = getContact();
    var qlString =
        """
        SELECT json_extract_integer(self.attrs, 'myObject', 'myInteger') FROM Contact self \
        WHERE self.id = :id""";
    var query = JPA.em().createQuery(qlString).setParameter("id", contact.getId());
    var result = query.getSingleResult();

    assertEquals(EXPECTED_INTEGER, result);
  }

  static class SupportCondition implements ExecutionCondition {
    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
      if (DBHelper.isPostgreSQL() || DBHelper.isMySQL()) {
        return ConditionEvaluationResult.enabled("JSON functions are supported.");
      } else {
        return ConditionEvaluationResult.disabled(
            "JSON functions are not fully supported with current dialect.");
      }
    }
  }
}
