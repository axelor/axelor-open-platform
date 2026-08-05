/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.db.audit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.axelor.db.EntityHelper;
import com.axelor.db.JPA;
import com.axelor.db.Query;
import com.axelor.db.mapper.Mapper;
import com.axelor.meta.db.MetaJsonField;
import com.axelor.test.GuiceModules;
import com.axelor.test.db.AuditCheck;
import com.google.inject.persist.Transactional;
import org.hibernate.proxy.HibernateProxy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Verifies custom field tracking when the entity is an uninitialized Hibernate proxy.
 *
 * <p>{@code AuditProcessor} uses {@code JPA.findReferenceById}, which returns a reference via
 * {@code EntityManager#getReference}. Downstream services receive a proxy, requiring lookups to
 * resolve the underlying entity class via {@link EntityHelper#getEntityClass(Object)}:
 *
 * <ul>
 *   <li>{@code AuditTracker#getTrackedCustomFields} must query using the entity model name.
 *   <li>{@code Mapper#getProperty(Object, String)} must resolve JSON properties with the entity
 *       class.
 * </ul>
 */
@GuiceModules(BaseAuditTest.AuditTestModule.class)
class AuditTrackingCustomFieldTest extends BaseAuditTest {

  private static final String JSON_FIELD = "attrs";
  private static final String CUSTOM_FIELD = "nickname";
  private static final String TRACK_KEY = JSON_FIELD + "." + CUSTOM_FIELD;

  @BeforeEach
  void createTrackedCustomField() {
    if (Query.of(MetaJsonField.class)
            .filter("self.model = :model AND self.name = :name")
            .bind("model", AuditCheck.class.getName())
            .bind("name", CUSTOM_FIELD)
            .count()
        == 0) {
      saveTrackedCustomField();
    }
  }

  /** Both tracking lookups must resolve custom fields directly from a Hibernate proxy. */
  @Test
  void testCustomFieldIsResolvedThroughProxy() {
    var entityId = createTracked("Custom field");
    JPA.clear();

    var reference = JPA.findReferenceById(AuditCheck.class, entityId);
    assertNotNull(reference);
    assertTrue(
        reference instanceof HibernateProxy,
        "fixture must exercise a proxy, got " + reference.getClass());

    var track = AuditTracker.getTrack(reference);
    assertNotNull(track, "no tracking config resolved for the proxy");
    assertTrue(
        track.getFields().stream().anyMatch(field -> TRACK_KEY.equals(field.getName())),
        "tracked custom field not found through the proxy: "
            + track.getFields().stream().map(f -> f.getName()).toList());

    assertNotNull(
        Mapper.of(EntityHelper.getEntityClass(reference)).getProperty(reference, TRACK_KEY),
        "custom field property not resolved through the proxy");
  }

  /** Custom field changes on an audited entity must be recorded in the tracking message. */
  @Test
  void testTrackedCustomFieldReachesTheTrackingMessage() throws Exception {
    var entityId = asAdmin(() -> createTracked("Custom field"));
    processAuditLogs();

    asAdmin(() -> setCustomField(entityId, "Bob"));
    processAuditLogs();

    var tracks = tracksOf(lastMessage(entityId));
    assertTrue(tracks.containsKey(TRACK_KEY), "custom field was not tracked: " + tracks);
    assertEquals("Bob", tracks.get(TRACK_KEY).get("value"));
  }

  // --- fixtures ---

  @Transactional
  void saveTrackedCustomField() {
    var field = new MetaJsonField();
    field.setModel(AuditCheck.class.getName());
    field.setModelField(JSON_FIELD);
    field.setName(CUSTOM_FIELD);
    field.setTitle("Nickname");
    field.setType("string");
    field.setTracked(Boolean.TRUE);
    getEntityManager().persist(field);
  }

  @Transactional
  Long setCustomField(Long entityId, String value) {
    var entity = JPA.em().find(AuditCheck.class, entityId);
    entity.setAttrs("{\"" + CUSTOM_FIELD + "\":\"" + value + "\"}");
    JPA.flush();
    return entity.getId();
  }
}
