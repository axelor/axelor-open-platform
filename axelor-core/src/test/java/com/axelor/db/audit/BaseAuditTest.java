/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.db.audit;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.axelor.JpaTest;
import com.axelor.JpaTestModule;
import com.axelor.app.AppSettings;
import com.axelor.app.AvailableAppSettings;
import com.axelor.auth.AuthUtils;
import com.axelor.auth.db.User;
import com.axelor.concurrent.ContextAware;
import com.axelor.db.JPA;
import com.axelor.db.Query;
import com.axelor.inject.Beans;
import com.axelor.mail.db.MailMessage;
import com.axelor.meta.db.MetaSequence;
import com.axelor.test.db.AuditCheck;
import com.axelor.test.db.Contact;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.persist.Transactional;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.TestMethodOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class BaseAuditTest extends JpaTest {

  private static final Logger log = LoggerFactory.getLogger(BaseAuditTest.class);

  /**
   * Base test module that configures audit settings but does not install the {@link AuditModule}.
   */
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
    }
  }

  /** Test module that additionally installs the {@link AuditModule}. */
  public static class AuditTestModuleWithAudit extends AuditTestModule {
    @Override
    protected void configure() {
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

    int pauseTimeMillis = 2000;
    int maxWaitingMinutes = 5;
    long currentMillis = System.currentTimeMillis();
    // Wait to drain the queue or timeout
    while ((auditQueue.getStatistics().pending() > 0 || auditQueue.getStatistics().isActive())
        && Duration.ofMillis(System.currentTimeMillis() - currentMillis).toMinutes()
            < maxWaitingMinutes) {
      try {
        Thread.sleep(Duration.ofMillis(pauseTimeMillis));
        log.info(
            "Waiting for audit queue to drain {} tasks...",
            (auditQueue.getStatistics().pending() + 1));
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        break;
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

  @Transactional
  protected Contact createContact(String firstName, String lastName) {
    var user = new Contact(firstName, lastName);
    getEntityManager().persist(user);
    return user;
  }

  @Transactional
  protected Contact updateContact(Contact entity, String firstName, String lastName) {
    entity.setFirstName(firstName);
    entity.setLastName(lastName);
    getEntityManager().persist(entity);
    return entity;
  }

  @Transactional
  protected Long createTracked(String name) {
    var entity = new AuditCheck();
    entity.setName(name);
    getEntityManager().persist(entity);
    return entity.getId();
  }

  protected void processAuditLogs() {
    JPA.clear();
    new AuditProcessor().process();
    JPA.clear();
  }

  protected MailMessage lastMessage(Long entityId) {
    return lastMessage(AuditCheck.class, entityId);
  }

  protected MailMessage lastMessage(Class<?> model, Long entityId) {
    var message =
        Query.of(MailMessage.class)
            .filter("self.relatedModel = :model AND self.relatedId = :id")
            .bind("model", model.getName())
            .bind("id", entityId)
            .order("-id")
            .fetchOne();
    assertNotNull(message, "no tracking message was created");
    return message;
  }

  protected Map<String, Map<String, String>> tracksOf(MailMessage message) throws Exception {
    Map<String, Object> body =
        Beans.get(ObjectMapper.class)
            .readValue(message.getBody(), new TypeReference<Map<String, Object>>() {});

    @SuppressWarnings("unchecked")
    var tracks = (List<Map<String, String>>) body.get("tracks");
    return tracks == null
        ? Map.of()
        // Deduplicate fields declared multiple times in <track>.
        : tracks.stream()
            .collect(Collectors.toMap(item -> item.get("name"), item -> item, (a, b) -> a));
  }

  protected <T> T asAdmin(Callable<T> job) throws Exception {
    return ContextAware.of()
        .withTransaction(false)
        .withUser(AuthUtils.getUser("admin"))
        .build(job)
        .call();
  }
}
