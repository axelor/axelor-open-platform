/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.db.audit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.axelor.audit.db.AuditEventType;
import com.axelor.auth.AuthUtils;
import com.axelor.db.JPA;
import com.axelor.db.audit.state.AuditState;
import com.axelor.db.audit.state.EntityState;
import com.axelor.mail.service.MailMessageTrackingService;
import com.axelor.test.GuiceModules;
import com.axelor.test.db.AuditCheck;
import jakarta.inject.Inject;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Tests reference field tracking in {@link MailMessageTrackingService}.
 *
 * <p>Audited references arrive as compact maps containing {@code id}, {@code $version}, and name.
 * Tests verify:
 *
 * <ul>
 *   <li>Labels are read directly from the map without reloading or proxying the entity (AOP-1829).
 *   <li>Change detection compares target IDs rather than version or name changes.
 * </ul>
 */
@GuiceModules(BaseAuditTest.AuditTestModule.class)
class AuditTrackingReferenceTest extends BaseAuditTest {

  @Inject private MailMessageTrackingService service;

  /** Reference labels are read from the audit map even when the target ID is unresolvable. */
  @Test
  void testReferenceLabelComesFromStoredMap() throws Exception {
    var values = new HashMap<String, Object>();
    values.put("name", "Created");
    values.put("primaryEmail", reference(Long.MAX_VALUE, 0, "Created [some@example.com]"));

    var tracks = track(AuditEventType.CREATE, values, new HashMap<>());

    assertTrue(tracks.containsKey("primaryEmail"), "primaryEmail was not tracked: " + tracks);
    assertEquals("Created [some@example.com]", tracks.get("primaryEmail").get("value"));
  }

  /** Version or name updates on the same target ID must not be tracked as reference changes. */
  @Test
  void testSameReferenceWithNewVersionAndNameIsNotTracked() throws Exception {
    var oldValues = new HashMap<String, Object>();
    oldValues.put("name", "Before");
    oldValues.put("primaryEmail", reference(11L, 0, "Before [some@example.com]"));

    var values = new HashMap<String, Object>();
    values.put("name", "After");
    values.put("primaryEmail", reference(11L, 1, "After [some@example.com]"));

    var tracks = track(AuditEventType.UPDATE, values, oldValues);

    assertTrue(tracks.containsKey("name"), "name change was not tracked: " + tracks);
    assertNull(tracks.get("primaryEmail"), "unchanged reference reported as changed: " + tracks);
  }

  /** Changing the target ID must be tracked with previous and updated values. */
  @Test
  void testChangedReferenceIsTracked() throws Exception {
    var oldValues = new HashMap<String, Object>();
    oldValues.put("primaryEmail", reference(11L, 0, "First"));

    var values = new HashMap<String, Object>();
    values.put("primaryEmail", reference(22L, 0, "Second"));

    var tracks = track(AuditEventType.UPDATE, values, oldValues);

    assertNotNull(tracks.get("primaryEmail"), "reference change was not tracked: " + tracks);
    assertEquals("Second", tracks.get("primaryEmail").get("value"));
    assertEquals("First", tracks.get("primaryEmail").get("oldValue"));
  }

  /** Clearing a reference must track the change with an empty new value. */
  @Test
  void testClearedReferenceIsTracked() throws Exception {
    var oldValues = new HashMap<String, Object>();
    oldValues.put("primaryEmail", reference(11L, 0, "First"));

    var values = new HashMap<String, Object>();
    values.put("primaryEmail", null);

    var tracks = track(AuditEventType.UPDATE, values, oldValues);

    assertNotNull(tracks.get("primaryEmail"), "reference change was not tracked: " + tracks);
    assertEquals("", tracks.get("primaryEmail").get("value"));
    assertEquals("First", tracks.get("primaryEmail").get("oldValue"));
  }

  /** Many-to-one references must follow the same change detection and label rules. */
  @Test
  void testManyToOneFollowsTheSameRules() throws Exception {
    var admin = reference(1L, 0, "Administrator");
    admin.put("code", "admin");

    var oldValues = new HashMap<String, Object>();
    oldValues.put("name", "Before");
    oldValues.put("user", admin);

    var bumped = new HashMap<String, Object>(admin);
    bumped.put("$version", 7);

    var values = new HashMap<String, Object>();
    values.put("name", "After");
    values.put("user", bumped);

    var tracks = track(AuditEventType.UPDATE, values, oldValues);

    assertTrue(tracks.containsKey("name"), "name change was not tracked: " + tracks);
    assertNull(tracks.get("user"), "unchanged reference reported as changed: " + tracks);
  }

  // --- helpers ---

  /** Create a compact reference map matching {@code Resource#toMapCompact}. */
  private Map<String, Object> reference(Long id, Integer version, String name) {
    var map = new HashMap<String, Object>();
    map.put("id", id);
    map.put("$version", version);
    map.put("name", name);
    return map;
  }

  /** Process an audit state with {@link MailMessageTrackingService} and return tracking entries. */
  private Map<String, Map<String, String>> track(
      AuditEventType eventType, Map<String, Object> values, Map<String, Object> oldValues)
      throws Exception {

    var entityId = createTracked("Tracking reference");

    JPA.runInTransaction(
        () -> {
          var entity = JPA.em().find(AuditCheck.class, entityId);
          service.process(
              new AuditState(
                  LocalDateTime.now(), eventType, new EntityState(entity, values, oldValues)),
              AuthUtils.getUser("admin"));
        });

    return tracksOf(lastMessage(entityId));
  }
}
