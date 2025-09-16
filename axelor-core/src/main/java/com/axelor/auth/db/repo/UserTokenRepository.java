/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.auth.db.repo;

import com.axelor.auth.db.UserToken;
import com.axelor.db.Query;
import java.util.Map;
import org.apache.shiro.authz.UnauthorizedException;

public class UserTokenRepository extends AbstractUserTokenRepository {
  public UserToken findByKey(String key) {
    return Query.of(UserToken.class)
        .filter("self.tokenKey = :key")
        .bind("key", key)
        .cacheable()
        .fetchOne();
  }

  @Override
  public Map<String, Object> populate(Map<String, Object> json, Map<String, Object> context) {
    json.remove("tokenKey");
    json.remove("tokenDigest");
    return json;
  }

  @Override
  public Map<String, Object> validate(Map<String, Object> json, Map<String, Object> context) {
    if (json.containsKey("tokenKey")) {
      throw new UnauthorizedException(
          "Your request includes changes to the protected field tokenKey, which is not allowed");
    }
    if (json.containsKey("tokenDigest")) {
      throw new UnauthorizedException(
          "Your request includes changes to the protected field tokenDigest, which is not allowed");
    }
    return super.validate(json, context);
  }
}
