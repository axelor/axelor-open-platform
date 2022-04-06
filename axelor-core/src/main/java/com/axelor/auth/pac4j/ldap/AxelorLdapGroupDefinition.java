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

public class AxelorLdapGroupDefinition {
  public static final String NAME = "cn";
  public static final String ID = "gidNumber";
  public static final String OBJECT_CLASS = "objectClass";
  public static final String UNIQUE_MEMBER = "uniqueMember";
  public static final String MEMBER = "member";

  public static final List<String> ATTRIBUTES =
      ImmutableList.of(NAME, ID, OBJECT_CLASS, UNIQUE_MEMBER, MEMBER);

  private AxelorLdapGroupDefinition() {}
}
