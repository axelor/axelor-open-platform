/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.axelor.JpaTest;
import com.axelor.auth.db.User;
import com.axelor.auth.db.UserToken;
import com.axelor.auth.db.repo.UserRepository;
import com.axelor.auth.db.repo.UserTokenRepository;
import com.google.inject.persist.Transactional;
import jakarta.inject.Inject;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

public class ApiKeyLastUsedServiceTest extends JpaTest {

  @Inject private ApiKeyLastUsedService lastUsedService;

  @Inject private AuthService authService;

  @Inject private UserRepository users;

  @Inject private UserTokenRepository userTokens;

  @Test
  public void shouldBeOutdatedWhenNeverUsed() {
    UserToken token = new UserToken();
    assertTrue(lastUsedService.isOutdated(token, LocalDateTime.now()));
  }

  @Test
  public void shouldNotBeOutdatedWithinInterval() {
    LocalDateTime now = LocalDateTime.now();
    UserToken token = new UserToken();
    token.setLastUsedAt(now.minusMinutes(1));

    assertFalse(lastUsedService.isOutdated(token, now));
  }

  @Test
  public void shouldBeOutdatedAfterInterval() {
    LocalDateTime now = LocalDateTime.now();
    UserToken token = new UserToken();
    token.setLastUsedAt(now.minus(ApiKeyLastUsedService.INTERVAL).minusMinutes(1));

    assertTrue(lastUsedService.isOutdated(token, now));
  }

  @Test
  public void shouldWriteWithoutBumpingVersionOrAuditFields() {
    UserToken token = createToken(LocalDateTime.now().minusHours(1));
    Long id = token.getId();
    Integer version = token.getVersion();
    LocalDateTime now = LocalDateTime.now();

    lastUsedService.setLastUsed(token);

    UserToken reloaded = reload(id);
    assertFalse(reloaded.getLastUsedAt().isBefore(now), "lastUsedAt should have been refreshed");
    assertEquals(version, reloaded.getVersion(), "version should not have been bumped");
    assertNull(reloaded.getUpdatedOn(), "updatedOn should not have been stamped");
    assertNull(reloaded.getUpdatedBy(), "updatedBy should not have been stamped");
  }

  @Test
  public void shouldNotWriteWithinInterval() {
    LocalDateTime lastUsedAt = LocalDateTime.now().minusMinutes(1).withNano(0);
    UserToken token = createToken(lastUsedAt);
    Long id = token.getId();

    lastUsedService.setLastUsed(token);

    assertEquals(lastUsedAt, reload(id).getLastUsedAt(), "lastUsedAt should have been left alone");
  }

  @Test
  public void shouldNotRefreshTwiceWithinLease() {
    UserToken token = createToken(LocalDateTime.now().minusHours(1));
    Long id = token.getId();

    lastUsedService.setLastUsed(token);
    LocalDateTime refreshedAt = reload(id).getLastUsedAt();

    // the given instance still holds the outdated value, as a concurrent request would
    lastUsedService.setLastUsed(token);

    assertEquals(
        refreshedAt, reload(id).getLastUsedAt(), "lease should have held off the second refresh");
  }

  @Test
  public void shouldLeasePerToken() {
    UserToken token = createToken(LocalDateTime.now().minusHours(1));
    UserToken other = createToken(LocalDateTime.now().minusHours(1));
    LocalDateTime now = LocalDateTime.now();

    lastUsedService.setLastUsed(token);
    lastUsedService.setLastUsed(other);

    assertFalse(reload(token.getId()).getLastUsedAt().isBefore(now));
    assertFalse(reload(other.getId()).getLastUsedAt().isBefore(now));
  }

  @Test
  public void shouldNotLeaveCachedTokenStale() {
    LocalDateTime lastUsedAt = LocalDateTime.now().minusHours(1).withNano(0);
    UserToken token = createToken(lastUsedAt);
    String key = token.getTokenKey();
    LocalDateTime now = LocalDateTime.now();

    // warm up the cacheable lookup used by the authenticator
    assertEquals(lastUsedAt, userTokens.findByKey(key).getLastUsedAt());

    lastUsedService.setLastUsed(token);

    getEntityManager().clear();
    assertFalse(
        userTokens.findByKey(key).getLastUsedAt().isBefore(now),
        "cached lookup should not serve a stale lastUsedAt");
  }

  private UserToken reload(Long id) {
    // the update bypasses the persistence context on purpose, so drop the stale copy
    getEntityManager().clear();
    return userTokens.find(id);
  }

  @Transactional
  protected UserToken createToken(LocalDateTime lastUsedAt) {
    User owner = users.findByCode("api-key-owner");

    if (owner == null) {
      owner = new User("api-key-owner", "API Key Owner");
      owner.setPassword("secret");
      authService.encrypt(owner);
      owner = users.save(owner);
    }

    UserToken token = new UserToken();
    token.setName("test");
    token.setOwner(owner);
    token.setExpiresAt(LocalDateTime.now().plusDays(1));
    token.setTokenKey("key-" + System.nanoTime());
    token.setTokenDigest("digest");
    token.setLastUsedAt(lastUsedAt);

    return userTokens.save(token);
  }
}
