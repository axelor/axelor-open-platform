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
package com.axelor.meta;

import com.axelor.auth.db.Role;
import com.axelor.auth.db.User;
import com.axelor.common.StringUtils;
import com.axelor.db.JpaSecurity;
import com.axelor.db.Model;
import com.axelor.db.mapper.Mapper;
import com.axelor.db.mapper.Property;
import com.axelor.inject.Beans;
import com.axelor.meta.db.MetaPermission;
import com.axelor.meta.db.MetaPermissionRule;
import com.axelor.meta.schema.views.PanelField;
import com.axelor.meta.schema.views.SimpleWidget;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import javax.inject.Singleton;

@Singleton
public class MetaPermissions {

  private static final String CAN_READ = "read";
  private static final String CAN_WRITE = "write";
  private static final String CAN_EXPORT = "export";

  private MetaPermission find(Set<MetaPermission> permissions, String object, String field) {
    if (permissions == null) {
      return null;
    }
    for (MetaPermission perm : permissions) {
      if (!object.equals(perm.getObject())) {
        continue;
      }
      if (!Boolean.TRUE.equals(perm.getActive()) || perm.getRules() == null) {
        continue;
      }
      for (MetaPermissionRule rule : perm.getRules()) {
        if (field.equals(rule.getField())) {
          return perm;
        }
      }
    }
    return null;
  }

  private MetaPermission find(User user, String object, String field) {
    if (user == null) {
      return null;
    }

    MetaPermission permission = find(user.getMetaPermissions(), object, field);
    if (permission == null && user.getGroup() != null) {
      permission = find(user.getGroup().getMetaPermissions(), object, field);
    }
    if (permission == null && user.getRoles() != null) {
      for (Role role : user.getRoles()) {
        permission = find(role.getMetaPermissions(), object, field);
        if (permission != null) {
          break;
        }
      }
    }
    if (permission == null && user.getGroup() != null && user.getGroup().getRoles() != null) {
      for (Role role : user.getGroup().getRoles()) {
        permission = find(role.getMetaPermissions(), object, field);
        if (permission != null) {
          break;
        }
      }
    }
    return permission;
  }

  public MetaPermissionRule findRule(User user, String object, String field) {
    final MetaPermission permission = find(user, object, field);
    if (permission == null) {
      return null;
    }
    for (MetaPermissionRule rule : permission.getRules()) {
      if (field.equals(rule.getField())) {
        return rule;
      }
    }
    return null;
  }

  @Deprecated
  public boolean isCollectionReadable(User user, String object, String field) {
    if (StringUtils.isBlank(object)) return true;
    final Class<?> klass;
    try {
      klass = Class.forName(object);
    } catch (ClassNotFoundException e) {
      throw new IllegalArgumentException(e);
    }
    final Mapper mapper = Mapper.of(klass);
    final Property property = mapper.getProperty(field);
    return property == null
        || !property.isCollection()
        || Beans.get(JpaSecurity.class)
            .isPermitted(JpaSecurity.CAN_READ, property.getTarget().asSubclass(Model.class));
  }

  public boolean isRelatedReadable(String object, String field, SimpleWidget widget) {
    if (StringUtils.isBlank(object)) return true;
    final Class<?> klass;
    try {
      klass = Class.forName(object);
    } catch (ClassNotFoundException e) {
      throw new IllegalArgumentException(e);
    }
    Mapper mapper = Mapper.of(klass);
    Property property;
    final String[] fieldParts = Optional.ofNullable(field).orElse("").split("\\.");

    if (fieldParts.length < 2) {
      property = mapper.getProperty(field);

      // For editor fields, allow name field only if no read permissions.
      if (widget instanceof PanelField && ((PanelField) widget).isFromEditor()) {
        return Objects.equals(property, mapper.getNameField())
            || Beans.get(JpaSecurity.class)
                .isPermitted(JpaSecurity.CAN_READ, klass.asSubclass(Model.class));
      }

      return property == null
          || !property.isCollection()
          || Beans.get(JpaSecurity.class)
              .isPermitted(JpaSecurity.CAN_READ, property.getTarget().asSubclass(Model.class));
    }

    // Check for read permissions on dotted field

    final JpaSecurity security = Beans.get(JpaSecurity.class);

    for (final String fieldPart : fieldParts) {
      property = mapper.getProperty(fieldPart);
      final Optional<Class<?>> target = Optional.ofNullable(property).map(Property::getTarget);

      if (!target.isPresent()) {
        break;
      }

      final Class<?> targetClass = target.get();

      if (!security.isPermitted(JpaSecurity.CAN_READ, targetClass.asSubclass(Model.class))) {
        return false;
      }

      mapper = Mapper.of(targetClass);
    }

    return true;
  }

  private boolean can(User user, String object, String field, String access) {
    final MetaPermissionRule rule = findRule(user, object, field);
    if (rule == null) {
      return true;
    }
    switch (access) {
      case CAN_READ:
        if (Boolean.TRUE.equals(rule.getCanRead())) return true;
        break;
      case CAN_WRITE:
        if (Boolean.TRUE.equals(rule.getCanWrite())) return true;
        break;
      case CAN_EXPORT:
        if (Boolean.TRUE.equals(rule.getCanExport())) return true;
        break;
    }
    return false;
  }

  public boolean canRead(User user, String object, String field) {
    return can(user, object, field, CAN_READ);
  }

  public boolean canWrite(User user, String object, String field) {
    return can(user, object, field, CAN_WRITE);
  }

  public boolean canExport(User user, String object, String field) {
    return can(user, object, field, CAN_EXPORT);
  }
}
