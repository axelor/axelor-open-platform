/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2017 Axelor (<http://axelor.com>).
 *
 * This program is free software: you can redistribute it and/or  modify
 * it under the terms of the GNU Affero General Public License, version 3,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.axelor.meta;

import java.util.Set;

import javax.inject.Singleton;

import com.axelor.auth.db.Role;
import com.axelor.auth.db.User;
import com.axelor.meta.db.MetaPermission;
import com.axelor.meta.db.MetaPermissionRule;

@Singleton
public class MetaPermissions {

	private static final String CAN_READ = "read";
	private static final String CAN_WRITE = "write";
	private static final String CAN_EXPORT = "export";

	private MetaPermission find(Set<MetaPermission> permissions, String object, String field) {
		if (permissions == null) { return null; }
		for (MetaPermission perm : permissions) {
			if (!object.equals(perm.getObject())) { continue; }
			if (perm.getActive() != Boolean.TRUE || perm.getRules() == null) { continue; }
			for (MetaPermissionRule rule : perm.getRules()) {
				if (field.equals(rule.getField())) {
					return perm;
				}
			}
		}
		return null;
	}

	private MetaPermission find(User user, String object, String field) {
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
		if (permission == null) { return null; }
		for (MetaPermissionRule rule : permission.getRules()) {
			if (field.equals(rule.getField())) {
				return rule;
			}
		}
		return null;
	}

	private boolean can(User user, String object, String field, String access) {
		final MetaPermissionRule rule = findRule(user, object, field);
		if (rule == null) { return true; }
		switch (access) {
		case CAN_READ:
			if (rule.getCanRead() == Boolean.TRUE) return true;
			break;
		case CAN_WRITE:
			if (rule.getCanWrite() == Boolean.TRUE) return true;
			break;
		case CAN_EXPORT:
			if (rule.getCanExport() == Boolean.TRUE) return true;
			break;
		}
		return false;
	}
	
	public boolean canRead(User user, String object, String field) {
		return can(user, object, field, CAN_READ);
	}

	public boolean canWrite(User user, String object, String field) {
		return can(user, object, field, CAN_EXPORT);
	}

	public boolean canExport(User user, String object, String field) {
		return can(user, object, field, CAN_EXPORT);
	}
}
