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
import com.google.inject.persist.Transactional;
import jakarta.inject.Inject;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MetaStoreJsonFieldsCacheTest extends JpaTest {

  private static final String MODEL = "MyModel";

  @Inject private MetaJsonModelRepository jsonModels;

  @BeforeEach
  @Transactional
  void ensureModel() {
    MetaStore.clear();
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
