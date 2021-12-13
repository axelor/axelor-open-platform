/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2021 Axelor (<http://axelor.com>).
 *
 * This program is free software: you can redistribute it and/or  modify
 * it under the terms of the GNU Affero General Public License, version 3,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.axelor.auth;

import com.axelor.db.JpaSecurity.AccessType;
import java.util.Optional;

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
    final String modelName = Optional.ofNullable(model).map(Class::getName).orElse("null");
    final StringBuilder builder = new StringBuilder().append(modelName);
    if (ids.length > 0) {
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
