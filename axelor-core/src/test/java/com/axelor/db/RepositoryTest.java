/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.axelor.JpaTest;
import com.axelor.JpaTestModule;
import com.axelor.inject.Beans;
import com.axelor.script.GroovyScriptHelper;
import com.axelor.script.ScriptBindings;
import com.axelor.test.GuiceModules;
import com.axelor.test.db.Contact;
import com.axelor.test.db.Person;
import com.axelor.test.db.repo.ContactRepository;
import com.axelor.test.db.repo.ContactRepositoryEx;
import jakarta.inject.Inject;
import jakarta.persistence.EntityNotFoundException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import org.hibernate.SessionFactory;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;

@GuiceModules(RepositoryTest.MyModule.class)
public class RepositoryTest extends JpaTest {

  public static class MyModule extends JpaTestModule {

    @Override
    protected void configure() {
      bind(ContactRepository.class).to(ContactRepositoryEx.class);
      super.configure();
    }
  }

  @Inject private ContactRepository contacts;

  @Test
  public void test() {
    assertNotNull(contacts);
    assertTrue(contacts instanceof ContactRepositoryEx);

    Query<Contact> q = contacts.all();
    assertNotNull(q);

    // test manual instantiation
    ContactRepository repo = Beans.get(ContactRepository.class);
    assertNotNull(repo);
    assertTrue(repo instanceof ContactRepositoryEx);

    // test manual instantiation by model class name
    JpaRepository<Contact> repo2 = JpaRepository.of(Contact.class);
    assertNotNull(repo2);
    assertTrue(repo2 instanceof ContactRepositoryEx);

    // test groovy expression
    ScriptBindings bindings = new ScriptBindings(new HashMap<String, Object>());
    GroovyScriptHelper helper = new GroovyScriptHelper(bindings);
    String expr = "__repo__(Contact)";
    Object obj = helper.eval(expr);
    assertNotNull(obj);
    assertTrue(obj instanceof ContactRepositoryEx);
  }

  @Test
  public void findByIds_returnPartialListOfEntities() {
    List<Contact> contacts =
        Beans.get(ContactRepository.class).findByIds(Arrays.asList(2L, 999L, 1L));
    assertNotNull(contacts);
    assertEquals(3, contacts.size());
    assertNull(contacts.get(1));
  }

  @Test
  public void findByIds_returnListOfEntities() {
    List<Contact> contacts = Beans.get(ContactRepository.class).findByIds(Arrays.asList(1L, 2L));
    assertNotNull(contacts);
    assertEquals(2, contacts.size());
    assertTrue(contacts.stream().allMatch(c -> c instanceof Contact));
  }

  @Test
  public void findById_whenIdIsNull_throwsException() {
    assertThrows(
        IllegalArgumentException.class, () -> Beans.get(ContactRepository.class).findById(null));
  }

  @Test
  public void findById_whenEntityMissing_returnEmptyOptional() {
    Long nonExistentId = 999999L;
    Optional<Contact> optEntity = Beans.get(ContactRepository.class).findById(nonExistentId);
    assertTrue(optEntity.isEmpty());
    assertThrows(NoSuchElementException.class, optEntity::get);
  }

  @Test
  public void findById_whenEntityExist_returnsEntityAndHitDatabase() {
    SessionFactory sessionFactory = JPA.em().getEntityManagerFactory().unwrap(SessionFactory.class);
    Statistics stats = sessionFactory.getStatistics();
    stats.setStatisticsEnabled(true);
    stats.clear();

    Optional<Contact> existId = Beans.get(ContactRepository.class).findById(1L);
    assertTrue(existId.isPresent());
    assertEquals(1L, (long) existId.get().getId());
    assertEquals(1, stats.getPrepareStatementCount());
  }

  @Test
  public void getReferenceById_whenIdIsNull_throwsException() {
    assertThrows(
        IllegalArgumentException.class,
        () -> Beans.get(ContactRepository.class).getReferenceById(null));
  }

  @Test
  public void getReferenceById_whenEntityMissing_returnsProxyAndThrowsOnAccess() {
    Long nonExistentId = 999999L;
    Contact emptyProxy = Beans.get(ContactRepository.class).getReferenceById(nonExistentId);
    assertInstanceOf(HibernateProxy.class, emptyProxy);
    assertThrows(EntityNotFoundException.class, emptyProxy::getFirstName);
  }

  @Test
  public void getReferenceById_whenEntityExist_returnsProxyAndHitDatabaseOnAccess() {
    SessionFactory sessionFactory = JPA.em().getEntityManagerFactory().unwrap(SessionFactory.class);
    Statistics stats = sessionFactory.getStatistics();
    stats.setStatisticsEnabled(true);
    stats.clear();

    // clear any cache
    JPA.em().clear();

    final Contact proxy = Beans.get(ContactRepository.class).getReferenceById(1L);
    assertTrue(((HibernateProxy) proxy).getHibernateLazyInitializer().isUninitialized());
    proxy.getId();
    assertTrue(((HibernateProxy) proxy).getHibernateLazyInitializer().isUninitialized());
    assertEquals(0, stats.getPrepareStatementCount());

    // will hit the database and initialize the proxy entity
    assertEquals("Mark", proxy.getFirstName());
    assertFalse(((HibernateProxy) proxy).getHibernateLazyInitializer().isUninitialized());
    assertEquals(1, stats.getPrepareStatementCount());

    JPA.runInTransaction(
        () -> {
          Person person = new Person();
          person.setCode(String.valueOf(ThreadLocalRandom.current().nextInt()));
          person.setContact(proxy);
          JPA.save(person);
          // one more query to retrieve the person id (with `nextval`)
          // but no extra select on contact
          assertEquals(2, stats.getPrepareStatementCount());
        });
  }

  @Test
  public void getReferenceById_whenEntityLoaded_returnsEntity() {
    // clear any cache
    JPA.em().clear();

    Contact findEntity = Beans.get(ContactRepository.class).find(1L);
    assertNotNull(findEntity);

    Contact refEntity = Beans.get(ContactRepository.class).getReferenceById(1L);
    assertInstanceOf(Contact.class, refEntity);
    assertEquals(findEntity, refEntity);
  }
}
