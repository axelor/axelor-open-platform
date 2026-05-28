/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.auth.db.repo;

import com.axelor.auth.db.UserToken;
import com.axelor.db.JpaSecurity;
import com.axelor.db.Query;
import com.axelor.inject.Beans;
import java.util.Map;
import java.util.Set;

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

  @Override
  public Map<String, Object> populate(Map<String, Object> json, Map<String, Object> context) {
    return protectedFieldsProcessor.populate(json, context);
  }

  @Override
  public Map<String, Object> validate(Map<String, Object> json, Map<String, Object> context) {
    return protectedFieldsProcessor.validate(json, context);
  }

  public boolean isPermitted() {
    return Beans.get(JpaSecurity.class).isPermitted(JpaSecurity.CAN_READ, UserToken.class);
  }
}
