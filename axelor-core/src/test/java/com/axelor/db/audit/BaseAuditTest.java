/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.db.audit;

import com.axelor.JpaTest;
import com.axelor.JpaTestModule;
import com.axelor.app.AppSettings;
import com.axelor.app.AvailableAppSettings;
import com.axelor.auth.db.User;
import com.axelor.db.Query;
import com.axelor.inject.Beans;
import com.axelor.meta.db.MetaSequence;
import com.axelor.test.GuiceModules;
import com.axelor.test.db.AuditCheck;
import com.google.inject.persist.Transactional;
import java.time.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.TestMethodOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@GuiceModules(BaseAuditTest.AuditTestModule.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class BaseAuditTest extends JpaTest {

  private static final Logger log = LoggerFactory.getLogger(BaseAuditTest.class);

  public static class AuditTestModule extends JpaTestModule {
    @Override
    protected void configure() {
      AppSettings.get()
          .getInternalProperties()
          .put(AvailableAppSettings.AUDIT_PROCESSOR_BUSY_BACKOFF_INTERVAL, "0");
      AppSettings.get()
          .getInternalProperties()
          .put(AvailableAppSettings.AUDIT_PROCESSOR_ACTIVITY_WINDOW, "0");
      super.configure();
      install(new AuditModule());
    }
  }

  @BeforeEach
  public void beforeAll() {
    if (Query.of(MetaSequence.class).count() == 0) {
      fixture("sequence-data.yml");
    }
    if (Query.of(User.class).count() == 0) {
      createUser();
    }
  }

  @AfterEach
  public void afterEach() {
    AuditQueue auditQueue = Beans.get(AuditQueue.class);
    if (auditQueue.getStatistics().pending() > 0) {
      log.info(
          "Waiting for audit queue to drain {} tasks...", auditQueue.getStatistics().pending());
    }

    int retry = 0;
    int maxRetry = 10;
    int pauseTime = 2000 / maxRetry;
    while (auditQueue.getStatistics().pending() > 0 && retry++ < maxRetry) {
      try {
        Thread.sleep(Duration.ofMillis(pauseTime));
      } catch (InterruptedException e) {
        // ignore
      }
    }
  }

  @Transactional
  protected void createUser() {
    User user = new User();
    user.setName("Administrator");
    user.setCode("admin");
    user.setPassword("password");
    getEntityManager().persist(user);
  }

  @Transactional
  protected void createEntity(String name, String email) {
    AuditCheck entity = new AuditCheck();
    entity.setName(name);
    entity.setEmail(email);
    getEntityManager().persist(entity);
  }

  @Transactional
  protected void updateEntity(AuditCheck entity, String name) {
    entity.setName(name);
    getEntityManager().persist(entity);
  }

  @Transactional
  protected void updateUser(User entity, String code) {
    entity.setCode(code);
    getEntityManager().persist(entity);
  }

  @Transactional
  protected void deleteUser(User entity) {
    getEntityManager().remove(entity);
  }
}
