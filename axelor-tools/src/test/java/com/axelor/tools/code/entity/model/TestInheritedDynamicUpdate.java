/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.tools.code.entity.model;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.axelor.tools.code.JavaAnnotation;
import com.axelor.tools.code.JavaType;
import java.time.Duration;
import org.hibernate.annotations.DynamicUpdate;
import org.junit.jupiter.api.Test;

class TestInheritedDynamicUpdate {

  private static final String DYNAMIC_UPDATE = DynamicUpdate.class.getName();

  private boolean hasDynamicUpdate(Entity entity) {
    JavaType javaClass = entity.toJavaClass();
    return javaClass.getAnnotations().stream()
        .map(JavaAnnotation::getName)
        .anyMatch(DYNAMIC_UPDATE::equals);
  }

  @Test
  void testInheritedLargeField() {
    Entity parent = new Entity();
    parent.setName("Parent");
    Property.StringProperty largeProp = new Property.StringProperty();
    largeProp.setName("largeText");
    largeProp.setLarge(true);
    parent.getFields().add(largeProp);

    Entity child = new Entity();
    child.setName("Child");
    child.setSuperClass("com.example.Parent");
    child.setSuperEntity(parent);

    Property.StringProperty titleProp = new Property.StringProperty();
    titleProp.setName("title");
    child.getFields().add(titleProp);

    assertTrue(
        hasDynamicUpdate(child), "Child should inherit @DynamicUpdate due to parent's large field");
  }

  @Test
  void testInheritedBinaryField() {
    Entity parent = new Entity();
    parent.setName("Parent");
    Property.BinaryProperty binaryProp = new Property.BinaryProperty();
    binaryProp.setName("fileData");
    parent.getFields().add(binaryProp);

    Entity child = new Entity();
    child.setName("Child");
    child.setSuperClass("com.example.Parent");
    child.setSuperEntity(parent);

    Property.StringProperty titleProp = new Property.StringProperty();
    titleProp.setName("title");
    child.getFields().add(titleProp);

    assertTrue(
        hasDynamicUpdate(child),
        "Child should inherit @DynamicUpdate due to parent's binary field");
  }

  @Test
  void testInheritedWideEntity() {
    int parentCount = Entity.DYNAMIC_UPDATE_FIELD_THRESHOLD / 2;
    int childCount = (Entity.DYNAMIC_UPDATE_FIELD_THRESHOLD - parentCount) + 1;

    Entity parent = new Entity();
    parent.setName("Parent");
    for (int i = 0; i < parentCount; i++) {
      Property.StringProperty p = new Property.StringProperty();
      p.setName("parentField" + i);
      parent.getFields().add(p);
    }

    Entity child = new Entity();
    child.setName("Child");
    child.setSuperClass("com.example.Parent");
    child.setSuperEntity(parent);
    for (int i = 0; i < childCount; i++) {
      Property.StringProperty p = new Property.StringProperty();
      p.setName("childField" + i);
      child.getFields().add(p);
    }

    // Combined = DYNAMIC_UPDATE_FIELD_THRESHOLD + 1 (> threshold)
    assertTrue(
        hasDynamicUpdate(child),
        "Child should have @DynamicUpdate when combined column count exceeds threshold");
    // Parent alone has THRESHOLD / 2 (<= threshold), no large/binary field
    assertFalse(
        hasDynamicUpdate(parent),
        "Parent with fewer fields than threshold should not have @DynamicUpdate");
  }

  @Test
  void testInheritedBelowThreshold() {
    int parentCount = Entity.DYNAMIC_UPDATE_FIELD_THRESHOLD / 4;
    int childCount = Entity.DYNAMIC_UPDATE_FIELD_THRESHOLD / 4;

    Entity parent = new Entity();
    parent.setName("Parent");
    for (int i = 0; i < parentCount; i++) {
      Property.StringProperty p = new Property.StringProperty();
      p.setName("parentField" + i);
      parent.getFields().add(p);
    }

    Entity child = new Entity();
    child.setName("Child");
    child.setSuperClass("com.example.Parent");
    child.setSuperEntity(parent);
    for (int i = 0; i < childCount; i++) {
      Property.StringProperty p = new Property.StringProperty();
      p.setName("childField" + i);
      child.getFields().add(p);
    }

    // Combined = THRESHOLD / 2 (<= threshold)
    assertFalse(
        hasDynamicUpdate(child),
        "Child with combined fields below threshold should not have @DynamicUpdate");
  }

