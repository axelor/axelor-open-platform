/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.axelor.JpaTest;
import com.axelor.test.db.ComputedField;
import com.axelor.test.db.ComputedFieldOwner;
import com.google.inject.persist.Transactional;
import org.hibernate.annotations.DynamicUpdate;
import org.junit.jupiter.api.Test;

/**
 * Verifies that loading an entity (directly or via many-to-one) never bumps its {@code @Version},
 * while modifying an entity correctly bumps only its own {@code @Version}.
 *
 * <p>{@link ComputedFieldOwner} declares the many-to-one, so it owns the foreign key; {@link
 * ComputedField} is the referenced entity.
 */
class ComputedFieldVersionTest extends JpaTest {

  /** Requires entities without {@code @DynamicUpdate} for this test to be meaningful. */
  @Test
  void testEntitiesHaveNoDynamicUpdate() {
    assertFalse(
        ComputedField.class.isAnnotationPresent(DynamicUpdate.class),
        "ComputedField must not be @DynamicUpdate");
    assertFalse(
        ComputedFieldOwner.class.isAnnotationPresent(DynamicUpdate.class),
        "ComputedFieldOwner must not be @DynamicUpdate");
  }

  /** Loading an entity with a computed field, without modifying it, must not bump its version. */
  @Test
  void testLoadingEntityDoesNotBumpVersion() {
    Long id = createComputedField("John", "Smith");
    int before = versionOf(ComputedField.class, id);

    loadComputedField(id);

    assertEquals(
        before, versionOf(ComputedField.class, id), "read-only load must not bump @Version");
  }

  /** Reaching a computed-field entity through a many-to-one must not bump either version. */
  @Test
  void testLoadingThroughManyToOneDoesNotBumpEitherVersion() {
    Long[] ids = createOwnerWithRef("owner", "Jane", "Doe");
    Long ownerId = ids[0];
    Long refId = ids[1];
    int ownerBefore = versionOf(ComputedFieldOwner.class, ownerId);
    int refBefore = versionOf(ComputedField.class, refId);

    loadOwner(ownerId);

    assertEquals(
        refBefore,
        versionOf(ComputedField.class, refId),
        "loading through m2o must not bump referenced @Version");
    assertEquals(
        ownerBefore,
        versionOf(ComputedFieldOwner.class, ownerId),
        "loading owner must not bump its @Version");
  }

  /** Modifying an entity with computed fields must bump its version exactly once. */
  @Test
  void testModifyingEntityBumpsVersionOnce() {
    Long id = createComputedField("John", "Smith");
    int before = versionOf(ComputedField.class, id);

    updateComputedFieldFirstName(id, "Johnny");

    assertEquals(
        before + 1,
        versionOf(ComputedField.class, id),
        "modifying an entity must bump its @Version");
  }

  /** Modifying the owner must bump its own @Version but leave the referenced @Version untouched. */
  @Test
  void testModifyingOwnerBumpsOwnerVersionOnly() {
    Long[] ids = createOwnerWithRef("owner", "Jane", "Doe");
    Long ownerId = ids[0];
    Long refId = ids[1];
    int ownerBefore = versionOf(ComputedFieldOwner.class, ownerId);
    int refBefore = versionOf(ComputedField.class, refId);

    updateOwnerName(ownerId, "updatedOwner");

    assertEquals(
        ownerBefore + 1,
        versionOf(ComputedFieldOwner.class, ownerId),
        "modifying owner must bump owner @Version");
    assertEquals(
        refBefore,
        versionOf(ComputedField.class, refId),
        "modifying owner must not bump referenced @Version");
  }

  /** Modifying the referenced entity must bump its @Version but leave the owner's untouched. */
  @Test
  void testModifyingRefBumpsRefVersionOnly() {
    Long[] ids = createOwnerWithRef("owner", "Jane", "Doe");
    Long ownerId = ids[0];
    Long refId = ids[1];
    int ownerBefore = versionOf(ComputedFieldOwner.class, ownerId);
    int refBefore = versionOf(ComputedField.class, refId);

    updateRefFirstName(ownerId, "Janet");

    assertEquals(
        refBefore + 1,
        versionOf(ComputedField.class, refId),
        "modifying referenced entity must bump its @Version");
    assertEquals(
        ownerBefore,
        versionOf(ComputedFieldOwner.class, ownerId),
        "modifying referenced entity must not bump owner @Version");
  }

  @Transactional
  Long createComputedField(String firstName, String lastName) {
    return JPA.save(new ComputedField(firstName, lastName)).getId();
  }

  @Transactional
  Long[] createOwnerWithRef(String ownerName, String firstName, String lastName) {
    var owner = new ComputedFieldOwner(ownerName);
    owner.setRef(new ComputedField(firstName, lastName));
    owner = JPA.save(owner);
    return new Long[] {owner.getId(), owner.getRef().getId()};
  }

  @Transactional
  void loadComputedField(Long id) {
    var entity = JPA.find(ComputedField.class, id);
    assertNotNull(entity);
    assertNotNull(entity.getFullName()); // forces the recompute in the getter
  }

  @Transactional
  void loadOwner(Long id) {
    var owner = JPA.find(ComputedFieldOwner.class, id);
    assertNotNull(owner);
    assertNotNull(owner.getRef());
    assertNotNull(owner.getRef().getFullName()); // recompute on the referenced entity
  }

  @Transactional
  void updateComputedFieldFirstName(Long id, String newFirstName) {
    var entity = JPA.find(ComputedField.class, id);
    assertNotNull(entity);
    entity.setFirstName(newFirstName);
    assertNotNull(entity.getFullName()); // forces the recompute in the getter
  }

  @Transactional
  void updateOwnerName(Long ownerId, String newName) {
    var owner = JPA.find(ComputedFieldOwner.class, ownerId);
    assertNotNull(owner);
    owner.setName(newName);
    assertNotNull(owner.getRef());
    assertNotNull(owner.getRef().getFullName()); // recompute on referenced entity
  }

  @Transactional
  void updateRefFirstName(Long ownerId, String newFirstName) {
    var owner = JPA.find(ComputedFieldOwner.class, ownerId);
    assertNotNull(owner);
    assertNotNull(owner.getRef());
    owner.getRef().setFirstName(newFirstName);
    assertNotNull(owner.getRef().getFullName()); // forces recompute on referenced entity
  }

  private int versionOf(Class<? extends Model> entityClass, Long id) {
    JPA.clear();
    return JPA.find(entityClass, id).getVersion();
  }
}
