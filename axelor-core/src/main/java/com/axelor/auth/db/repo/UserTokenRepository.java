/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2025 Axelor (<http://axelor.com>).
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
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
