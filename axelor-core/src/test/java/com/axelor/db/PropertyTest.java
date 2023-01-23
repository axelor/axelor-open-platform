/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2023 Axelor (<http://axelor.com>).
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
