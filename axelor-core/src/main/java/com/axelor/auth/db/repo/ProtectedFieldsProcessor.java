/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.auth.db.repo;

import java.util.Map;
import java.util.Set;
import org.apache.shiro.authz.UnauthorizedException;

class ProtectedFieldsProcessor {

  private final Set<String> protectedFields;

  public ProtectedFieldsProcessor(Set<String> protectedFields) {
    this.protectedFields = protectedFields;
  }

  public Map<String, Object> populate(Map<String, Object> json, Map<String, Object> context) {
    json.keySet().removeAll(protectedFields);
    return json;
  }

  public Map<String, Object> validate(Map<String, Object> json, Map<String, Object> context) {
    for (var field : protectedFields) {
      if (json.containsKey(field)) {
        throw new UnauthorizedException();
      }
    }

    return json;
  }
}
