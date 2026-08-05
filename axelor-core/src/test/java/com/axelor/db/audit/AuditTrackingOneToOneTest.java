/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.db.audit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.axelor.audit.db.AuditLog;
import com.axelor.db.JPA;
import com.axelor.db.Query;
import com.axelor.test.GuiceModules;
import com.axelor.test.db.AuditCheck;
import com.axelor.test.db.AuditCheckEmail;
import com.google.inject.persist.Transactional;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/**
 * Regression tests for tracking {@code @OneToOne} relations whose target derives its name column
 * from the back-reference (AOP-1829).
 *
 * <p>{@code AuditCheck.primaryEmail} owns the relation and {@code AuditCheckEmail.auditCheck} is
 * the inverse side. {@code AuditCheckEmail.name} is a {@code @NameColumn} computed from the parent.
 * Tests cover:
 *
 * <ul>
 *   <li>Wiring the back-reference during {@code JPA.edit} so computed name columns persist
 *       properly.
 *   <li>Preventing entity detachment or duplicate rows when audit logging reads reference names.
 * </ul>
 *
 * <p>Tests run {@code AuditProcessor} synchronously to catch processing errors directly.
 */
@GuiceModules(BaseAuditTest.AuditTestModule.class)
class AuditTrackingOneToOneTest extends BaseAuditTest {

  private static final String ENTITY_NAME = "One To One";
  private static final String ADDRESS = "one.to.one@example.com";
  private static final String EXPECTED_LABEL = ENTITY_NAME + " [" + ADDRESS + "]";

  /** Attaching a new one-to-one record must track the change and persist the computed name. */
  @Test
  void testAttachingTrackedOneToOneIsTracked() throws Exception {
    var entityId = asAdmin(() -> createTracked(ENTITY_NAME));
    processAuditLogs();

    var emailId = asAdmin(() -> attachEmail(entityId, ADDRESS));
    processAuditLogs();

    // Check the raw database column, as the entity getter recomputes dynamically.
    assertEquals(EXPECTED_LABEL, storedName(emailId));

    // Verify that the worker processed all pending logs.
    assertNoPendingAuditLogs(entityId);

    // Verify the tracking message contains the recorded reference label.
    var tracks = tracksOf(lastMessage(entityId));
    assertTrue(tracks.containsKey("primaryEmail"), "primaryEmail was not tracked: " + tracks);
    assertEquals(EXPECTED_LABEL, tracks.get("primaryEmail").get("value"));
  }

  /** Attaching a one-to-one entity must not create duplicate or orphaned records. */
  @Test
  void testAttachingTrackedOneToOneLeavesNoOrphan() throws Exception {
    var address = "no.orphan@example.com";

    var entityId = asAdmin(() -> createTracked("No Orphan"));
    processAuditLogs();

    var emailId = asAdmin(() -> attachEmail(entityId, address));
    processAuditLogs();

    assertEquals(1, Query.of(AuditCheckEmail.class).filter("self.address = ?", address).count());

    JPA.clear();
    var entity = JPA.em().find(AuditCheck.class, entityId);
    assertNotNull(entity.getPrimaryEmail());
    assertEquals(emailId, entity.getPrimaryEmail().getId());
    assertEquals(entityId, JPA.em().find(AuditCheckEmail.class, emailId).getAuditCheck().getId());
  }

  /** Updates altering only the target entity computed name must not trigger reference tracking. */
  @Test
  void testReferenceNameChangeIsNotTrackedAsAReferenceChange() throws Exception {
    var address = "rename@example.com";

    var entityId = asAdmin(() -> createTracked("Before"));
    processAuditLogs();

    var emailId = asAdmin(() -> attachEmail(entityId, address));
    processAuditLogs();

    assertEquals("Before [" + address + "]", storedName(emailId));

    asAdmin(() -> renameTracked(entityId, "After"));
    processAuditLogs();

    // Verify the computed label updated in the database.
    assertEquals("After [" + address + "]", storedName(emailId));

    var tracks = tracksOf(lastMessage(entityId));
    assertTrue(tracks.containsKey("name"), "name change was not tracked: " + tracks);
    assertNull(tracks.get("primaryEmail"), "unchanged reference reported as changed: " + tracks);
  }

  /** Replacing a referenced entity with a different record must be tracked. */
  @Test
  void testReplacingReferenceIsTracked() throws Exception {
    var entityId = asAdmin(() -> createTracked("Replace"));
    processAuditLogs();

    asAdmin(() -> attachEmail(entityId, "first@example.com"));
    processAuditLogs();

    asAdmin(() -> attachEmail(entityId, "second@example.com"));
    processAuditLogs();

    assertNoPendingAuditLogs(entityId);

    var tracks = tracksOf(lastMessage(entityId));
    assertTrue(tracks.containsKey("primaryEmail"), "primaryEmail was not tracked: " + tracks);
    assertEquals("Replace [second@example.com]", tracks.get("primaryEmail").get("value"));
    assertEquals("Replace [first@example.com]", tracks.get("primaryEmail").get("oldValue"));
  }

  // --- fixtures ---

  /** Attaches a new record via map payload, matching inline editor behavior. */
  @Transactional
  Long attachEmail(Long entityId, String address) {
    var entity = JPA.em().find(AuditCheck.class, entityId);

    var email = new HashMap<String, Object>();
    email.put("address", address);

    var values = new HashMap<String, Object>();
    values.put("id", entityId);
    values.put("version", entity.getVersion());
    values.put("primaryEmail", email);

    var edited = JPA.save(JPA.edit(AuditCheck.class, values));
    JPA.flush();

    return edited.getPrimaryEmail().getId();
  }

  @Transactional
  Long renameTracked(Long entityId, String name) {
    var entity = JPA.em().find(AuditCheck.class, entityId);
    entity.setName(name);
    JPA.flush();
    return entity.getId();
  }

  // --- helpers ---

  /** Reads the persisted name column directly from the database table. */
  private String storedName(Long emailId) {
    JPA.clear();
    return (String)
        JPA.em()
            .createNativeQuery("SELECT name FROM audit_check_email WHERE id = ?1")
            .setParameter(1, emailId)
            .getSingleResult();
  }

  private void assertNoPendingAuditLogs(Long entityId) {
    List<AuditLog> pending =
        Query.of(AuditLog.class)
            .filter("self.relatedModel = :model AND self.relatedId = :id")
            .bind("model", AuditCheck.class.getName())
            .bind("id", entityId)
            .fetch();
    assertTrue(
        pending.isEmpty(),
        () ->
            "audit logs were not processed: "
                + pending.stream()
                    .map(log -> log.getEventType() + " -> " + log.getErrorMessage())
                    .collect(Collectors.joining(", ")));
  }
}
