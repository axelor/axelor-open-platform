/**
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

	public static final String WILDCARD = "*";

	private MetaPermissionRule findRule(Set<MetaPermission> permissions, String object, String field) {
		MetaPermissionRule result = null;
		if (permissions != null) {
			for (MetaPermission perm : permissions) {
				if (object.equals(perm.getObject())
					&& perm.getActive())
				{
					for (MetaPermissionRule rule : perm.getRules()) {
						if (field.equals(rule.getField())) {
							result = rule;
							break;
						} else if (WILDCARD.equals(rule.getField())) {
							result = rule;
						}
					}
				}
			}
		}
		return result;
	}

	public MetaPermissionRule findRule(User user, String object, String field) {
		MetaPermissionRule permission = findRule(user.getMetaPermissions(), object, field);
		if (permission == null && user.getGroup() != null) {
			permission = findRule(user.getGroup().getMetaPermissions(), object, field);
		}
		if (permission == null && user.getRoles() != null) {
			for (Role role : user.getRoles()) {
				permission = findRule(role.getMetaPermissions(), object, field);
				if (permission != null) {
					break;
				}
			}
		}
		if (permission == null && user.getGroup() != null && user.getGroup().getRoles() != null) {
			for (Role role : user.getGroup().getRoles()) {
				permission = findRule(role.getMetaPermissions(), object, field);
				if (permission != null) {
					break;
				}
			}
		}
		return permission;
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
