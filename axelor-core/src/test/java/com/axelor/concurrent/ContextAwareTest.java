/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.concurrent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.axelor.JpaTest;
import com.axelor.TestingHelpers;
import com.axelor.auth.AuthService;
import com.axelor.auth.AuthUtils;
import com.axelor.auth.db.User;
import com.axelor.auth.db.repo.UserRepository;
import com.axelor.db.JPA;
import com.axelor.inject.Beans;
import com.axelor.test.fixture.Fixture;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class ContextAwareTest extends JpaTest {

  @BeforeAll
  public static void loadData() {
    JPA.runInTransaction(
        () -> {
          if (JPA.all(User.class).count() == 0) {
            try {
              Beans.get(Fixture.class)
                  .load(
                      "users-data.yml",
                      JPA::model,
                      bean -> {
                        if (!(bean instanceof User user)) {
                          return bean;
                        }
                        Beans.get(AuthService.class).encrypt(user);
                        return Beans.get(UserRepository.class).save(user);
                      });
            } catch (IOException e) {
              throw new RuntimeException(e);
            }
          }
        });
  }

  @Test
  public void testUser() throws Exception {
    // Log as admin user
    login("admin", "admin1234");

    assertEquals("admin", AuthUtils.getUser().getCode());

    // Check is admin is also the user in threads

    Runnable runnableAdmin =
        () -> {
          User user = AuthUtils.getUser();
          assertEquals(
              "admin",
              user.getCode(),
              "AuthUtils.getUser() doesn't return admin user but " + user.getCode());
        };

    try (ExecutorService executorService = Executors.newSingleThreadExecutor()) {
      executorService.submit(ContextAware.of().build(runnableAdmin)).get();
    }

    try (ExecutorService executorService = Executors.newSingleThreadExecutor()) {
      assertEquals(
          "admin",
          executorService
              .submit(ContextAware.of().build(() -> AuthUtils.getUser()))
              .get()
              .getCode());
    }

    // Run thread as demo user

    Runnable runnableDemo =
        () -> {
          User user = AuthUtils.getUser();
          assertEquals(
              "demo",
              user.getCode(),
              "AuthUtils.getUser() doesn't return demo user but " + user.getCode());
        };

    try (ExecutorService executorService = Executors.newSingleThreadExecutor()) {
      executorService
          .submit(ContextAware.of().withUser(AuthUtils.getUser("demo")).build(runnableDemo))
          .get();
    }

    try (ExecutorService executorService = Executors.newSingleThreadExecutor()) {
      assertEquals(
          "demo",
          executorService
              .submit(
                  ContextAware.of()
                      .withUser(AuthUtils.getUser("demo"))
                      .build(() -> AuthUtils.getUser()))
              .get()
              .getCode());
    }

    // log out admin user
    TestingHelpers.logout();

    // Check on recycled threads, all will be executed on same thread
    try (ExecutorService executorService = Executors.newSingleThreadExecutor()) {

      assertNull(AuthUtils.getUser());

      Runnable onNullUser =
          ContextAware.of()
              .build(
                  () -> {
                    User user = AuthUtils.getUser();
                    assertNull(
                        user, () -> "AuthUtils.getUser() should be null but " + user.getCode());
                  });

      executorService.submit(onNullUser).get();

      // re log user
      login("admin", "admin1234");

      Runnable myRunnable =
          ContextAware.of()
              .withUser(AuthUtils.getUser("demo"))
              .build(
                  () -> {
                    User user = AuthUtils.getUser();
                    assertEquals(
                        "demo",
                        user.getCode(),
                        "AuthUtils.getUser() doesn't return demo user but " + user.getCode());
                  });

      // run as demo
      executorService.submit(myRunnable).get();

      // run as admin
      Runnable onCurrentAdminUser =
          ContextAware.of()
              .build(
                  () -> {
                    User user = AuthUtils.getUser();
                    assertEquals(
                        "admin",
                        user.getCode(),
                        "AuthUtils.getUser() should be admin but " + user.getCode());
                  });

      executorService.submit(onCurrentAdminUser).get();
    }
  }

  @Test
  void testContextAwareRunnable() throws InterruptedException, ExecutionException {
    login("demo", "demo1234");

    try (var executor = Executors.newSingleThreadExecutor()) {
      var user = AuthUtils.getUser();
      var adminUser = AuthUtils.getUser("admin");

      Runnable ctxRunnable =
          ContextAware.of()
              .build(
                  () -> {
                    assertEquals(user, AuthUtils.getUser());
                  });

      Runnable ctxRunnableAdmin =
          ContextAware.of()
              .withUser(adminUser)
              .build(
                  () -> {
                    assertEquals(adminUser, AuthUtils.getUser());
                  });

      Runnable runnable =
          () -> {
            assertNull(AuthUtils.getUser());
          };

      var futures = new ArrayList<Future<?>>();

      for (var i = 0; i < 100; ++i) {
        futures.add(executor.submit(ctxRunnable));
        futures.add(executor.submit(ctxRunnableAdmin));
        futures.add(executor.submit(runnable));
      }

      for (var future : futures) {
        future.get();
      }
    } finally {
      TestingHelpers.logout();
    }
  }

  @Test
  void testContextAwareCallable() throws ExecutionException, InterruptedException {
    login("demo", "demo1234");

    try (var executor = Executors.newSingleThreadExecutor()) {
      var user = AuthUtils.getUser();
      var adminUser = AuthUtils.getUser("admin");

      Callable<User> ctxCallable =
          ContextAware.of()
              .build(
                  () -> {
                    return AuthUtils.getUser();
                  });

      Callable<User> ctxCallableAdmin =
          ContextAware.of()
              .withUser(adminUser)
              .build(
                  () -> {
                    return AuthUtils.getUser();
                  });

      Callable<User> callable =
          () -> {
            return AuthUtils.getUser();
          };

      var ctxFutures = new ArrayList<Future<User>>();
      var ctxFuturesAdmin = new ArrayList<Future<User>>();
      var futures = new ArrayList<Future<User>>();

      for (var i = 0; i < 100; ++i) {
        ctxFutures.add(executor.submit(ctxCallable));
        ctxFuturesAdmin.add(executor.submit(ctxCallableAdmin));
        futures.add(executor.submit(callable));
      }

      for (var future : ctxFutures) {
        assertEquals(user, future.get());
      }
      for (var future : ctxFuturesAdmin) {
        assertEquals(adminUser, future.get());
      }
      for (var future : futures) {
        assertNull(future.get());
      }
    } finally {
      TestingHelpers.logout();
    }
  }
}
