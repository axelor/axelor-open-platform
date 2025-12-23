/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.db.audit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.axelor.audit.db.AuditLog;
import com.axelor.auth.AuthUtils;
import com.axelor.auth.db.User;
import com.axelor.concurrent.ContextAware;
import com.axelor.db.JPA;
import com.axelor.db.JpaRepository;
import com.axelor.db.Query;
import com.axelor.test.db.AuditCheck;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import java.util.List;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

public class AuditTest extends BaseAuditTest {

  @Test
  @Order(1)
  public void shouldSetAuditableFieldsOnInsert() {
    final Runnable job = () -> createEntity("Some NAME", "some.name@example.com");
    ContextAware.of().withTransaction(false).withUser(AuthUtils.getUser("admin")).build(job).run();

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

    // Should have AuditLog for the record
    List<AuditLog> auditLogs =
        Query.of(AuditLog.class)
            .filter("self.relatedId = :relatedId AND self.relatedModel = :relatedModel")
            .bind("relatedId", entity.getId())
            .bind("relatedModel", entity.getClass().getName())
            .fetch();
    assertNotNull(auditLogs);
    assertEquals(1, auditLogs.size());
  }

  @Test
  @Order(2)
  public void shouldSetAuditableFieldsOnUpdate() {
    final Runnable job = () -> createEntity("Another NAME", "another.name@example.com");
    ContextAware.of().withTransaction(false).withUser(AuthUtils.getUser("admin")).build(job).run();

    final AuditCheck entity =
        Query.of(AuditCheck.class).filter("self.name = ?", "Another NAME").fetchOne();

    assertNotNull(entity);

    final Integer lastVersion = entity.getVersion();

    final Runnable job2 = () -> updateEntity(entity, "New NAME");
    ContextAware.of().withTransaction(false).withUser(AuthUtils.getUser("admin")).build(job2).run();

    final AuditCheck updatedEntity =
        Query.of(AuditCheck.class).filter("self.name = ?", "New NAME").fetchOne();

    final Integer newVersion = updatedEntity.getVersion();

    assertEquals("New NAME", updatedEntity.getName());
    assertNotEquals(lastVersion, newVersion);

    // updated(On|By) fields should are set
    assertNotNull(updatedEntity.getUpdatedOn());
    assertNotNull(updatedEntity.getUpdatedBy());

    // Should have AuditLog for the record
    List<AuditLog> auditLogs =
        Query.of(AuditLog.class)
            .filter("self.relatedId = :relatedId AND self.relatedModel = :relatedModel")
            .bind("relatedId", entity.getId())
            .bind("relatedModel", entity.getClass().getName())
            .fetch();
    assertNotNull(auditLogs);
    assertEquals(2, auditLogs.size());
  }

  @Test
  @Order(3)
  public void shouldNotChangeAdminUserCode() {
    User user = Query.of(User.class).filter("self.code = ?", "admin").fetchOne();
    assertThrows(PersistenceException.class, () -> updateUser(user, "administrator"));
  }

  @Test
  @Order(4)
  public void shouldNotDeleteAdminUser() {
    User user = Query.of(User.class).filter("self.code = ?", "admin").fetchOne();
    assertThrows(PersistenceException.class, () -> deleteUser(user));
  }

  @Test
  @Order(5)
  void testClear() {
    EntityManager em = getEntityManager();
    assertDoesNotThrow(
        () -> {
          JPA.runInTransaction(
              () -> {
                AuditCheck entity = new AuditCheck();
                entity.setName("testClear");
                User user = JpaRepository.of(User.class).all().fetchOne();
                assertNotNull(user);
                entity.setUser(user);
                em.persist(entity);

                em.flush();
                em.clear();

                entity = em.find(AuditCheck.class, entity.getId());
                entity.setEmail("test@example.com");

                em.flush();
                em.clear();
              });
        });
  }
}
