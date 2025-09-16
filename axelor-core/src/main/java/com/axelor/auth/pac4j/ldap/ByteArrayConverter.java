/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.auth.pac4j.ldap;

import com.axelor.common.ObjectUtils;
import java.util.Base64;
import org.pac4j.core.profile.converter.AbstractAttributeConverter;

public class ByteArrayConverter extends AbstractAttributeConverter {

  public static final ByteArrayConverter INSTANCE = new ByteArrayConverter();

  protected ByteArrayConverter() {
    super(byte[].class);
  }

  @Override
  protected byte[] internalConvert(Object attribute) {
    return ObjectUtils.isEmpty(attribute) ? null : Base64.getDecoder().decode(attribute.toString());
  }
}
