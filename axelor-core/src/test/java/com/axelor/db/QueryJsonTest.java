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
import static org.junit.jupiter.api.Assertions.assertNull;

import com.axelor.script.ScriptTest;
import com.axelor.test.db.Address;
import com.axelor.test.db.Contact;
import com.axelor.test.db.Country;
import com.google.inject.persist.Transactional;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

/** Works only on Postgresql database */
@EnabledIf("com.axelor.db.internal.DBHelper#isPostgreSQL")
public class QueryJsonTest extends ScriptTest {

  @BeforeEach
  @Transactional
  public void setUp() {
    Contact contact = JPA.all(Contact.class).fetchOne();
    contact.setAnotherAttrs(getCustomerAnotherAttrsJson());

    Country country = new Country();
    country.setName("foo");
    country.setCode("FOO");

    Address address = new Address();
    address.setStreet("my street");
    address.setCity("my city");
    address.setZip("my zip");
    address.setCountry(country);
    address.setContact(contact);

    JPA.save(address);
  }

  @Test
  public void filterOnCustomField() {

    // Filter on custom field
    List<Map> list =
        JPA.all(Contact.class)
            .filter("self.anotherAttrs.nickName = :nickName")
            .bind("nickName", "Some Custom Name")
            .select("id", "email")
            .fetch(0, 0);

    assertNotNull(list.get(0));
    assertEquals("jsmith@gmail.com", list.get(0).get("email"));

    list =
        JPA.all(Contact.class)
            .filter("self.anotherAttrs.guardian.id = :guardianId")
            .bind("guardianId", 1L)
            .select("id", "email")
            .fetch(0, 0);

    assertNotNull(list.get(0));
    assertEquals("jsmith@gmail.com", list.get(0).get("email"));

    // select custom field
    list =
        JPA.all(Contact.class)
            .filter("self.id = 1")
            .select("id", "anotherAttrs.guardian.id")
            .fetch(0, 0);

    assertEquals("1", list.get(0).get("anotherAttrs.guardian.id"));

    // cast column value
    list =
        JPA.all(Contact.class)
            .filter("self.anotherAttrs.guardian.id = :guardianId")
            .bind("guardianId", 1L)
            .select("anotherAttrs.guardian.id::integer")
            .fetch(0, 0);

    assertEquals(1, list.get(0).get("anotherAttrs.guardian.id"));
  }

  @Test
  public void filterOnNextLevelCustomField() {

    // Filter on a custom field with level >= 1
    List<Map> list =
        JPA.all(Address.class)
            .filter("self.contact.anotherAttrs.nickName = :nickName")
            .bind("nickName", "Some Custom Name")
            .select("id", "street")
            .fetch(0, 0);

    assertNotNull(list.get(0));
    assertEquals("my street", list.get(0).get("street"));

    list =
        JPA.all(Address.class)
            .filter("self.contact.anotherAttrs.guardian.id = :guardianId")
            .bind("guardianId", 1L)
            .select("id", "street")
            .fetch(0, 0);

    assertNotNull(list.get(0));
    assertEquals("my street", list.get(0).get("street"));

    // select on a custom field with level >= 1 isn't supported
    list =
        JPA.all(Contact.class)
            .filter("self.id = 1")
            .select("contact.anotherAttrs.guardian.id")
            .fetch(0, 0);

    assertNull(list.get(0).get("contact.anotherAttrs.guardian.id"));
  }
}
