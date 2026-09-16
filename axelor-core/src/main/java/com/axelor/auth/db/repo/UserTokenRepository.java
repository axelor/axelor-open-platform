/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.auth.db.repo;

import com.axelor.auth.db.UserToken;
import com.axelor.common.net.IpAddressMatcher;
import com.axelor.db.Query;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class UserTokenRepository extends AbstractUserTokenRepository {

  private static final ProtectedFieldsProcessor protectedFieldsProcessor =
      new ProtectedFieldsProcessor(Set.of("tokenKey", "tokenDigest"));

  public UserToken findByKey(String key) {
    return Query.of(UserToken.class)
        .filter("self.tokenKey = :key")
        .bind("key", key)
        .cacheable()
        .fetchOne();
  }

  /**
   * Saves the API key, normalizing its comma-separated allowed IPs.
   *
   * @throws IllegalArgumentException if any allowed IP is not a valid IP address or CIDR range
   */
  @Override
  public UserToken save(UserToken entity) {
    entity.setAllowedIps(normalizeAllowedIps(entity.getAllowedIps()));
    return super.save(entity);
  }

  /**
   * Normalizes a comma-separated list of IP addresses or CIDR ranges, removing duplicates.
   *
   * @param allowedIps comma-separated IP addresses or CIDR ranges
   * @return the normalized list, separated by {@code ", "}, or {@code null} if empty
   * @throws IllegalArgumentException if any entry is not a valid IP address or CIDR range
   */
  public static String normalizeAllowedIps(String allowedIps) {
    final String normalized =
        IpAddressMatcher.parseList(allowedIps).stream()
            .map(IpAddressMatcher::toString)
            .distinct()
            .collect(Collectors.joining(", "));
    return normalized.isEmpty() ? null : normalized;
  }

  @Override
  public Map<String, Object> populate(Map<String, Object> json, Map<String, Object> context) {
    return protectedFieldsProcessor.populate(json, context);
  }

  @Override
  public Map<String, Object> validate(Map<String, Object> json, Map<String, Object> context) {
    return protectedFieldsProcessor.validate(json, context);
  }
}
