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
