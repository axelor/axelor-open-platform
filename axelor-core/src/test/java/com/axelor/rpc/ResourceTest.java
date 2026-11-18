/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.rpc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.axelor.TestingHelpers;
import com.axelor.auth.db.User;
import com.axelor.auth.db.repo.UserRepository;
import com.axelor.db.JPA;
import com.axelor.test.db.Address;
import com.axelor.test.db.Circle;
import com.axelor.test.db.Contact;
import com.axelor.test.db.Title;
import com.axelor.test.db.repo.ContactRepository;
import com.google.inject.persist.Transactional;
import jakarta.inject.Inject;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class ResourceTest extends RpcTest {

  @Inject Resource<Contact> resource;

  @Inject Resource<User> userResource;

  @Inject ContactRepository contacts;

  @Inject UserRepository users;

  @BeforeEach
  @Transactional
  public void ensureAuth() {
    ensureAuth("admin", "admin");
  }

  @AfterAll
  static void tearDown() {
    TestingHelpers.logout();
  }

  @Test
  public void testFields() throws Exception {

    Response res = resource.fields();

    assertNotNull(res);
    assertNotNull(res.getData());
    assertTrue(res.getData() instanceof Map);
  }

  @Test
  public void testSearch() throws Exception {

    Request req = fromJson("find3.json", Request.class);
    Response res = resource.search(req);

    assertNotNull(res);
    assertNotNull(res.getData());
    assertTrue(res.getData() instanceof List);
  }

  @Test
  @SuppressWarnings("all")
  @Transactional
  public void testAdd() throws Exception {

    Request req = fromJson("add2.json", Request.class);
    Response res = resource.save(req);

    assertNotNull(res);
    assertNotNull(res.getData());
    assertNotNull(res.getItem(0));
    assertTrue(res.getItem(0) instanceof Map);
    assertNotNull(((Map) res.getItem(0)).get("id"));

    Contact p = JPA.em().find(Contact.class, ((Map) res.getItem(0)).get("id"));

    assertEquals(Title.class, p.getTitle().getClass());
    assertEquals(Address.class, p.getAddresses().getFirst().getClass());
    assertEquals(Circle.class, p.getCircles().iterator().next().getClass());
    assertEquals(LocalDate.class, p.getDateOfBirth().getClass());

    assertEquals("mr", p.getTitle().getCode());
    assertEquals("France", p.getAddresses().getFirst().getCountry().getName());
    assertEquals("family", p.getCircles().iterator().next().getCode());
    assertEquals("1977-05-01", p.getDateOfBirth().toString());
  }

  @Test
  @SuppressWarnings("all")
  @Transactional
  public void testUpdate() throws Exception {

    Contact c = contacts.all().fetchOne();
    Map<String, Object> data = new HashMap<>();

    data.put("id", c.getId());
    data.put("version", c.getVersion());
    data.put("firstName", "jack");
    data.put("lastName", "sparrow");

    String json = toJson(Map.of("data", data));

    Request req = fromJson(json, Request.class);
    Response res = resource.save(req);

    assertNotNull(res);
    assertNotNull(res.getData());
    assertNotNull(res.getItem(0));
    assertTrue(res.getItem(0) instanceof Map);
    assertNotNull(((Map) res.getItem(0)).get("id"));

    Contact contact = JPA.em().find(Contact.class, ((Map) res.getItem(0)).get("id"));

    assertEquals("jack", contact.getFirstName());
    assertEquals("sparrow", contact.getLastName());
  }

  @Test
  public void testCopy() {

    Contact c = contacts.all().filter("firstName = ?", "James").fetchOne();
    c.setCid(1L);
    Contact n = contacts.copy(c, true);

    assertNotSame(c, n);
    assertNull(n.getCid());
    assertNotSame(c.getAddresses(), n.getAddresses());
    assertEquals(c.getAddresses().size(), n.getAddresses().size());

    assertSame(c, c.getAddresses().getFirst().getContact());
    assertSame(n, n.getAddresses().getFirst().getContact());
  }

  private static Request massUpdateRequest(Long id, String key, Object value) {
    Map<String, Object> values = new HashMap<>();
    values.put(key, value);
    Request req = new Request();
    req.setData(new HashMap<>(Map.of("_domain", "self.id = " + id)));
    req.setRecords(List.of(values));
    return req;
  }

  @Test
  public void testUpdateMass() {
    Long id = contacts.all().fetchOne().getId();

    Response res = resource.updateMass(massUpdateRequest(id, "proEmail", "mass@update.test"));

    assertEquals(Response.STATUS_SUCCESS, res.getStatus());
    assertEquals(
        "mass@update.test",
        JPA.em()
            .createQuery("SELECT self.proEmail FROM Contact self WHERE self.id = :id", String.class)
            .setParameter("id", id)
            .getSingleResult());
  }

  @ParameterizedTest
  @ValueSource(strings = {"self.proEmail", "`proEmail`", "proEmail /**/", " proEmail", "unknown"})
  public void testUpdateMassRejectsUnknownFields(String key) {
    Long id = contacts.all().fetchOne().getId();

    assertThrows(
        IllegalArgumentException.class,
        () -> resource.updateMass(massUpdateRequest(id, key, "mass@update.test")));
  }

  @ParameterizedTest
  @ValueSource(strings = {"id", "version", "notes", "addresses", "circles"})
  public void testUpdateMassRejectsNonUpdatableFields(String key) {
    Long id = contacts.all().fetchOne().getId();

    assertThrows(
        IllegalArgumentException.class,
        () -> resource.updateMass(massUpdateRequest(id, key, null)));
  }

  @Test
  public void testUpdateMassRejectsUserPassword() {
    Long id = users.findByCode("admin").getId();

    assertThrows(
        UnauthorizedException.class,
        () -> userResource.updateMass(massUpdateRequest(id, "password", "secret")));
    assertThrows(
        IllegalArgumentException.class,
        () -> userResource.updateMass(massUpdateRequest(id, "self.password", "secret")));
  }
}
