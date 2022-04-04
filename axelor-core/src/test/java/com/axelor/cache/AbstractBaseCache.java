package com.axelor.cache;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.axelor.JpaTest;
import com.axelor.JpaTestModule;
import com.axelor.app.AppSettings;
import com.axelor.app.AvailableAppSettings;
import com.axelor.common.ObjectUtils;
import com.axelor.db.JPA;
import com.axelor.inject.Beans;
import com.axelor.test.db.Contact;
import com.axelor.test.db.Person;
import com.axelor.test.db.repo.PersonRepository;
import com.google.inject.persist.UnitOfWork;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLong;
import javax.persistence.PersistenceException;
import javax.persistence.SharedCacheMode;
import javax.validation.ValidationException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.stat.CacheRegionStatistics;
import org.hibernate.stat.EntityStatistics;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

/** Run in different sessions to avoid first level of cache (associated to the session) */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public abstract class AbstractBaseCache extends JpaTest {

  public static class CacheTestModule extends JpaTestModule {

    @Override
    protected void configure() {
      // Enable cache as `ENABLE_SELECTIVE`
      AppSettings.get()
          .getProperties()
          .put(
              AvailableAppSettings.JAVAX_PERSISTENCE_SHAREDCACHE_MODE,
              SharedCacheMode.ENABLE_SELECTIVE.toString());
      // Enable statistics
      AppSettings.get()
          .getProperties()
          .put(AvailableSettings.GENERATE_STATISTICS, Boolean.TRUE.toString());
      // Use `read-write` cache strategy
      AppSettings.get()
          .getProperties()
          .put(
              AvailableSettings.DEFAULT_CACHE_CONCURRENCY_STRATEGY,
              AccessType.READ_WRITE.getExternalName());

      // Avoid warning `created-on-the-fly`
      AppSettings.get()
          .getProperties()
          .put("hibernate.javax.cache.missing_cache_strategy", "create");

      super.configure();
    }
  }

  @BeforeAll
  public static void doInit() {
    doInSession(
        () -> {
          JPA.runInTransaction(
              () -> {
                Person aPerson = new Person();
                aPerson.setName("John Doe");
                aPerson.setCode("my-unique-code");
                JPA.save(aPerson);
              });
        });
  }

  @AfterAll
  public static void clear() {
    resetSettings();
  }

  @Test
  @Order(1)
  public void cacheShouldBeEnabled() {
    doInSession(
        () -> {
          SessionFactory sessionFactory = JPA.em().unwrap(Session.class).getSessionFactory();
          assertTrue(sessionFactory.getSessionFactoryOptions().isSecondLevelCacheEnabled());
          assertTrue(sessionFactory.getSessionFactoryOptions().isQueryCacheEnabled());

          String[] secondLevelCacheRegionNames =
              sessionFactory.getStatistics().getSecondLevelCacheRegionNames();
          assertTrue(ObjectUtils.notEmpty(secondLevelCacheRegionNames));

          // Should contain `default-update-timestamps-region` region
          assertTrue(
              Arrays.asList(secondLevelCacheRegionNames)
                  .contains("default-update-timestamps-region"));

          // Person is annotated with `@Cacheable`
          assertTrue(
              Arrays.stream(secondLevelCacheRegionNames)
                  .anyMatch(it -> Person.class.getName().equals(it)));

          // Contact is NOT annotated with `@Cacheable`
          assertFalse(
              Arrays.stream(secondLevelCacheRegionNames)
                  .anyMatch(it -> Contact.class.getName().equals(it)));
        });
  }

  @Test
  @Order(2)
  public void shouldHitQueryCache() {
    doInSession(this::clearStats);

    final AtomicLong aPersonId = new AtomicLong();

    doInSession(
        () -> {
          JPA.runInTransaction(
              () -> {
                Person aPerson = new Person();
                aPerson.setName("John Doe");
                aPerson.setCode("unique-code");
                JPA.save(aPerson);
                aPersonId.set(aPerson.getId());
              });
        });

    // Should NOT hit query cache because this is first run
    doInSession(
        () -> {
          Beans.get(PersonRepository.class).findByCode("unique-code");

          Statistics statistics =
              JPA.em().unwrap(Session.class).getSessionFactory().getStatistics();
          assertEquals(0, statistics.getQueryCacheHitCount());
          assertEquals(1, statistics.getQueryCacheMissCount());
        });

    // Should hit query cache because of second run
    doInSession(
        () -> {
          Beans.get(PersonRepository.class).findByCode("unique-code");

          SessionFactory factory = JPA.em().unwrap(Session.class).getSessionFactory();
          Statistics statistics = factory.getStatistics();
          assertEquals(1, statistics.getQueryCacheHitCount());
          assertEquals(1, statistics.getQueryCacheMissCount());
        });
  }

  @Test
  @Order(3)
  public void shouldHitEntityCache() {
    doInSession(this::clearStats);

    final AtomicLong aPersonId = new AtomicLong();

    doInSession(
        () -> {
          JPA.runInTransaction(
              () -> {
                Person aPerson = new Person();
                aPerson.setName("John Doe 2");
                aPerson.setCode("unique-code2");
                aPerson.setContact(JPA.all(Contact.class).fetchOne());
                JPA.save(aPerson);
                aPersonId.set(aPerson.getId());
              });
        });

    doInSession(
        () -> {
          CacheRegionStatistics regionStatistics =
              JPA.em()
                  .unwrap(Session.class)
                  .getSessionFactory()
                  .getStatistics()
                  .getCacheRegionStatistics(Person.class.getName());
          assertEquals(0, regionStatistics.getHitCount());
          assertEquals(1, regionStatistics.getPutCount());
        });

    // Should hit query cache after saving entity
    doInSession(
        () -> {
          JPA.find(Person.class, aPersonId.get());

          SessionFactory factory = JPA.em().unwrap(Session.class).getSessionFactory();
          CacheRegionStatistics regionStatistics =
              factory.getStatistics().getCacheRegionStatistics(Person.class.getName());
          assertEquals(1, regionStatistics.getHitCount());
          assertEquals(1, regionStatistics.getPutCount());
          assertTrue(factory.getCache().contains(Person.class, aPersonId.get()));
        });
  }

  @Test
  @Order(4)
  public void shouldNotHitCacheOnConstraintViolationException() {
    doInSession(this::clearStats);

    final AtomicLong aPersonId = new AtomicLong();

    // try to insert an existing person (duplicated code)
    try {
      doInSession(
          () -> {
            JPA.runInTransaction(
                () -> {
                  Person aPerson = new Person();
                  aPerson.setName("John Doe 2");
                  aPerson.setCode("my-unique-code");
                  JPA.save(aPerson);
                  aPersonId.set(aPerson.getId());
                });
          });
      fail("Should trigger ConstraintViolationException : not unique `code`");
    } catch (PersistenceException e) {
      // ignore
    }

    // Should have nothing in cache
    doInSession(
        () -> {
          SessionFactory factory = JPA.em().unwrap(Session.class).getSessionFactory();
          EntityStatistics statistics =
              factory.getStatistics().getEntityStatistics(Person.class.getName());
          assertEquals(0, statistics.getInsertCount());
          assertEquals(0, statistics.getFetchCount());
          assertFalse(factory.getCache().contains(Person.class, aPersonId.get()));
        });
  }

  @Test
  @Order(5)
  public void shouldNotHitCacheOnRepositoryException() {
    doInSession(this::clearStats);

    final AtomicLong aPersonId = new AtomicLong();

    // Add a new person
    doInSession(
        () -> {
          JPA.runInTransaction(
              () -> {
                Person aPerson = new Person();
                aPerson.setName("John Doe 2");
                aPerson.setCode("my-unique-code2");
                JPA.save(aPerson);
                aPersonId.set(aPerson.getId());
              });
        });

    // try update previous person with a non-unique code from repository
    try {
      doInSession(
          () -> {
            JPA.runInTransaction(
                () -> {
                  Person aPerson = JPA.find(Person.class, aPersonId.get());
                  aPerson.setName("hello");
                  Beans.get(PersonRepository.class).save(aPerson);
                });
          });
      fail("Should trigger ValidationException : see in PersonRepository");
    } catch (ValidationException e) {
      // ignore
    }

    doInSession(
        () -> {
          SessionFactory factory = JPA.em().unwrap(Session.class).getSessionFactory();
          EntityStatistics statistics =
              factory.getStatistics().getEntityStatistics(Person.class.getName());

          // should be in cache when inserting entity
          assertEquals(1, statistics.getInsertCount());
          assertEquals(0, statistics.getFetchCount());
          assertEquals(1, factory.getStatistics().getSecondLevelCacheHitCount());
          assertTrue(factory.getCache().contains(Person.class, aPersonId.get()));

          // will fetch from 2lv cache, check if data have been rollback
          Person person = JPA.find(Person.class, aPersonId.get());
          assertEquals("my-unique-code2", person.getCode());
          assertEquals("John Doe 2", person.getName());
        });
  }

  static void doInSession(Runnable task) {
    UnitOfWork unitOfWork = Beans.get(UnitOfWork.class);
    unitOfWork.begin();
    try {
      task.run();
    } finally {
      unitOfWork.end();
    }
  }

  void clearStats() {
    Session session = JPA.em().unwrap(Session.class);
    SessionFactory sessionFactory = session.getSessionFactory();
    Statistics statistics = sessionFactory.getStatistics();

    sessionFactory.getCache().evictAll();
    statistics.clear();
    session.clear();
  }
}