  @Test
  void testExplicitDynamicUpdateFalseOverrides() {
    Entity parent = new Entity();
    parent.setName("Parent");
    Property.StringProperty largeProp = new Property.StringProperty();
    largeProp.setName("largeText");
    largeProp.setLarge(true);
    parent.getFields().add(largeProp);

    Entity child = new Entity();
    child.setName("Child");
    child.setSuperClass("com.example.Parent");
    child.setSuperEntity(parent);
    child.setDynamicUpdate(false);

    Property.StringProperty titleProp = new Property.StringProperty();
    titleProp.setName("title");
    child.getFields().add(titleProp);

    assertFalse(
        hasDynamicUpdate(child), "Explicit dynamicUpdate=false must override auto-detection");
  }

  @Test
  void testMultiLevelInheritance() {
    Entity grandParent = new Entity();
    grandParent.setName("GrandParent");
    Property.StringProperty largeProp = new Property.StringProperty();
    largeProp.setName("content");
    largeProp.setLarge(true);
    grandParent.getFields().add(largeProp);

    Entity parent = new Entity();
    parent.setName("Parent");
    parent.setSuperClass("com.example.GrandParent");
    parent.setSuperEntity(grandParent);
    Property.StringProperty parentProp = new Property.StringProperty();
    parentProp.setName("parentField");
    parent.getFields().add(parentProp);

    Entity child = new Entity();
    child.setName("Child");
    child.setSuperClass("com.example.Parent");
    child.setSuperEntity(parent);
    Property.StringProperty childProp = new Property.StringProperty();
    childProp.setName("childField");
    child.getFields().add(childProp);

    assertTrue(hasDynamicUpdate(child), "Child should inherit @DynamicUpdate from GrandParent");
  }

  @Test
  void testNonColumnFieldsIgnored() {
    Entity parent = new Entity();
    parent.setName("Parent");

    // One-to-many collection
    Property.OneToManyProperty o2m = new Property.OneToManyProperty();
    o2m.setName("items");
    o2m.setMappedBy("parent");
    parent.getFields().add(o2m);

    // Transient field
    Property.StringProperty trans = new Property.StringProperty();
    trans.setName("tempData");
    trans.setTransient(true);
    trans.setLarge(true); // Large but transient - should NOT trigger @DynamicUpdate
    parent.getFields().add(trans);

    // Formula field
    Property.StringProperty formula = new Property.StringProperty();
    formula.setName("computed");
    formula.setFormula(true);
    formula.setLarge(true); // Large but formula - should NOT trigger @DynamicUpdate
    parent.getFields().add(formula);

    Entity child = new Entity();
    child.setName("Child");
    child.setSuperClass("com.example.Parent");
    child.setSuperEntity(parent);

    Property.StringProperty titleProp = new Property.StringProperty();
    titleProp.setName("title");
    child.getFields().add(titleProp);

    assertFalse(
        hasDynamicUpdate(child),
        "Non-column fields (collection, transient, formula) must be ignored");
  }

  @Test
  void testCycleSafety() {
    Entity a = new Entity();
    a.setName("A");
    Entity b = new Entity();
    b.setName("B");

    a.setSuperEntity(b);
    b.setSuperEntity(a);

    Property.StringProperty p1 = new Property.StringProperty();
    p1.setName("fieldA");
    a.getFields().add(p1);

    Property.StringProperty p2 = new Property.StringProperty();
    p2.setName("fieldB");
    b.getFields().add(p2);

    // Should complete without infinite recursion
    assertTimeoutPreemptively(
        Duration.ofMinutes(1),
        () -> assertFalse(hasDynamicUpdate(a)),
        "Infinite recursion detected");
  }
}
