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
package com.axelor.rpc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.axelor.auth.db.Group;
import com.axelor.auth.db.Role;
import com.axelor.auth.db.User;
import com.axelor.auth.db.repo.UserRepository;
import com.axelor.db.JPA;
import com.axelor.test.db.Invoice;
import com.google.inject.persist.Transactional;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class NestedSaveTest extends RpcTest {

  @Inject private Resource<Invoice> resource;
  @Inject private UserRepository users;

  private static Long userId;
  private static Long roleId;
  private static Long groupId;

  @BeforeAll
  public static void loadData() {
    JPA.runInTransaction(
        () -> {
          // create role/group
          Role adminRole = new Role("nested.test.admin.role");
          JPA.save(adminRole);
          roleId = adminRole.getId();

          Group adminGroup = new Group("nested.test.admins", "Nested Test Admins");
          JPA.save(adminGroup);
          groupId = adminGroup.getId();

          // a plain user, with no roles or groups
          User member = new User("nested.test.member", "Nested Test Member");
          member.setPassword("pass");
          JPA.save(member);
          userId = member.getId();
        });
  }

  @Test
  @Transactional
  public void testNestedUserCannotGainRestrictedFields() {
    User user = JPA.find(User.class, userId);

    // a record relating to that user
    Invoice invoice = new Invoice();
    invoice.setSaleUser(user);
    JPA.save(invoice);

    JPA.em().flush();

    final Long recordId = invoice.getId();

    Map<String, Object> nestedUser =
        Map.of(
            "id", user.getId(),
            "version", user.getVersion(),
            "roles", List.of(Map.of("id", roleId)),
            "group", Map.of("id", groupId));

    Map<String, Object> data =
        Map.of(
            "id", recordId,
            "version", invoice.getVersion(),
            "saleUser", nestedUser);

    Request request = fromJson(toJson(Map.of("data", data)), Request.class);
    Response response = resource.save(request);

    assertEquals(Response.STATUS_SUCCESS, response.getStatus());

    JPA.em().clear();

    User saved = users.find(userId);
    assertTrue(
        saved.getRoles() == null || saved.getRoles().isEmpty(),
        "nested save must not assign roles to a User");
    assertFalse(
        saved.getGroup() != null && groupId.equals(saved.getGroup().getId()),
        "nested save must not assign a group to a User");

    Invoice savedRecord = JPA.em().find(Invoice.class, recordId);
    assertNotNull(savedRecord.getSaleUser());
    assertEquals(
        userId,
        savedRecord.getSaleUser().getId(),
        "referencing an existing user through a relation must still work");
  }

  @Test
  @Transactional
  public void testNestedUsersCannotGainRestrictedFields() {
    User user = JPA.find(User.class, userId);

    Invoice invoice = new Invoice();
    JPA.save(invoice);

    JPA.em().flush();

    final Long recordId = invoice.getId();

    Map<String, Object> nestedUser =
        Map.of(
            "id", user.getId(),
            "version", user.getVersion(),
            "roles", List.of(Map.of("id", roleId)),
            "group", Map.of("id", groupId));

    Map<String, Object> data =
        Map.of(
            "id", recordId,
            "version", invoice.getVersion(),
            "members", List.of(nestedUser));

    Request request = fromJson(toJson(Map.of("data", data)), Request.class);
    Response response = resource.save(request);

    assertEquals(Response.STATUS_SUCCESS, response.getStatus());

    JPA.em().clear();

    User saved = users.find(userId);
    assertTrue(
        saved.getRoles() == null || saved.getRoles().isEmpty(),
        "nested save must not assign roles to a User");
    assertFalse(
        saved.getGroup() != null && groupId.equals(saved.getGroup().getId()),
        "nested save must not assign a group to a User");

    Invoice savedRecord = JPA.em().find(Invoice.class, recordId);
    assertNotNull(savedRecord.getMembers());
    assertEquals(
        userId,
        savedRecord.getMembers().iterator().next().getId(),
        "referencing an existing user through a relation must still work");
  }

  @Test
  @Transactional
  public void testNestedUserCannotBeCreated() {
    Invoice invoice = new Invoice();
    JPA.save(invoice);

    JPA.em().flush();

    final Long recordId = invoice.getId();

    Map<String, Object> nestedUser =
        Map.of(
            "code",
            "nested.test.created",
            "name",
            "Nested Test Created",
            "roles",
            List.of(Map.of("id", roleId)),
            "group",
            Map.of("id", groupId));

    Map<String, Object> data =
        Map.of(
            "id", recordId,
            "version", invoice.getVersion(),
            "saleUser", nestedUser);

    Request request = fromJson(toJson(Map.of("data", data)), Request.class);
    Response response = resource.save(request);

    assertEquals(Response.STATUS_SUCCESS, response.getStatus());

    JPA.em().clear();

    assertNull(
        users.findByCode("nested.test.created"),
        "nested save must not create a User through a relation");

    Invoice savedRecord = JPA.em().find(Invoice.class, recordId);
    assertNull(
        savedRecord.getSaleUser(),
        "an uncreatable nested user must not be attached to the relation");
  }

  @Test
  @Transactional
  public void testNestedUsersCannotBeCreated() {
    Invoice invoice = new Invoice();
    JPA.save(invoice);

    JPA.em().flush();

    final Long recordId = invoice.getId();

    Map<String, Object> nestedUser =
        Map.of(
            "code",
            "nested.test.created.member",
            "name",
            "Nested Test Created Member",
            "roles",
            List.of(Map.of("id", roleId)),
            "group",
            Map.of("id", groupId));

    Map<String, Object> data =
        Map.of(
            "id", recordId,
            "version", invoice.getVersion(),
            "members", List.of(nestedUser));

    Request request = fromJson(toJson(Map.of("data", data)), Request.class);
    Response response = resource.save(request);

    assertEquals(Response.STATUS_SUCCESS, response.getStatus());

    JPA.em().clear();

    assertNull(
        users.findByCode("nested.test.created.member"),
        "nested save must not create a User through a relation");

    Invoice savedRecord = JPA.em().find(Invoice.class, recordId);
    assertTrue(
        savedRecord.getMembers() == null || savedRecord.getMembers().isEmpty(),
        "an uncreatable nested user must not be attached to the relation");
  }
}
