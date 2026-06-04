/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
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
import jakarta.inject.Inject;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Verifies that a User nested in a relation of another entity is treated as a reference only: it
 * can be attached, but never created or modified through the relation.
 */
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

    // a save nesting the related user with roles and groups
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

    // the nested user must be left untouched: no role and no group assigned
    User saved = users.find(userId);
    assertTrue(
        saved.getRoles() == null || saved.getRoles().isEmpty(),
        "nested save must not assign roles to a User");
    assertFalse(
        saved.getGroup() != null && groupId.equals(saved.getGroup().getId()),
        "nested save must not assign a group to a User");

    // but referencing an existing user through the relation must still work
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

    // a save nesting the related user with roles and groups
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

    // the nested user must be left untouched: no role and no group assigned
    User saved = users.find(userId);
    assertTrue(
        saved.getRoles() == null || saved.getRoles().isEmpty(),
        "nested save must not assign roles to a User");
    assertFalse(
        saved.getGroup() != null && groupId.equals(saved.getGroup().getId()),
        "nested save must not assign a group to a User");

    // but referencing an existing user through the relation must still work
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

    // a save nesting a brand-new user (no id) through a to-one relation
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

    // no user must have been created through the relation
    assertNull(
        users.findByCode("nested.test.created"),
        "nested save must not create a User through a relation");

    // and the relation must be left unset
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

    // a save nesting a brand-new user (no id) through a to-many relation
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

    // no user must have been created through the relation
    assertNull(
        users.findByCode("nested.test.created.member"),
        "nested save must not create a User through a relation");

    // and the relation must be left empty
    Invoice savedRecord = JPA.em().find(Invoice.class, recordId);
    assertTrue(
        savedRecord.getMembers() == null || savedRecord.getMembers().isEmpty(),
        "an uncreatable nested user must not be attached to the relation");
  }
}
