/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.axelor.JpaTest;
import com.axelor.db.internal.DBHelper;
import com.axelor.meta.db.MetaJsonField;
import com.axelor.meta.db.MetaJsonModel;
import com.axelor.meta.db.MetaJsonRecord;
import com.axelor.meta.db.repo.MetaJsonModelRepository;
import com.axelor.meta.db.repo.MetaJsonRecordRepository;
import com.axelor.rpc.Context;
import com.axelor.test.db.Contact;
import com.axelor.test.db.repo.ContactRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.persist.Transactional;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class JsonReferenceTest extends JpaTest {

  private static final String PARENT_MODEL = "jrc_parent";
  private static final String CHILD_MODEL = "jrc_child";

  @Inject private MetaJsonModelRepository jsonModels;
  @Inject private MetaJsonRecordRepository jsonRecords;
  @Inject private ContactRepository contacts;
  @Inject private ObjectMapper jsonMapper;

  @BeforeEach
  @Transactional
  public void setup() {
    ensureModels();
  }

  @Test
  @Transactional
  public void testCascadeCreateAndUpdateJsonChildren() {
    var parent =
        saveParent(
            Map.of(
                "name",
                "Parent-1",
                "lines",
                List.of(Map.of("name", "Line-A", "qty", 1), Map.of("name", "Line-B", "qty", 2))));

    var attrs = parseAttrs(parent);
    var lines = asList(attrs.get("lines"));
    assertEquals(2, lines.size());

    var first = asMap(lines.get(0));
    var second = asMap(lines.get(1));

    var firstId = ((Number) first.get("id")).longValue();
    var secondId = ((Number) second.get("id")).longValue();

    assertTrue(firstId > 0);
    assertTrue(secondId > 0);

    var updatedLines = new ArrayList<Map<String, Object>>();
    updatedLines.add(Map.of("id", firstId, "name", "Line-A2", "qty", 10));
    updatedLines.add(Map.of("id", secondId, "name", "Line-B", "qty", 2));

    parent.setAttrs(toJson(Map.of("name", "Parent-1", "lines", updatedLines)));
    parent = jsonRecords.save(parent);

    var updatedChild = jsonRecords.find(firstId);
    assertNotNull(updatedChild);
    var childAttrs = parseAttrs(updatedChild);
    assertEquals("Line-A2", childAttrs.get("name"));
    assertEquals(10, ((Number) childAttrs.get("qty")).intValue());
  }

  @Test
  @Transactional
  public void testPartialChildPatchPreservesOmittedAttrs() {
    var parent =
        saveParent(
            Map.of("name", "Parent-patch", "lines", List.of(Map.of("name", "Line-P", "qty", 1))));

    var attrs = parseAttrs(parent);
    var lines = asList(attrs.get("lines"));
    var line = asMap(lines.get(0));
    var lineId = ((Number) line.get("id")).longValue();

    parent.setAttrs(
        toJson(Map.of("name", "Parent-patch", "lines", List.of(Map.of("id", lineId, "qty", 5)))));
    parent = jsonRecords.save(parent);

    var updatedChild = jsonRecords.find(lineId);
    assertNotNull(updatedChild);
    var childAttrs = parseAttrs(updatedChild);
    assertEquals("Line-P", childAttrs.get("name"));
    assertEquals(5, ((Number) childAttrs.get("qty")).intValue());
  }

  @Test
  @Transactional
  public void testAttachByIdOnlyDoesNotTouchChildState() {
    var parent =
        saveParent(
            Map.of(
                "name", "Parent-attach", "lines", List.of(Map.of("name", "Original", "qty", 42))));

    var attrs = parseAttrs(parent);
    var lines = asList(attrs.get("lines"));
    var lineId = ((Number) asMap(lines.get(0)).get("id")).longValue();

    // Re-save parent with child referenced by id only — no extra keys
    parent.setAttrs(
        toJson(Map.of("name", "Parent-attach", "lines", List.of(Map.of("id", lineId)))));
    parent = jsonRecords.save(parent);

    var child = jsonRecords.find(lineId);
    assertNotNull(child);
    var childAttrs = parseAttrs(child);
    assertEquals("Original", childAttrs.get("name"));
    assertEquals(42, ((Number) childAttrs.get("qty")).intValue());
  }

  @Test
  @Transactional
  public void testOrphanDeleteForJsonAndEntityO2m() {
    var contact1 = createContact("Alpha", "One");
    var contact2 = createContact("Beta", "Two");

    var parent =
        saveParent(
            Map.of(
                "name",
                "Parent-2",
                "lines",
                List.of(Map.of("name", "Line-1", "qty", 1), Map.of("name", "Line-2", "qty", 2)),
                "contacts",
                List.of(Map.of("id", contact1.getId()), Map.of("id", contact2.getId()))));

    var attrs = parseAttrs(parent);
    var lines = asList(attrs.get("lines"));
    var line1 = asMap(lines.get(0));
    var line2 = asMap(lines.get(1));

    var remainingLineId = ((Number) line1.get("id")).longValue();
    var removedLineId = ((Number) line2.get("id")).longValue();

    var updatedAttrs = new HashMap<String, Object>();
    updatedAttrs.put("name", "Parent-2");
    updatedAttrs.put("lines", List.of(Map.of("id", remainingLineId)));
    updatedAttrs.put("contacts", List.of(Map.of("id", contact1.getId())));

    parent.setAttrs(toJson(updatedAttrs));
    jsonRecords.save(parent);

    assertNotNull(jsonRecords.find(remainingLineId));
    assertNull(jsonRecords.find(removedLineId));

    // Regular JPA entities are NOT cascade-deleted when removed from a JSON one-to-many;
    // they are only dissociated. Only json-model children (MetaJsonRecord) are cascade-deleted.
    assertNotNull(contacts.find(contact1.getId()));
    assertNotNull(contacts.find(contact2.getId()));
  }

  @Test
  @Transactional
  public void testCascadeDeleteOnParentRemoval() {
    var contact1 = createContact("Gamma", "One");
    var contact2 = createContact("Delta", "Two");

    var parent =
        saveParent(
            Map.of(
                "name",
                "Parent-3",
                "lines",
                List.of(Map.of("name", "Line-X", "qty", 5), Map.of("name", "Line-Y", "qty", 6)),
                "contacts",
                List.of(Map.of("id", contact1.getId()), Map.of("id", contact2.getId()))));

    var attrs = parseAttrs(parent);
    var lines = asList(attrs.get("lines"));
    var line1Id = ((Number) asMap(lines.get(0)).get("id")).longValue();
    var line2Id = ((Number) asMap(lines.get(1)).get("id")).longValue();

    jsonRecords.remove(parent);

    assertNull(jsonRecords.find(line1Id));
    assertNull(jsonRecords.find(line2Id));
    // Regular JPA entities are NOT cascade-deleted when parent is removed;
    // only json-model children are owned and cascade-deleted.
    assertNotNull(contacts.find(contact1.getId()));
    assertNotNull(contacts.find(contact2.getId()));
  }

  @Test
  @Transactional
  public void testUpdateNameForJsonM2o() {
    Assumptions.assumeFalse(DBHelper.isHSQL());
    var contact = createContact("Echo", "Initial");
    var fullName = contacts.find(contact.getId()).getFullName();

    var parent =
        saveParent(
            Map.of(
                "name",
                "Parent-4",
                "contact",
                Map.of("id", contact.getId(), "fullName", fullName)));

    contact.setFirstName("Echo");
    contact.setLastName("Updated");
    contacts.save(contact);

    var refreshed = jsonRecords.find(parent.getId());

    // JsonReferenceUpdater#executeUpdate() bypasses the persistence context
    // call em.refresh(entity) after this to avoid stale L1 cache after bulk update
    JPA.em().refresh(refreshed);

    var attrs = parseAttrs(refreshed);
    var contactRef = asMap(attrs.get("contact"));
    assertEquals(contacts.find(contact.getId()).getFullName(), contactRef.get("fullName"));
  }

  @Test
  @Transactional
  public void testUnsavedRegularEntityRejected() {
    var error =
        assertThrows(
            IllegalStateException.class,
            () ->
                saveParent(
                    Map.of("name", "Parent-5", "contact", Map.of("fullName", "Unsaved Contact"))));

    assertTrue(error.getMessage().contains("unsaved"));
    assertTrue(error.getMessage().contains("Save the target entity first"));
  }

  private void ensureModels() {
    var child = jsonModels.findByName(CHILD_MODEL);
    if (child == null) {
      child = new MetaJsonModel();
      child.setName(CHILD_MODEL);
      child.setTitle("JRC Child");
      child.addField(
          new MetaJsonField() {
            {
              setName("name");
              setNameField(true);
              setType("string");
              setModel(MetaJsonRecord.class.getName());
              setModelField("attrs");
            }
          });
      child.addField(
          new MetaJsonField() {
            {
              setName("qty");
              setType("integer");
              setModel(MetaJsonRecord.class.getName());
              setModelField("attrs");
            }
          });
      jsonModels.save(child);
    }

    var parent = jsonModels.findByName(PARENT_MODEL);
    if (parent == null) {
      parent = new MetaJsonModel();
      parent.setName(PARENT_MODEL);
      parent.setTitle("JRC Parent");
      parent.addField(
          new MetaJsonField() {
            {
              setName("name");
              setNameField(true);
              setType("string");
              setModel(MetaJsonRecord.class.getName());
              setModelField("attrs");
            }
          });
      parent.addField(
          new MetaJsonField() {
            {
              setName("lines");
              setType("json-one-to-many");
              setTargetJsonModel(jsonModels.findByName(CHILD_MODEL));
              setModel(MetaJsonRecord.class.getName());
              setModelField("attrs");
            }
          });
      parent.addField(
          new MetaJsonField() {
            {
              setName("contacts");
              setType("one-to-many");
              setTargetModel(Contact.class.getName());
              setModel(MetaJsonRecord.class.getName());
              setModelField("attrs");
            }
          });
      parent.addField(
          new MetaJsonField() {
            {
              setName("contact");
              setType("many-to-one");
              setTargetModel(Contact.class.getName());
              setModel(MetaJsonRecord.class.getName());
              setModelField("attrs");
            }
          });
      jsonModels.save(parent);
    }
  }

  private Contact createContact(String firstName, String lastName) {
    var contact = new Contact();
    contact.setFirstName(firstName);
    contact.setLastName(lastName);
    return contacts.save(contact);
  }

  private MetaJsonRecord saveParent(Map<String, Object> attrs) {
    Context ctx = jsonRecords.create(PARENT_MODEL);
    ctx.putAll(attrs);
    return jsonRecords.save(ctx);
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> parseAttrs(MetaJsonRecord record) {
    try {
      return jsonMapper.readValue(record.getAttrs(), Map.class);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("Failed to parse attrs", e);
    }
  }

  private String toJson(Map<String, Object> attrs) {
    try {
      return jsonMapper.writeValueAsString(attrs);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("Failed to serialize attrs", e);
    }
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> asMap(Object value) {
    return (Map<String, Object>) value;
  }

  @SuppressWarnings("unchecked")
  private List<Object> asList(Object value) {
    return (List<Object>) value;
  }
}
