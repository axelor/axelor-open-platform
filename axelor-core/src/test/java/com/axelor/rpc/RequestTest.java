/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2022 Axelor (<http://axelor.com>).
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
package com.axelor.rpc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.axelor.db.Query;
import com.axelor.test.db.Address;
import com.axelor.test.db.Circle;
import com.axelor.test.db.Contact;
import com.axelor.test.db.Title;
import com.axelor.test.db.repo.ContactRepository;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.inject.persist.Transactional;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import org.junit.jupiter.api.Test;

public class RequestTest extends RpcTest {

  @Inject private ContactRepository contacts;

  @Test
  public void testObjects() {

    Request req = fromJson("find.json", Request.class);

    assertTrue(req.getData() instanceof Map);
    assertTrue(req.getData().get("criteria") instanceof List);

    for (Object o : (List<?>) req.getData().get("criteria")) {
      assertTrue(o instanceof Map);
    }
  }

  @Test
  public void testFind() {

    Request req = fromJson("find.json", Request.class);

    Criteria c = Criteria.parse(req);
    Query<Contact> q = c.createQuery(Contact.class);

    String actual = q.toString();
    assertNotNull(actual);
  }

  @Test
  public void testFind2() {

    Request req = fromJson("find2.json", Request.class);

    Criteria c = Criteria.parse(req);
    Query<Contact> q = c.createQuery(Contact.class);

    String actual = q.toString();

    assertEquals(
        "SELECT self FROM Contact self WHERE (self.archived is null OR self.archived = false)",
        actual);
  }

  @Test
  public void testFind3() {

    Request req = fromJson("find3.json", Request.class);

    Criteria c = Criteria.parse(req);
    Query<Contact> q = c.createQuery(Contact.class);

    assertTrue(q.count() > 0);
  }

  @Test
  public void testAdd() {

    Request req = fromJson("add.json", Request.class);

    assertTrue(req.getData() instanceof Map);

    Map<String, Object> data = req.getData();

    assertEquals("some", data.get("firstName"));
    assertEquals("thing", data.get("lastName"));
    assertEquals("some@thing.com", data.get("email"));
  }

  @Test
  @Transactional
  public void testAdd2() {

    Request req = fromJson("add2.json", Request.class);

    assertTrue(req.getData() instanceof Map);

    Map<String, Object> data = req.getData();

    assertEquals("Jack", data.get("firstName"));
    assertEquals("Sparrow", data.get("lastName"));
    assertEquals("jack.sparrow@gmail.com", data.get("email"));

    Contact p = contacts.edit(data);

    assertEquals(Title.class, p.getTitle().getClass());
    assertEquals(Address.class, p.getAddresses().get(0).getClass());
    assertEquals(Circle.class, p.getCircle(0).getClass());
    assertEquals(LocalDate.class, p.getDateOfBirth().getClass());

    assertEquals("mr", p.getTitle().getCode());
    assertEquals("France", p.getAddresses().get(0).getCountry().getName());
    assertEquals("family", p.getCircle(0).getCode());
    assertEquals("1977-05-01", p.getDateOfBirth().toString());

    contacts.manage(p);
  }

  @Test
  @Transactional
  public void testUpdate() {

    Contact c = contacts.all().fetchOne();
    Map<String, Object> data = Maps.newHashMap();

    data.put("id", c.getId());
    data.put("version", c.getVersion());
    data.put("firstName", "Some");
    data.put("lastName", "thing");

    String json = toJson(ImmutableMap.of("data", data));
    Request req = fromJson(json, Request.class);

    assertTrue(req.getData() instanceof Map);

    data = req.getData();

    assertEquals("Some", data.get("firstName"));
    assertEquals("thing", data.get("lastName"));

    Contact o = contacts.edit(data);

    o = contacts.manage(o);
  }

  @Test
  public void testEdit() {
    Map<String, Object> janeValues = ImmutableMap.of("firstName", "Jane", "lastName", "Doe");
    Map<String, Object> babyValues = ImmutableMap.of("firstName", "Baby", "lastName", "Doe");
    Map<String, Object> johnValues =
        ImmutableMap.of(
            "firstName",
            "John",
            "lastName",
            "Doe",
            "relatedContacts",
            ImmutableList.of(janeValues, babyValues));
    Contact john = contacts.edit(johnValues);
    assertNotNull(
        String.format("Entity instance should contain field passed to JPA#edit.", john),
        john.getFirstName());
    john.getRelatedContacts().stream()
        .forEach(
            relatedContact ->
                assertNotNull(
                    String.format(
                        "Child entity instance should contain field passed to JPA#edit.", john),
                    relatedContact.getFirstName()));
  }
}
