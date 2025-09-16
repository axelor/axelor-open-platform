/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.auth;

import com.axelor.auth.db.User;
import com.axelor.common.ObjectUtils;
import com.axelor.db.Model;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Deprecated
public class AuthSecurityWarner extends AuthSecurity {

  private static final int MAX_IDS = 5;
  private static final Logger log = LoggerFactory.getLogger(AuthSecurityWarner.class);

  @Override
  public void check(AccessType type, Class<? extends Model> model, Long... ids) {
    if (!isPermitted(type, model, ids)) {
      log.warn(
          "User \"{}\" doesn’t have {} permission on {}",
          Optional.ofNullable(AuthUtils.getUser()).map(User::getCode).orElse(null),
          type,
          getTargetString(model, ids));
    }
  }

  private String getTargetString(Class<? extends Model> model, Long... ids) {
    final StringBuilder sb =
        new StringBuilder(Optional.ofNullable(model).map(Class::getName).orElse("null"));

    if (ObjectUtils.notEmpty(ids)) {
      sb.append("#");
      final List<String> items = new ArrayList<>();
      for (int i = 0, max = Math.min(ids.length, MAX_IDS); i < max; ++i) {
        items.add(String.valueOf(ids[i]));
      }
      if (ids.length > MAX_IDS) {
        items.add("…");
      }
      sb.append(items.stream().collect(Collectors.joining(",")));
    }

    return sb.toString();
  }
}
