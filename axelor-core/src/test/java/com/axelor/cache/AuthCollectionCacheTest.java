/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.cache;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.axelor.JpaTest;
import com.axelor.JpaTestModule;
import com.axelor.TestingHelpers;
import com.axelor.app.AppSettings;
import com.axelor.app.AvailableAppSettings;
import com.axelor.auth.db.Group;
import com.axelor.auth.db.Permission;
import com.axelor.auth.db.Role;
import com.axelor.auth.db.User;
import com.axelor.db.JPA;
import com.axelor.db.hibernate.contributor.AuthCollectionsCacheContributor;
import com.axelor.inject.Beans;
import com.axelor.test.GuiceModules;
import com.google.inject.persist.UnitOfWork;
import jakarta.persistence.SharedCacheMode;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cache.jcache.ConfigSettings;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.stat.CollectionStatistics;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

/**
 * Verifies that the auth association collections enabled by {@link AuthCollectionsCacheContributor}
 * are served from the L2 collection cache, i.e. accessing them on a fresh session issues no
 * additional SQL against the link table.
 *
 * <p>Each test runs in its own session ({@link #doInSession}) so the first-level (session) cache
 * never masks a missing second-level cache hit.
 */
@GuiceModules(AuthCollectionCacheTest.AuthCacheTestModule.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AuthCollectionCacheTest extends JpaTest {

  /** Collection roles enabled by the contributor; kept in sync with it. */
  private static final List<String> CACHED_COLLECTION_ROLES =
      List.of(
          "com.axelor.auth.db.User.roles",
          "com.axelor.auth.db.User.permissions",
          "com.axelor.auth.db.Group.roles",
          "com.axelor.auth.db.Group.permissions",
          "com.axelor.auth.db.Role.permissions");

  private static Long userId;
  private static Long groupId;
  private static Long roleAId;
  private static Long roleBId;

  public static class AuthCacheTestModule extends JpaTestModule {

    @Override
    protected void configure() {
      TestingHelpers.resetSettings();

      final var props = AppSettings.get().getInternalProperties();
      props.put(ConfigSettings.PROVIDER, CacheConfig.DEFAULT_JCACHE_PROVIDER);
      props.put(
          AvailableAppSettings.JAVAX_PERSISTENCE_SHARED_CACHE_MODE,
          SharedCacheMode.ENABLE_SELECTIVE.toString());
      props.put(AvailableSettings.GENERATE_STATISTICS, Boolean.TRUE.toString());

      super.configure();
    }
  }

  @BeforeAll
  public static void createData() {
    doInSession(
        () ->
            JPA.runInTransaction(
                () -> {
                  final Permission read = permission("auth.cache.read", "com.axelor.auth.db.User");
                  final Permission write = permission("auth.cache.write", "com.axelor.auth.db.*");

                  final Role roleA = new Role("auth.cache.roleA");
                  roleA.addPermission(read);
                  final Role roleB = new Role("auth.cache.roleB");
                  roleB.addPermission(write);

                  final Group group = new Group("auth.cache.group", "Auth Cache Group");
                  group.addRole(roleA);
                  group.setPermissions(new HashSet<>(List.of(read)));

                  final User user = new User("auth.cache.user", "Auth Cache User");
                  user.setGroup(group);
                  user.setRoles(new HashSet<>(List.of(roleA, roleB)));
                  user.setPermissions(new HashSet<>(List.of(write)));

                  JPA.save(user);

                  userId = user.getId();
                  groupId = group.getId();
                  roleAId = roleA.getId();
                  roleBId = roleB.getId();
                }));
  }

  @AfterAll
  public static void clear() {
    TestingHelpers.resetSettings();
  }

  @Test
  @Order(1)
  public void collectionRegionsShouldBeRegistered() {
    doInSession(
        () -> {
          // Prime the regions: a region is created lazily on first access.
          touchAllCollections();

          final String[] regions =
              JPA.em()
                  .unwrap(Session.class)
                  .getSessionFactory()
                  .getStatistics()
                  .getSecondLevelCacheRegionNames();

          for (String role : CACHED_COLLECTION_ROLES) {
            assertTrue(
                Arrays.asList(regions).contains(role),
                () -> "Missing collection cache region: " + role);
          }
        });
  }

  @Test
  @Order(2)
  public void collectionsShouldBeServedFromCacheWithoutSql() {
    // 1. Start from a clean slate: drop L2 and reset stats.
    doInSession(
        () -> {
          final SessionFactory factory = JPA.em().unwrap(Session.class).getSessionFactory();
          factory.getCache().evictAll();
          factory.getStatistics().clear();
        });

    // 2. Prime: first access fetches from DB and populates the collection + entity caches.
    doInSession(AuthCollectionCacheTest::touchAllCollections);

    // 3. Fresh session, L2 kept but stats and L1 cleared: a second access must hit the cache.
    doInSession(
        () -> {
          final SessionFactory factory = JPA.em().unwrap(Session.class).getSessionFactory();
          final Statistics stats = factory.getStatistics();
          stats.clear();
          JPA.em().unwrap(Session.class).clear();

          touchAllCollections();

          for (String role : CACHED_COLLECTION_ROLES) {
            final CollectionStatistics cs = stats.getCollectionStatistics(role);
            assertTrue(cs.getCacheHitCount() > 0, () -> role + " should hit the collection cache");
            assertEquals(
                0, cs.getCacheMissCount(), () -> role + " should not miss the collection cache");
            assertEquals(
                0, cs.getFetchCount(), () -> role + " should not be fetched from the database");
          }

          // Everything touched (owner entities, target entities and collections) is cached,
          // so the whole traversal must not prepare a single JDBC statement.
          assertEquals(
              0,
              stats.getPrepareStatementCount(),
              "Reading cached auth associations should issue no SQL");
        });
  }

  /**
   * Loads the owning entities <b>by id</b> (so they resolve from the entity cache rather than via a
   * query, which would always hit the database) and iterates each cached collection so it is
   * resolved.
   */
  private static void touchAllCollections() {
    final User user = JPA.find(User.class, userId);
    user.getRoles().forEach(Role::getName);
    user.getPermissions().forEach(Permission::getName);

    final Group group = JPA.find(Group.class, groupId);
    group.getRoles().forEach(Role::getName);
    group.getPermissions().forEach(Permission::getName);

    for (Long roleId : List.of(roleAId, roleBId)) {
      JPA.find(Role.class, roleId).getPermissions().forEach(Permission::getName);
    }
  }

  private static Permission permission(String name, String object) {
    final Permission permission = new Permission();
    permission.setName(name);
    permission.setObject(object);
    permission.setCanRead(true);
    return permission;
  }

  static void doInSession(Runnable task) {
    final UnitOfWork unitOfWork = Beans.get(UnitOfWork.class);
    unitOfWork.begin();
    try {
      task.run();
    } finally {
      unitOfWork.end();
    }
  }
}
