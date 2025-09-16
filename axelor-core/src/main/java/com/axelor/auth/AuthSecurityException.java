/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.auth;

import com.axelor.common.ObjectUtils;
import com.axelor.db.JpaSecurity.AccessType;

public class AuthSecurityException extends RuntimeException {

  private static final long serialVersionUID = -794508889899422879L;

  private AccessType type;

  private Class<?> model;

  private Long[] ids;

  public AuthSecurityException(AccessType type) {
    this(type, null);
  }

  public AuthSecurityException(AccessType type, Class<?> model, Long... ids) {
    this.type = type;
    this.model = model;
    this.ids = ids;
  }

  public AccessType getType() {
    return type;
  }

  public Class<?> getModel() {
    return model;
  }

  public Long[] getIds() {
    return ids;
  }

  @Override
  public String getMessage() {
    return type.getMessage();
  }

  public String getViolationsDetail() {
    if (model == null) {
      return null;
    }
    final StringBuilder builder = new StringBuilder().append(model.getName());
    if (ObjectUtils.notEmpty(ids)) {
      builder.append("#");
      for (int i = 0, n = Math.min(5, ids.length); i < n; i++) {
        builder.append(ids[i]);
        if (i < n - 1) {
          builder.append(",");
        }
      }
      if (ids.length > 5) {
        builder.append(",...");
      }
    }
    return builder.toString();
  }
}
