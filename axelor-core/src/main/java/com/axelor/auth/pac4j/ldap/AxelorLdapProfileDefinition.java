/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2022 Axelor (<http://axelor.com>).
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
package com.axelor.auth.pac4j.ldap;

import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.pac4j.core.profile.converter.Converters;
import org.pac4j.core.profile.definition.CommonProfileDefinition;

public class AxelorLdapProfileDefinition extends CommonProfileDefinition {

  public static final String USERNAME = "uid";
  public static final String DISPLAY_NAME = "cn";
  public static final String EMAIL = "mail";
  public static final String FIRST_NAME = "givenName";
  public static final String FAMILY_NAME = "sn";
  public static final String LOCALE = "preferredLanguage";
  public static final String PICTURE_JPEG = "jpegPhoto";
  public static final String PASSWORD = "userPassword";

  public static final List<String> ATTRIBUTES =
      ImmutableList.of(
          USERNAME,
          EMAIL,
          DISPLAY_NAME,
          FIRST_NAME,
          FAMILY_NAME,
          LOCALE,
          PICTURE_JPEG,
          AxelorLdapGroupDefinition.ID);

  public AxelorLdapProfileDefinition() {
    super(x -> new AxelorLdapProfile());
    Stream.of(USERNAME, EMAIL, DISPLAY_NAME, FIRST_NAME, FAMILY_NAME)
        .forEach(attribute -> primary(attribute, Converters.STRING));
    primary(LOCALE, Converters.LOCALE);
    primary(AxelorLdapGroupDefinition.ID, Converters.INTEGER);
    primary(PICTURE_JPEG, ByteArrayConverter.INSTANCE);
  }

  public static String getAttributes(String idAttribute) {
    return ATTRIBUTES.stream()
        .filter(attribute -> !attribute.equals(idAttribute))
        .collect(Collectors.joining(","));
  }
}
