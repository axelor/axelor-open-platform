/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.auth;

import com.axelor.auth.db.UserToken;
import com.axelor.cache.AxelorCache;
import com.axelor.cache.CacheBuilder;
import com.axelor.common.UuidUtils;
import com.axelor.db.Query;
import com.google.inject.persist.Transactional;
import java.time.Duration;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service keeping track of when API keys are used.
 *
 * <p>API key authentication is stateless: it happens on every single request. Writing the {@code
 * lastUsedAt} of the API key each time turns a read-only workload into one {@code UPDATE} plus one
 * commit per request and invalidates the cache regions of {@link UserToken} just as often.
 *
 * <p>The timestamp is therefore only refreshed once per {@link #INTERVAL}, making it accurate to
 * within that interval. On top of that, a lease of {@link #LEASE_TIMEOUT} keeps concurrent requests
 * sharing an API key from all refreshing it at once.
 */
public class ApiKeyLastUsedService {

  /** Duration during which {@code lastUsedAt} is not refreshed again. */
  protected static final Duration INTERVAL = Duration.ofMinutes(5);

  /** Duration of the lease acquired to refresh {@code lastUsedAt}. */
  protected static final Duration LEASE_TIMEOUT = Duration.ofSeconds(30);

  /**
   * Leases acquired to refresh {@code lastUsedAt}, by token id.
   *
   * <p>A lease is deliberately never released: it expires on its own, so that requests still
   * holding an outdated {@code lastUsedAt} do not refresh it again right after.
   */
  private static final AxelorCache<Long, String> leases =
      CacheBuilder.newBuilder("leases").expireAfterWrite(LEASE_TIMEOUT).build();

  private static final Logger log = LoggerFactory.getLogger(ApiKeyLastUsedService.class);

  /**
   * Refreshes the {@code lastUsedAt} of the given token, unless it has been refreshed recently
   * enough.
   *
   * @param userToken the token being used
   */
  public void setLastUsed(UserToken userToken) {
    final LocalDateTime now = LocalDateTime.now();

    try {
      if (isOutdated(userToken, now) && tryAcquireLease(userToken)) {
        update(userToken, now);
      }
    } catch (Exception e) {
      log.error("Unable to refresh lastUsedAt of API key #{}", userToken.getId(), e);
    }
  }

  /**
   * Whether the {@code lastUsedAt} of the given token is outdated enough to be written again.
   *
   * @param userToken the token being used
   * @param now the current time
   * @return {@code true} if {@code lastUsedAt} is older than the interval
   */
  protected boolean isOutdated(UserToken userToken, LocalDateTime now) {
    final LocalDateTime lastUsedAt = userToken.getLastUsedAt();
    return lastUsedAt == null || !lastUsedAt.isAfter(now.minus(INTERVAL));
  }

  /**
   * Tries to acquire the lease allowing to refresh the {@code lastUsedAt} of the given token.
   *
   * <p>Once the interval has elapsed, concurrent requests using the same key all see the same
   * outdated value. Only the one acquiring the lease refreshes it, the others move on: they would
   * otherwise queue on the very same row. The lease is not released once the refresh is done, it
   * expires after {@link #LEASE_TIMEOUT}, which also covers the requests that read {@code
   * lastUsedAt} before the refresh but reach this point after it.
   *
   * <p>The lease is always acquired, so that {@code lastUsedAt} is never refreshed more than once
   * per {@link #LEASE_TIMEOUT}. It is the last guard against a burst of requests hammering the very
   * same row.
   *
   * @param userToken the token being used
   * @return {@code true} if the lease was acquired
   */
  protected boolean tryAcquireLease(UserToken userToken) {
    final String lease = UuidUtils.v4().toString();
    return lease.equals(leases.get(userToken.getId(), key -> lease));
  }

  /**
   * Writes the {@code lastUsedAt} of the given token.
   *
   * <p>This performs a non-versioned update: {@code version}, {@code updatedOn} and {@code
   * updatedBy} are intentionally left untouched, as using an API key is not a modification of the
   * token. Keeping {@code version} out of it also means concurrent requests sharing a key can no
   * longer make each other fail with an optimistic lock error.
   *
   * @param userToken the token being used
   * @param lastUsedAt the value to write
   */
  @Transactional
  protected void update(UserToken userToken, LocalDateTime lastUsedAt) {
    Query.of(UserToken.class)
        .filter("self.id = :id")
        .bind("id", userToken.getId())
        .update("lastUsedAt", lastUsedAt, null);
  }
}
