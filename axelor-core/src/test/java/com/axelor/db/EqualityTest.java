/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2022 Axelor (<http://axelor.com>).
 *
 * This program is free software: you can redistribute it and/or  modify
 * it under the terms of the GNU Affero General Public License, version 3,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.axelor.db;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import com.axelor.JpaTest;
import com.axelor.test.db.Address;
import com.axelor.test.db.Contact;
import com.google.common.collect.ImmutableList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import javax.persistence.EntityManager;
import org.hibernate.Session;
import org.junit.Test;

/**
 * Test entity equality
 *
 * <p>Equality should behave the same, whether entities have unique fields or not.
 *
 * <p>Address has no unique fields, while Contact has a unique field.
 */
public class EqualityTest extends JpaTest {

  private final List<Class<? extends Model>> modelClasses =
      ImmutableList.of(Address.class, Contact.class);

  @Test
  public void testNewInstanceNotEquals() throws Exception {
    for (Class<? extends Model> modelClass : modelClasses) {
      Model entity1 = modelClass.getDeclaredConstructor().newInstance();
      Model entity2 = modelClass.getDeclaredConstructor().newInstance();
      assertNotEquals(
          String.format(
              "Two new empty instances of %s should not be equal.", modelClass.getSimpleName()),
          entity1,
          entity2);
    }
  }

  @Test
  public void testModelSet() throws Exception {
    for (Class<? extends Model> modelClass : modelClasses) {
      Set<Model> entities = new HashSet<>();
      Model entity1 = modelClass.getDeclaredConstructor().newInstance();
      Model entity2 = modelClass.getDeclaredConstructor().newInstance();
      entities.add(entity1);
      entities.add(entity2);
      assertTrue(
          String.format("Set should contain added instance of %s.", modelClass.getSimpleName()),
          entities.contains(entity1));
      assertTrue(
          String.format("Set should contain added instance of %s.", modelClass.getSimpleName()),
          entities.contains(entity2));
      entities.remove(entity1);
      assertFalse(
          String.format(
              "Set should not contain removed instance of %s.", modelClass.getSimpleName()),
          entities.contains(entity1));
      assertTrue(
          String.format("Set should contain added instance of %s.", modelClass.getSimpleName()),
          entities.contains(entity2));
      entities.remove(entity2);
      assertFalse(
          String.format(
              "Set should not contain removed instance of %s.", modelClass.getSimpleName()),
          entities.contains(entity2));
    }
  }

  @Test
  public void testModelMap() throws Exception {
    for (Class<? extends Model> modelClass : ImmutableList.of(Address.class, Contact.class)) {
      Map<Model, Model> entities = new HashMap<>();
      Model entity1 = modelClass.getDeclaredConstructor().newInstance();
      Model entity2 = modelClass.getDeclaredConstructor().newInstance();
      entities.put(entity1, entity1);
      entities.put(entity2, entity2);
      assertSame(
          String.format(
              "Should retrieve same instance of %s from map.", modelClass.getSimpleName()),
          entity1,
          entities.get(entity1));
      assertSame(
          String.format(
              "Should retrieve same instance of %s from map.", modelClass.getSimpleName()),
          entity2,
          entities.get(entity2));
      entities.remove(entity1);
      assertSame(
          String.format(
              "Should not find removed instance of %s from map.", modelClass.getSimpleName()),
          null,
          entities.get(entity1));
      assertSame(
          String.format(
              "Should retrieve same instandce of %s from map.", modelClass.getSimpleName()),
          entity2,
          entities.get(entity2));
      entities.remove(entity2);
      assertSame(
          String.format(
              "Should not find removed instance of %s from map.", modelClass.getSimpleName()),
          null,
          entities.get(entity2));
    }
  }

  @Test
  public void testContactNewInstanceWithUniqueNotEquals() {
    Contact entity1 = new Contact();
    Contact entity2 = new Contact();
    entity1.setUniqueName("John");
    entity2.setUniqueName("James");
    assertNotEquals(
        "Entity instances having different unique fields should not be equal.", entity1, entity2);
  }

  @Test
  public void testContactNewInstanceWithUniqueEquals() {
    Contact entity1 = new Contact();
    Contact entity2 = new Contact();
    entity1.setUniqueName("John");
    entity2.setUniqueName("John");
    assertEquals("Entity instances having same unique fields should be equal.", entity1, entity2);
    assertEquals(
        "Entity instances that are equal should have same hash code.",
        entity1.hashCode(),
        entity2.hashCode());
  }

  @Test
  public void testContactChangeUnique() {
    Set<Contact> contacts = new HashSet<>();
    Contact c1 = new Contact();

    assertFalse(contacts.contains(c1));
    contacts.add(c1);
    assertTrue(contacts.contains(c1));

    c1.setUniqueName("John");
    assertTrue(contacts.contains(c1));
  }

  @Test
  public void testContactWithUniqueMerge() {
    EntityManager em = getEntityManager();

    Contact c1 = em.find(Contact.class, 1L);
    em.detach(c1);
    c1.setUniqueName(null); // make sure the unique name is null

    em.clear();

    assertFalse(em.contains(c1)); // c1 is detached

    Set<Contact> contacts = new HashSet<>();
    contacts.add(c1);
    assertTrue(contacts.contains(c1));

    c1 = em.merge(c1); // merge returns a new instance

    assertTrue(contacts.contains(c1));
  }

  @Test
  public void testEntityHelperEquals() {
    Address address1 = new Address();
    Address address2 = new Address();
    assertEquals(address1.equals(address2), EntityHelper.equals(address1, address2));
    address1.setId(1L);
    assertEquals(address1.equals(address2), EntityHelper.equals(address1, address2));
    address2.setId(1L);
    assertEquals(address1.equals(address2), EntityHelper.equals(address1, address2));
    address1.setId(2L);
    assertEquals(address1.equals(address2), EntityHelper.equals(address1, address2));
    address2.setId(2L);
    assertEquals(address1.equals(address2), EntityHelper.equals(address1, address2));

    Contact contact1 = new Contact();
    Contact contact2 = new Contact();
    assertEquals(contact1.equals(contact2), EntityHelper.equals(contact1, contact2));
    contact1.setUniqueName("John");
    assertEquals(contact1.equals(contact2), EntityHelper.equals(contact1, contact2));
    contact2.setUniqueName("John");
    assertEquals(contact1.equals(contact2), EntityHelper.equals(contact1, contact2));
    contact1.setUniqueName("Jane");
    assertEquals(contact1.equals(contact2), EntityHelper.equals(contact1, contact2));
    contact2.setUniqueName("Jane");
    assertEquals(contact1.equals(contact2), EntityHelper.equals(contact1, contact2));
  }

  @Test
  public void testEntityHelperHashCode() {
    Address address = new Address();
    assertEquals(address.hashCode(), EntityHelper.hashCode(address));

    Contact contact1 = new Contact();
    Contact contact2 = new Contact();
    contact1.setUniqueName("John");
    contact2.setUniqueName("Jane");
    contact1.setUUID(UUID.randomUUID().toString());
    contact2.setUUID(UUID.randomUUID().toString());
    assertEquals(contact1.hashCode(), EntityHelper.hashCode(contact1));
    assertEquals(contact2.hashCode(), EntityHelper.hashCode(contact2));

    Set<Contact> contacts = new HashSet<>();
    contacts.add(contact1);
    contacts.add(contact2);
    assertTrue(contacts.contains(contact1));
    assertTrue(contacts.contains(contact2));

    contacts.remove(contact1);
    assertFalse(contacts.contains(contact1));
    assertTrue(contacts.contains(contact2));

    // Change of fields used for equality test should not affect Set.
    contact1.setUniqueName("James");
    contact2.setUniqueName("Mary");
    assertFalse(contacts.contains(contact1));
    assertTrue(contacts.contains(contact2));

    // Change of fields used for hash code will affect Set.
    contact1.setUUID(null);
    contact2.setUUID(null);
    assertFalse(contacts.contains(contact1));
    assertFalse(contacts.contains(contact2));
  }

  @Test
  public void testEqualityConsistencyContact() {
    Contact contact = new Contact("John", "Doe");
    assertEqualityConsistency(Contact.class, contact);
  }

  @Test
  public void testEqualityConsistencyAddress() {
    Contact contact = new Contact("John", "Doe");
    Address address = new Address("street", "area", "city");
    address.setZip("zip");
    address.setContact(contact);
    assertEqualityConsistency(Address.class, address);
  }

  protected <T extends Model> void assertEqualityConsistency(Class<T> clazz, T entity) {
    Set<T> tuples = new HashSet<>();

    assertFalse(tuples.contains(entity));
    tuples.add(entity);
    assertTrue(tuples.contains(entity));

    inTransaction(
        () -> {
          getEntityManager().persist(entity);
          getEntityManager().flush();
          assertTrue(
              "The entity is not found in the Set after it's persisted.", tuples.contains(entity));
        });

    assertTrue(tuples.contains(entity));

    inTransaction(
        () -> {
          T entityProxy = getEntityManager().getReference(clazz, entity.getId());
          assertTrue("The entity proxy is not equal with the entity.", entityProxy.equals(entity));
        });

    inTransaction(
        () -> {
          T entityProxy = getEntityManager().getReference(clazz, entity.getId());
          assertTrue("The entity is not equal with the entity proxy.", entity.equals(entityProxy));
        });

    inTransaction(
        () -> {
          T _entity = getEntityManager().merge(entity);
          assertTrue(
              "The entity is not found in the Set after it's merged.", tuples.contains(_entity));
        });

    inTransaction(
        () -> {
          getEntityManager().unwrap(Session.class).update(entity);
          assertTrue(
              "The entity is not found in the Set after it's reattached.", tuples.contains(entity));
        });

    inTransaction(
        () -> {
          T _entity = getEntityManager().find(clazz, entity.getId());
          assertTrue(
              "The entity is not found in the Set after it's loaded in a different Persistence Context.",
              tuples.contains(_entity));
        });

    inTransaction(
        () -> {
          T _entity = getEntityManager().getReference(clazz, entity.getId());
          assertTrue(
              "The entity is not found in the Set after it's loaded as a proxy in a different Persistence Context.",
              tuples.contains(_entity));
        });

    T deletedEntity =
        inTransaction(
            () -> {
              T _entity = getEntityManager().getReference(clazz, entity.getId());
              getEntityManager().remove(_entity);
              return _entity;
            });

    assertTrue(
        "The entity is not found in the Set even after it's deleted.",
        tuples.contains(deletedEntity));
  }

  protected <T> T inTransaction(Supplier<T> supplier) {
    final JpaResult<T> jpaResult = new JpaResult<>();
    inTransaction(
        () -> {
          jpaResult.result = supplier.get();
        });
    return jpaResult.result;
  }

  private static class JpaResult<T> {
    private T result;
  }
}
