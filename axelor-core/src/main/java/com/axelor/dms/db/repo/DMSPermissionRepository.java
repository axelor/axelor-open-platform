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
package com.axelor.dms.db.repo;

import javax.inject.Inject;
import javax.persistence.PersistenceException;

import com.axelor.auth.db.Group;
import com.axelor.auth.db.Permission;
import com.axelor.auth.db.User;
import com.axelor.auth.db.repo.PermissionRepository;
import com.axelor.db.JpaRepository;
import com.axelor.dms.db.DMSFile;
import com.axelor.dms.db.DMSPermission;
import com.axelor.i18n.I18n;

public class DMSPermissionRepository extends JpaRepository<DMSPermission> {

	@Inject
	private PermissionRepository perms;

	public DMSPermissionRepository() {
		super(DMSPermission.class);
	}

	private Permission findOrCreate(String name, String... args) {
		Permission perm = perms.findByName(name);
		if (perm == null) {
			perm = new Permission();
			perm.setName(name);
			perm.setCondition(args.length > 0 ? args[0] : null);
			perm.setConditionParams(args.length > 1 ? args[1] : null);
			perm.setObject(args.length > 2 ? args[2] : DMSFile.class.getName());
			perm = perms.save(perm);
		}
		return perm;
	}

	@Override
	public DMSPermission save(DMSPermission entity) {

		final DMSFile file = entity.getFile();
		if (file == null) {
			throw new PersistenceException(I18n.get("Invalid permission"));
		}

		final User user = entity.getUser();
		final Group group = entity.getGroup();

		Permission permission = null;

		switch(entity.getValue()) {
		case "FULL":
			permission = findOrCreate("perm.dms.file.__full__",
					"(self.permissions.user = ? OR self.permissions.group = ?) AND self.permissions.permission.canCreate = true",
					"__user__, __user__.group");
			permission.setCanCreate(true);
			permission.setCanRead(true);
			permission.setCanWrite(true);
			permission.setCanRemove(true);
			break;
		case "WRITE":
			permission = findOrCreate("perm.dms.file.__write__",
					"(self.permissions.user = ? OR self.permissions.group = ?) AND self.permissions.permission.canWrite = true",
					"__user__, __user__.group");
			permission.setCanCreate(false);
			permission.setCanRead(true);
			permission.setCanWrite(true);
			permission.setCanRemove(true);
			break;
		case "READ":
			permission = findOrCreate("perm.dms.file.__read__",
					"(self.permissions.user = ? OR self.permissions.group = ?) AND self.permissions.permission.canRead = true",
					"__user__, __user__.group");
			permission.setCanCreate(false);
			permission.setCanRead(true);
			permission.setCanWrite(false);
			permission.setCanRemove(false);
			break;
		}

		if (permission == null) {
			return super.save(entity);
		}

		final Permission __self__ = findOrCreate("perm.dms.file.__self__", "self.createdBy = ?", "__user__", DMSFile.class.getName());
		final Permission __create__ = findOrCreate("perm.dms.__create__", null, null, "com.axelor.dms.db.*");
		final Permission __meta__ = findOrCreate("perm.meta.file.__create__", null, null, "com.axelor.meta.db.MetaFile");
		final Permission __parent__ = findOrCreate("perm.dms.file.__parent__",
				"(self.parent.permissions.user = ? OR self.parent.permissions.group = ?) AND self.parent.permissions.permission.canRead = true",
				"__user__, __user__.group");

		final Permission __perm_full__ = findOrCreate("perm.dms.perm.__full__",
				"self.createdBy = ? OR ((self.user = ? OR self.group = ?) AND self.value = 'FULL')",
				"__user__, __user__, __user__.group", DMSPermission.class.getName());

		__perm_full__.setCanCreate(true);
		__perm_full__.setCanRead(true);
		__perm_full__.setCanWrite(true);
		__perm_full__.setCanRemove(true);

		__parent__.setCanCreate(false);
		__parent__.setCanRead(true);
		__parent__.setCanWrite(false);
		__parent__.setCanRemove(false);

		__self__.setCanCreate(false);
		__self__.setCanRead(true);
		__self__.setCanWrite(true);
		__self__.setCanRemove(true);

		__create__.setCanCreate(true);
		__create__.setCanRead(false);
		__create__.setCanWrite(false);
		__create__.setCanRemove(false);

		__meta__.setCanCreate(true);
		__meta__.setCanRead(false);
		__meta__.setCanWrite(false);
		__meta__.setCanRemove(false);

		if (user != null) {
			user.addPermission(permission);
			user.addPermission(__self__);
			user.addPermission(__create__);
			user.addPermission(__parent__);
			user.addPermission(__meta__);
			user.addPermission(__perm_full__);
		}
		if (group != null) {
			group.addPermission(permission);
			group.addPermission(__self__);
			group.addPermission(__create__);
			group.addPermission(__parent__);
			group.addPermission(__meta__);
			group.addPermission(__perm_full__);
		}

		entity.setPermission(permission);

		return super.save(entity);
	}
}
