/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.data.adapter;

import com.axelor.auth.AuthService;
import com.axelor.common.StringUtils;
import com.axelor.inject.Beans;
import java.util.Map;

public class PasswordAdapter extends Adapter {

  @Override
  public Object adapt(Object value, Map<String, Object> context) {
    if (value == null) {
      return null;
    }
    if (!(value instanceof String)) {
      throw new IllegalArgumentException("password adapter accepts string value.");
    }
    if (StringUtils.isBlank((String) value)) {
      return null;
    }
    return Beans.get(AuthService.class).encrypt((String) value);
  }
}
