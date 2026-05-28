/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.auth.pac4j.ldap;

import java.util.List;

public class AxelorLdapGroupDefinition {
  public static final String NAME = "cn";
  public static final String ID = "gidNumber";
  public static final String OBJECT_CLASS = "objectClass";
  public static final String UNIQUE_MEMBER = "uniqueMember";
  public static final String MEMBER = "member";

  public static final List<String> ATTRIBUTES =
      List.of(NAME, ID, OBJECT_CLASS, UNIQUE_MEMBER, MEMBER);

  private AxelorLdapGroupDefinition() {}
}
