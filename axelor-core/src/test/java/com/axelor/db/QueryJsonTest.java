/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
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

    assertNotNull(list.getFirst());
    assertEquals("jsmith@gmail.com", list.getFirst().get("email"));

    list =
        JPA.all(Contact.class)
            .filter("self.anotherAttrs.guardian.id = :guardianId")
            .bind("guardianId", 1L)
            .select("id", "email")
            .fetch(0, 0);

    assertNotNull(list.getFirst());
    assertEquals("jsmith@gmail.com", list.getFirst().get("email"));

    // select custom field
    list =
        JPA.all(Contact.class)
            .filter("self.id = 1")
            .select("id", "anotherAttrs.guardian.id")
            .fetch(0, 0);

    assertEquals("1", list.getFirst().get("anotherAttrs.guardian.id"));

    // cast column value
    list =
        JPA.all(Contact.class)
            .filter("self.anotherAttrs.guardian.id = :guardianId")
            .bind("guardianId", 1L)
            .select("anotherAttrs.guardian.id::integer")
            .fetch(0, 0);

    assertEquals(1, list.getFirst().get("anotherAttrs.guardian.id"));
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

    assertNotNull(list.getFirst());
    assertEquals("my street", list.getFirst().get("street"));

    list =
        JPA.all(Address.class)
            .filter("self.contact.anotherAttrs.guardian.id = :guardianId")
            .bind("guardianId", 1L)
            .select("id", "street")
            .fetch(0, 0);

    assertNotNull(list.getFirst());
    assertEquals("my street", list.getFirst().get("street"));

    // select on a custom field with level >= 1 isn't supported
    list =
        JPA.all(Contact.class)
            .filter("self.id = 1")
            .select("contact.anotherAttrs.guardian.id")
            .fetch(0, 0);

    assertNull(list.getFirst().get("contact.anotherAttrs.guardian.id"));
  }
}
