/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.meta;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.axelor.JpaTest;
import com.axelor.db.JPA;
import com.axelor.meta.db.MetaJsonField;
import com.axelor.meta.db.MetaJsonModel;
import com.axelor.meta.db.MetaJsonRecord;
import com.axelor.meta.db.repo.MetaJsonModelRepository;
import com.axelor.test.db.Title;
import com.google.inject.persist.Transactional;
import jakarta.inject.Inject;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MetaStoreJsonFieldsCacheTest extends JpaTest {

  private static final String MODEL = "MyModel";
  private static final String EMPTY_MODEL = "MyEmptyModel";

  @Inject private MetaJsonModelRepository jsonModels;

  @BeforeEach
  @Transactional
  void ensureModel() {
    MetaStore.invalidateJsonFields();
    if (jsonModels.findByName(MODEL) == null) {
      MetaJsonModel model = new MetaJsonModel();
      model.setName(MODEL);
      model.setTitle("MS Cache");
      model.addField(stringField("name", true));
      model.addField(stringField("status", false));
      jsonModels.save(model);
    }
  }

  @Test
  @Transactional
  void findJsonFields_byModel_returnsConfiguredFields() {
    Map<String, Object> fields = MetaStore.findJsonFields(MODEL);
    assertNotNull(fields);
    assertTrue(fields.containsKey("name"));
    assertTrue(fields.containsKey("status"));
  }

  @Test
  @Transactional
  void findJsonFields_repeatedCalls_returnEqualKeysAndAttrs() {
    Map<String, Object> first = MetaStore.findJsonFields(MODEL);
    Map<String, Object> second = MetaStore.findJsonFields(MODEL);
    assertNotNull(first);
    assertNotNull(second);
    assertEquals(first.keySet(), second.keySet());
    // Returned maps are independent clones; mutating one must not affect the other.
    first.put("ephemeral", "x");
    assertFalse(second.containsKey("ephemeral"));
  }

  @Test
  void invalidate_afterSavingNewField_returnsFreshData() {
    Map<String, Object> before = MetaStore.findJsonFields(MODEL);
    assertNotNull(before);
    assertFalse(before.containsKey("extra"));

    JPA.runInTransaction(
        () -> {
          MetaJsonModel model = jsonModels.findByName(MODEL);
          model.addField(stringField("extra", false));
          jsonModels.save(model);
        });

    Map<String, Object> after = MetaStore.findJsonFields(MODEL);
    assertNotNull(after);
    assertTrue(after.containsKey("extra"), "cache should be invalidated when fields change");
  }

  @Test
  @Transactional
  void findJsonFields_unknownModel_returnsNull() {
    assertNull(MetaStore.findJsonFields("__nope__"));
    assertNull(MetaStore.findJsonFields("__nope__", "attrs"));
    // existing model, but not a JSON field
    assertNull(MetaStore.findJsonFields(Title.class.getName(), "name"));
  }

  @Test
  @Transactional
  void findJsonFields_withoutCustomFields_returnsEmptyMap() {
    // JSON field exists but has no custom fields defined
    Map<String, Object> fields = MetaStore.findJsonFields(Title.class.getName(), "attrs");
    assertNotNull(fields);
    assertTrue(fields.isEmpty());
  }

  @Test
  void findJsonFields_jsonModelWithoutFields_returnsEmptyMap() {
    JPA.runInTransaction(
        () -> {
          if (jsonModels.findByName(EMPTY_MODEL) == null) {
            MetaJsonModel model = new MetaJsonModel();
            model.setName(EMPTY_MODEL);
            model.setTitle("MS Cache Empty");
            jsonModels.save(model);
            JPA.clear();
          }
        });

    Map<String, Object> fields = MetaStore.findJsonFields(EMPTY_MODEL);
    assertNotNull(fields);
    assertTrue(fields.isEmpty());
  }

  @Test
  @Transactional
  void hasJsonField_checksCorrectly() {
    assertTrue(MetaStore.hasJsonField(MODEL, "name"));
    assertTrue(MetaStore.hasJsonField(MODEL, "status"));
    assertFalse(MetaStore.hasJsonField(MODEL, "extra"));
    assertFalse(MetaStore.hasJsonField("__nope__", "name"));

    assertTrue(MetaStore.hasJsonField(MetaJsonRecord.class.getName(), "attrs", "name"));
    assertTrue(MetaStore.hasJsonField(MetaJsonRecord.class.getName(), "attrs", "status"));
    assertFalse(MetaStore.hasJsonField(MetaJsonRecord.class.getName(), "attrs", "extra"));
    assertFalse(MetaStore.hasJsonField("__nope__", "attrs", "name"));
  }

  private MetaJsonField stringField(String name, boolean nameField) {
    MetaJsonField f = new MetaJsonField();
    f.setName(name);
    f.setNameField(nameField);
    f.setType("string");
    f.setModel(MetaJsonRecord.class.getName());
    f.setModelField("attrs");
    return f;
  }
}
