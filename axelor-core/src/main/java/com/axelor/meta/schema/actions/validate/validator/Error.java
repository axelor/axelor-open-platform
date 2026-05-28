/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.meta.schema.actions.validate.validator;

import jakarta.xml.bind.annotation.XmlType;

@XmlType
public class Error extends Validator {

  public static final String KEY = ValidatorType.ERROR.toString();

  @Override
  public String getKey() {
    return KEY;
  }
}
