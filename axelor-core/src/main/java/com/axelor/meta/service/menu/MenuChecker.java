/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2023 Axelor (<http://axelor.com>).
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
package com.axelor.meta.service.menu;

import static com.axelor.common.StringUtils.isBlank;
import static com.axelor.meta.loader.ModuleManager.isInstalled;

import com.axelor.auth.AuthUtils;
import com.axelor.auth.db.Role;
import com.axelor.auth.db.User;
import com.axelor.common.ObjectUtils;
import com.axelor.db.JPA;
import com.axelor.db.QueryBinder;
import com.axelor.meta.db.MetaMenu;
import com.axelor.script.CompositeScriptHelper;
import com.axelor.script.ScriptBindings;
import com.axelor.script.ScriptHelper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.persistence.Query;

/** Defined the {@link MetaMenu} policies */
public class MenuChecker {

  private final List<MetaMenu> menus;
  private final User user;

  private final Map<Long, Set<String>> menuGroups = new HashMap<>();
  private final Map<Long, Set<String>> menuRoles = new HashMap<>();

  private final List<String> userRoles = new ArrayList<>();
  private String userGroup;

  final ScriptHelper scriptHelper = new CompositeScriptHelper(new ScriptBindings(new HashMap<>()));

  public MenuChecker(List<MetaMenu> menus, User user) {
    this.menus = menus;
    this.user = user;
    init();
  }

  /**
   * Check whether the given {@link MetaMenu} is allowed for the {@link User}
   *
   * @param item the meta menu to check
   * @return true if the meta menu can be shown, false otherwise
   */
  public boolean isAllowed(MetaMenu item) {
    // check for user menus
    if (item.getUser() != null && item.getUser() != user) {
      return false;
    }

    final Set<String> myGroups = menuGroups.get(item.getId());
    final Set<String> myRoles = menuRoles.get(item.getId());

    // check for groups and roles
    return AuthUtils.isAdmin(user)
        || (myGroups != null && myGroups.contains(userGroup))
        || (myRoles != null && !Collections.disjoint(userRoles, myRoles))
        || (myRoles == null && myGroups == null && item.getParent() != null);
  }

  /**
   * Check if the specified module to check is installed and the conditions to check are verified
   *
   * @param item the meta menu to check
   * @return true if all is checks passed, false otherwise
   */
  private boolean test(MetaMenu item) {
    final String module = item.getModuleToCheck();
    final String condition = item.getConditionToCheck();
    if (!isBlank(module) && !isInstalled(module)) {
      return false;
    }
    if (isBlank(condition)) {
      return true;
    }
    return scriptHelper.test(condition);
  }

  /**
   * Check whether the given {@link MetaMenu} can be shown
   *
   * @param item the meta menu to check
   * @return true if the meta menu can be shown, false otherwise
   */
  public boolean canShow(MetaMenu item) {
    if (Boolean.TRUE.equals(item.getHidden()) || !test(item)) {
      return false;
    }
    return true;
  }

  /** Prepare menus groups and roles info and user roles to avoid additional queries */
  private void init() {

    final StringBuilder queryString =
        new StringBuilder()
            .append("SELECT new List(m.id, g.code, r.name) ")
            .append("FROM MetaMenu m ")
            .append("LEFT JOIN m.groups g ")
            .append("LEFT JOIN m.roles r ");

    if (ObjectUtils.notEmpty(menus)) {
      queryString.append("WHERE m.name IN (:names)");
    }
    Query query = JPA.em().createQuery(queryString.toString());
    QueryBinder.of(query)
        .setCacheable()
        .bind("names", menus.stream().map(MetaMenu::getName).collect(Collectors.toList()));

    for (Object item : query.getResultList()) {
      final List<?> vals = (List<?>) item;
      final Long id = (Long) vals.get(0);
      if (vals.get(1) != null) {
        Set<String> groups = menuGroups.get(id);
        if (groups == null) {
          groups = new HashSet<>();
          menuGroups.put(id, groups);
        }
        groups.add(vals.get(1).toString());
      }
      if (vals.get(2) != null) {
        Set<String> roles = menuRoles.get(id);
        if (roles == null) {
          roles = new HashSet<>();
          menuRoles.put(id, roles);
        }
        roles.add(vals.get(2).toString());
      }
    }

    if (user.getRoles() != null) {
      for (Role role : user.getRoles()) {
        userRoles.add(role.getName());
      }
    }
    if (user.getGroup() != null && user.getGroup().getRoles() != null) {
      for (Role role : user.getGroup().getRoles()) {
        userRoles.add(role.getName());
      }
    }

    userGroup = user.getGroup() == null ? null : user.getGroup().getCode();
  }
}
