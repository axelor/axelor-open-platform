/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2019 Axelor (<http://axelor.com>).
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

import com.axelor.dms.db.DMSFile;
import com.axelor.inject.Beans;
import java.util.Optional;
import javax.persistence.PostPersist;

public class DMSFileListener {

  private final DMSPermissionRepository dmsPermissionRepo;

  public DMSFileListener() {
    dmsPermissionRepo = Beans.get(DMSPermissionRepository.class);
  }

  @PostPersist
  public void copyParentPermissions(DMSFile dmsFile) {
    Optional.ofNullable(dmsFile.getParent())
        .map(DMSFile::getPermissions)
        .ifPresent(
            permissions ->
                permissions
                    .stream()
                    .map(permission -> dmsPermissionRepo.copy(permission, false))
                    .forEach(dmsFile::addPermission));
  }
}
