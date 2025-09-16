/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.auth.pac4j.ldap;

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
      List.of(
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
