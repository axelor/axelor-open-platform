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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.axelor.JpaTest;
import com.axelor.db.mapper.Mapper;
import com.axelor.test.db.Contact;
import com.axelor.test.db.TypeCheck;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class MapperTest extends JpaTest {

  private Mapper mapper = Mapper.of(Contact.class);

  @Test
  @Order(1)
  public void testGet() {

    Contact contact = all(Contact.class).fetchOne();

    String firstName = contact.getFirstName();
    String lastName = contact.getLastName();
    Long id = contact.getId();

    assertEquals(firstName, mapper.get(contact, "firstName"));
    assertEquals(lastName, mapper.get(contact, "lastName"));
    assertEquals(id, mapper.get(contact, "id"));
  }

  @Test
  @Order(2)
  public void testSet() {

    Contact contact = all(Contact.class).fetchOne();

    contact.setFirstName("Some");
    contact.setLastName("Name");

    assertEquals("Some", mapper.get(contact, "firstName"));
    assertEquals("Name", mapper.get(contact, "lastName"));
  }

  @Test
  @Order(3)
  public void testBean() {
    Map<String, Object> values = getDemoData();
    Contact contact = JPA.edit(Contact.class, values);

    assertEquals("Some", contact.getFirstName());
    assertEquals("Name", contact.getLastName());
    assertNotSame("Mr. My Name", contact.getFullName());
    assertEquals("Mr. Some Name", contact.getFullName());

    assertNotNull(contact);
    assertNotNull(contact.getId());
    assertNotNull(contact.getDateOfBirth());

    LocalDate date = contact.getDateOfBirth();
    assertEquals(1975, date.getYear());
    assertEquals(3, date.getMonthValue());
    assertEquals(23, date.getDayOfMonth());

    assertNotNull(contact.getTitle());
    assertEquals("Mr.", contact.getTitle().getName());

    assertNotNull(contact.getCircles());
    assertEquals(1, contact.getCircles().size());
    assertEquals("Business", contact.getCircles().iterator().next().getName());
  }

  private Map<String, Object> getDemoData() {
    Map<String, Object> values = new HashMap<>();
    values.put("id", 1L);
    values.put("firstName", "Some");
    values.put("lastName", "Name");
    values.put("fullName", "My Name"); // test readonly
    values.put("dateOfBirth", "1975-02-27");

    Map<String, Object> title = new HashMap<>();
    title.put("code", "mr");
    title.put("name", "Mr.");
    values.put("title", title);

    Set<Map<String, Object>> groups = new HashSet<>();
    Map<String, Object> family = new HashMap<>();
    family.put("code", "family");
    family.put("title", "Family");
    groups.add(family);
    values.put("groups", groups);

    return values;
  }

  @Test
  @Order(4)
  public void testTypes() {
    Map<String, Object> values = new HashMap<>();
    values.put("boolValue", "true");
    values.put("intValue", 121);
    values.put("longValue", 199L);
    values.put("doubleValue", 23.12);
    values.put("decimalValue1", "123.0123456789123");
    values.put("dateTime1", "2011-01-11");
    values.put("localDate1", "1111-11-11");

    values.put("boolValue2", null);
    values.put("intValue2", null);
    values.put("longValue2", null);
    values.put("doubleValue2", null);
    values.put("decimalValue2", null);
    values.put("dateTime2", null);
    values.put("localDate2", null);

    TypeCheck bean = JPA.edit(TypeCheck.class, values);

    assertSame(Boolean.TRUE, bean.getBoolValue());
    assertEquals(121, bean.getIntValue());
    assertEquals(199L, bean.getLongValue());
    assertEquals(new BigDecimal("23.12"), bean.getDoubleValue());

    assertFalse(bean.getBoolValue2());
    assertEquals(0, bean.getIntValue2());
    assertEquals(0L, bean.getLongValue2());
    assertEquals(BigDecimal.ZERO, bean.getDoubleValue2());

    assertEquals(new BigDecimal("123.0123456789"), bean.getDecimalValue1());
    assertEquals(2011, bean.getDateTime1().getYear());
    assertEquals(1111, bean.getLocalDate1().getYear());
  }
}
