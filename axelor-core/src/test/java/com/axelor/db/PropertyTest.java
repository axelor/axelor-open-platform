/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.axelor.AbstractTest;
import com.axelor.db.mapper.Mapper;
import com.axelor.db.mapper.Property;
import com.axelor.db.mapper.PropertyType;
import com.axelor.test.db.Address;
import com.axelor.test.db.Contact;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

public class PropertyTest extends AbstractTest {

  private Mapper mapper = Mapper.of(Contact.class);

  @Test
  public void test() {

    Property p = mapper.getProperty("firstName");
    assertEquals(Contact.class, p.getEntity());
    assertEquals("firstName", p.getName());
    assertEquals(PropertyType.STRING, p.getType());

    p = mapper.getProperty("addresses");
    assertEquals("addresses", p.getName());
    assertEquals(PropertyType.ONE_TO_MANY, p.getType());
    assertEquals("contact", p.getMappedBy());
    assertEquals(Address.class, p.getTarget());

    assertTrue(mapper.getProperties().length > 0);

    // virtual column
    p = mapper.getProperty("fullName");
    assertEquals(Contact.class, p.getEntity());
    assertEquals("fullName", p.getName());
    assertEquals(PropertyType.STRING, p.getType());

    // virtual column dependencies
    assertNotNull(mapper.getComputeDependencies(p));
    assertTrue(
        mapper
            .getComputeDependencies(p)
            .containsAll(Arrays.asList("firstName", "lastName", "title")));

    // binary column
    p = mapper.getProperty("image");
    assertEquals(Contact.class, p.getEntity());
    assertEquals("image", p.getName());
    assertEquals(PropertyType.BINARY, p.getType());

    // multiline text
    p = mapper.getProperty("notes");
    assertEquals(Contact.class, p.getEntity());
    assertEquals("notes", p.getName());
    assertEquals(PropertyType.TEXT, p.getType());
  }
}
