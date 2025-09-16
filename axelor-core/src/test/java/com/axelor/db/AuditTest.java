/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.axelor.JpaTest;
import com.axelor.auth.AuditableRunner;
import com.axelor.auth.db.User;
import com.axelor.inject.Beans;
import com.axelor.mail.db.MailMessage;
import com.axelor.meta.db.MetaSequence;
import com.axelor.test.db.AuditCheck;
import com.google.inject.persist.Transactional;
import jakarta.persistence.PersistenceException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AuditTest extends JpaTest {

  @BeforeEach
  public void beforeAll() {
    if (Query.of(MetaSequence.class).count() == 0) {
      fixture("sequence-data.yml");
    }
    if (Query.of(User.class).count() == 0) {
      createUser();
    }
  }

  @Transactional
  void createUser() {
    User user = new User();
    user.setName("Administrator");
    user.setCode("admin");
    user.setPassword("password");
    getEntityManager().persist(user);
  }

  @Transactional
  void createEntity(String name) {
    AuditCheck entity = new AuditCheck();
    entity.setName(name);
    getEntityManager().persist(entity);
  }

  @Transactional
  void updateEntity(AuditCheck entity, String name) {
    entity.setName(name);
    getEntityManager().persist(entity);
  }

  @Transactional
  void updateUser(User entity, String code) {
    entity.setCode(code);
    getEntityManager().persist(entity);
  }

  @Transactional
  void deleteUser(User entity) {
    getEntityManager().remove(entity);
  }

  @Test
  @Order(1)
  public void testInsert() {
    AuditableRunner runner = Beans.get(AuditableRunner.class);

    runner.run(() -> createEntity("Some NAME"));

    AuditCheck entity = Query.of(AuditCheck.class).fetchOne();

    assertNotNull(entity);
    assertNotNull(entity.getId());
    assertNotNull(entity.getName());

    // check audit fields are set
    assertNotNull(entity.getCreatedOn());
    assertNotNull(entity.getCreatedBy());

    // check updated(On|By) fields are not set
    assertNull(entity.getUpdatedOn());
    assertNull(entity.getUpdatedBy());

    // check sequence field is set
    assertNotNull(entity.getEmpSeq());
  }

  @Test
  @Order(2)
  public void testUpdate() {
    final AuditableRunner runner = Beans.get(AuditableRunner.class);

    runner.run(() -> createEntity("Another NAME"));

    final AuditCheck entity =
        Query.of(AuditCheck.class).filter("self.name = ?", "Another NAME").fetchOne();

    assertNotNull(entity);

    final Integer lastVersion = entity.getVersion();

    runner.run(() -> updateEntity(entity, "New NAME"));

    final AuditCheck updatedEntity =
        Query.of(AuditCheck.class).filter("self.name = ?", "New NAME").fetchOne();

    final Integer newVersion = updatedEntity.getVersion();

    assertEquals("New NAME", updatedEntity.getName());
    assertNotEquals(lastVersion, newVersion);

    // updated(On|By) fields should are set
    assertNotNull(updatedEntity.getUpdatedOn());
    assertNotNull(updatedEntity.getUpdatedBy());
  }

  @Test
  @Order(3)
  public void testUpdateUser() {
    User user = Query.of(User.class).filter("self.code = ?", "admin").fetchOne();
    assertThrows(PersistenceException.class, () -> updateUser(user, "administrator"));
  }

  @Test
  @Order(4)
  public void testDeleteUser() {
    User user = Query.of(User.class).filter("self.code = ?", "admin").fetchOne();
    assertThrows(PersistenceException.class, () -> deleteUser(user));
  }

  @Test
  @Order(5)
  public void testTrack() {
    List<MailMessage> messages = Query.of(MailMessage.class).fetch();
    assertNotNull(messages);
    assertNotEquals(0, messages.size());
    assertTrue(
        messages.stream().anyMatch(x -> AuditCheck.class.getName().equals(x.getRelatedModel())));
  }
}
