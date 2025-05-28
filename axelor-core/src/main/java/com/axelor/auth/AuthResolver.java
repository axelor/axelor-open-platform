/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2025 Axelor (<http://axelor.com>).
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
package com.axelor.auth;

import com.axelor.auth.db.Permission;
import com.axelor.auth.db.Role;
import com.axelor.auth.db.User;
import com.axelor.db.JpaSecurity.AccessType;
import com.google.common.base.Objects;
import com.google.common.collect.Sets;
import java.util.Set;

/** This class is responsible to resolve permissions. */
final class AuthResolver {

  /**
   * Check whether the given {@link Permission} confirms the requested access type.
   *
   * @param permission the permission instance to check
   * @param accessType the required access type, if null, returns true always
   * @return true if can confirm or given accessType is null otherwise false
   */
  boolean hasAccess(Permission permission, AccessType accessType) {
    if (accessType == null) {
      return true;
    }
    switch (accessType) {
      case READ:
        return Boolean.TRUE.equals(permission.getCanRead());
      case WRITE:
        return Boolean.TRUE.equals(permission.getCanWrite());
      case CREATE:
        return Boolean.TRUE.equals(permission.getCanCreate());
      case REMOVE:
        return Boolean.TRUE.equals(permission.getCanRemove());
      case IMPORT:
        return Boolean.TRUE.equals(permission.getCanImport());
      case EXPORT:
        return Boolean.TRUE.equals(permission.getCanExport());
      default:
        return false;
    }
  }

  /**
   * Filter the given set of permissions for the given object with the required access type. <br>
   *
   * <p>It first tries to find exact match for the given object else it tries to find wild card (by
   * package name). The permissions on objects without condition gets preference over wild card
   * permissions.
   *
   * @param permissions set of permissions to filter
   * @param object object name for which to check the permission
   * @param type the requested access type
   * @return filtered set of {@link Permission}
   */
  private Set<Permission> filterPermissions(
      final Set<Permission> permissions, final String object, final AccessType type) {
    final Set<Permission> all = Sets.newLinkedHashSet();
    if (permissions == null || permissions.isEmpty()) {
      return all;
    }

    // add object permissions
    for (final Permission permission : permissions) {
      if (Objects.equal(object, permission.getObject()) && hasAccess(permission, type)) {
        all.add(permission);
      }
    }

    // add wild card permissions
    final String pkg = object.substring(0, object.lastIndexOf('.')) + ".*";
    for (final Permission permission : permissions) {
      if (Objects.equal(pkg, permission.getObject()) && hasAccess(permission, type)) {
        all.add(permission);
      }
    }

    return all;
  }

  /**
   * Get the set of {@link Permission} for the given type on the object. <br>
   * <br>
   * The permission resolution is done in following way: <br>
   * <br>
   * Check the permissions directly assigned to the user, else check permissions assigned to the
   * user's roles, else check the permissions assigned directly to the user group, else check the
   * permissions assigned to the group's roles.
   *
   * @param user the user to authorize
   * @param object the object name (class or package name)
   * @param type access type to check
   * @return {@link Set} of {@link Permission}
   */
  public Set<Permission> resolve(final User user, final String object, final AccessType type) {

    // user permissions
    Set<Permission> all = filterPermissions(user.getPermissions(), object, type);

    // user's role permissions
    if (user.getRoles() != null) {
      for (final Role role : user.getRoles()) {
        all.addAll(filterPermissions(role.getPermissions(), object, type));
      }
    }

    // group permissions
    if (user.getGroup() != null) {
      all.addAll(filterPermissions(user.getGroup().getPermissions(), object, type));
    }

    // group's role permissions
    if (user.getGroup() != null && user.getGroup().getRoles() != null) {
      for (final Role role : user.getGroup().getRoles()) {
        all.addAll(filterPermissions(role.getPermissions(), object, type));
      }
    }

    return all;
  }
}
