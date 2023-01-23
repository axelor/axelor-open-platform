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
package com.axelor.dms.db.repo;

import com.axelor.auth.db.Group;
import com.axelor.auth.db.Permission;
import com.axelor.auth.db.User;
import com.axelor.auth.db.repo.PermissionRepository;
import com.axelor.db.JPA;
import com.axelor.db.JpaRepository;
import com.axelor.db.Query;
import com.axelor.db.internal.DBHelper;
import com.axelor.db.tenants.TenantAware;
import com.axelor.dms.db.DMSFile;
import com.axelor.dms.db.DMSPermission;
import com.axelor.i18n.I18n;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.stream.Stream;
import javax.inject.Inject;
import javax.persistence.PersistenceException;

public class DMSPermissionRepository extends JpaRepository<DMSPermission> {

  @Inject private PermissionRepository perms;

  private final ExecutorService executor = Executors.newSingleThreadExecutor();

  private static final int BATCH_SIZE = 1000;

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

  private Permission findOrCreateRead() {
    final Permission permission =
        findOrCreate(
            "perm.dms.file.__read__",
            "self.id = ANY(SELECT x.id FROM DMSFile x "
                + "LEFT JOIN x.permissions x_permissions "
                + "LEFT JOIN x_permissions.user x_permissions_user "
                + "LEFT JOIN x_permissions.group x_permissions_group "
                + "LEFT JOIN x_permissions.permission x_permissions_permission "
                + "WHERE (x_permissions_user = ? OR x_permissions_group = ?) "
                + "AND x_permissions_permission.canRead = true)",
            "__user__, __user__.group");
    permission.setCanCreate(false);
    permission.setCanRead(true);
    permission.setCanWrite(false);
    permission.setCanRemove(false);
    return permission;
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
    Permission __read__ = findOrCreateRead();

    switch (entity.getValue()) {
      case "FULL":
        permission =
            findOrCreate(
                "perm.dms.file.__full__",
                "self.id = ANY(SELECT x.id FROM DMSFile x "
                    + "LEFT JOIN x.permissions x_permissions "
                    + "LEFT JOIN x_permissions.user x_permissions_user "
                    + "LEFT JOIN x_permissions.group x_permissions_group "
                    + "LEFT JOIN x_permissions.permission x_permissions_permission "
                    + "WHERE (x_permissions_user = ? OR x_permissions_group = ?) "
                    + "AND x_permissions_permission.canCreate = true)",
                "__user__, __user__.group");
        permission.setCanCreate(true);
        permission.setCanRead(true);
        permission.setCanWrite(true);
        permission.setCanRemove(true);
        break;
      case "WRITE":
        permission =
            findOrCreate(
                "perm.dms.file.__write__",
                "self.id = ANY(SELECT x.id FROM DMSFile x "
                    + "LEFT JOIN x.permissions x_permissions "
                    + "LEFT JOIN x_permissions.user x_permissions_user "
                    + "LEFT JOIN x_permissions.group x_permissions_group "
                    + "LEFT JOIN x_permissions.permission x_permissions_permission "
                    + "WHERE (x_permissions_user = ? OR x_permissions_group = ?) "
                    + "AND x_permissions_permission.canWrite = true)",
                "__user__, __user__.group");
        permission.setCanCreate(false);
        permission.setCanRead(true);
        permission.setCanWrite(true);
        permission.setCanRemove(true);
        break;
      case "READ":
        permission = __read__;
        break;
    }

    if (permission == null) {
      return super.save(entity);
    }

    final Permission __self__ =
        findOrCreate(
            "perm.dms.file.__self__", "self.createdBy = ?", "__user__", DMSFile.class.getName());
    final Permission __create__ =
        findOrCreate("perm.dms.__create__", null, null, "com.axelor.dms.db.*");
    final Permission __meta__ =
        findOrCreate("perm.meta.file.__create__", null, null, "com.axelor.meta.db.MetaFile");

    final Permission __perm_full__ =
        findOrCreate(
            "perm.dms.perm.__full__",
            "self.createdBy = ? OR ((self.user = ? OR self.group = ?) AND self.value = 'FULL')",
            "__user__, __user__, __user__.group",
            DMSPermission.class.getName());

    __perm_full__.setCanCreate(true);
    __perm_full__.setCanRead(true);
    __perm_full__.setCanWrite(true);
    __perm_full__.setCanRemove(true);

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
      user.addPermission(__read__);
      user.addPermission(__create__);
      user.addPermission(__meta__);
      user.addPermission(__perm_full__);
    }
    if (group != null) {
      group.addPermission(permission);
      group.addPermission(__read__);
      group.addPermission(__create__);
      group.addPermission(__meta__);
      group.addPermission(__perm_full__);
    }

    entity.setPermission(permission);
    final int version = entity.getVersion();
    entity = super.save(entity);

    applyPermissionToParents(entity, __read__);

    if (file.getIsDirectory() && entity.getVersion() > version) {
      applySamePermissionToChildren(entity);
    }

    return entity;
  }

  @Override
  public void remove(DMSPermission entity) {
    recursiveRemoveHavingSamePermission(entity);
  }

  private void applyPermissionToParents(DMSPermission entity, Permission permission) {
    for (DMSFile parentFile = entity.getFile().getParent();
        parentFile != null;
        parentFile = parentFile.getParent()) {
      if (!filterPermission(parentFile.getPermissions(), entity).findFirst().isPresent()) {
        final DMSPermission dmsPermission = new DMSPermission();
        dmsPermission.setUser(entity.getUser());
        dmsPermission.setGroup(entity.getGroup());
        dmsPermission.setValue("READ");
        dmsPermission.setPermission(permission);
        dmsPermission.setFile(parentFile);
        JPA.em().persist(dmsPermission);
      }
    }
  }

  private void applySamePermissionToChildren(DMSPermission entity) {
    final int valueLevel = getValueLevel(entity);
    final List<Long> existingPermissionIds = new ArrayList<>();
    final List<Long> createPermissionFileIds = new ArrayList<>();

    processDMSFileChildren(
        entity,
        files ->
            files.forEach(
                file -> {
                  final DMSPermission basePermission = find(entity.getId());
                  final Optional<DMSPermission> existingPermissionOpt =
                      filterPermission(file.getPermissions(), basePermission).findFirst();
                  if (existingPermissionOpt.isPresent()) {
                    final DMSPermission existingPermission = existingPermissionOpt.get();

                    if (getValueLevel(existingPermission) < valueLevel) {
                      existingPermissionIds.add(existingPermission.getId());
                    }
                  } else {
                    createPermissionFileIds.add(file.getId());
                  }
                }));

    Lists.partition(existingPermissionIds, BATCH_SIZE)
        .forEach(
            ids ->
                executor.execute(
                    new TenantAware(
                        () -> {
                          JPA.em()
                              .createQuery(
                                  "UPDATE DMSPermission self SET self.value = :value "
                                      + "WHERE self.id IN :ids")
                              .setParameter("value", entity.getValue())
                              .setParameter("ids", ids)
                              .executeUpdate();
                          JPA.flush();
                          JPA.clear();
                        })));

    Lists.partition(createPermissionFileIds, BATCH_SIZE)
        .forEach(
            ids ->
                executor.execute(
                    new TenantAware(
                        () -> {
                          final Group group =
                              Optional.ofNullable(entity.getGroup())
                                  .map(Group::getId)
                                  .map(id -> JpaRepository.of(Group.class).find(id))
                                  .orElse(null);
                          final User user =
                              Optional.ofNullable(entity.getUser())
                                  .map(User::getId)
                                  .map(id -> JpaRepository.of(User.class).find(id))
                                  .orElse(null);
                          final Permission permission =
                              Optional.ofNullable(entity.getPermission())
                                  .map(Permission::getId)
                                  .map(id -> JpaRepository.of(Permission.class).find(id))
                                  .orElse(null);
                          JpaRepository.of(DMSFile.class)
                              .all()
                              .filter("self.id IN :ids")
                              .bind("ids", ids)
                              .fetch()
                              .forEach(
                                  file -> {
                                    final DMSPermission dmsPermission = new DMSPermission();
                                    dmsPermission.setValue(entity.getValue());
                                    dmsPermission.setFile(file);
                                    dmsPermission.setGroup(group);
                                    dmsPermission.setUser(user);
                                    dmsPermission.setPermission(permission);
                                    JPA.em().persist(dmsPermission);
                                  });
                          JPA.flush();
                          JPA.clear();
                        })));
  }

  private void recursiveRemoveHavingSamePermission(DMSPermission entity) {
    final JpaRepository<DMSFile> dmsFileRepo = JpaRepository.of(DMSFile.class);
    final List<Long> entityIds = Lists.newArrayList(entity.getId());

    // Find orphan parent permissions.
    for (DMSFile parentFile = entity.getFile().getParent();
        parentFile != null;
        parentFile = parentFile.getParent()) {
      if (dmsFileRepo
          .all()
          .filter(
              "self.parent = :parent AND self != :baseFile "
                  + "AND (self.permissions.user = :user OR self.permissions.group = :group)")
          .bind("parent", parentFile)
          .bind("baseFile", entity.getFile())
          .bind("user", entity.getUser())
          .bind("group", entity.getGroup())
          .fetch(1)
          .isEmpty()) {
        filterPermission(parentFile.getPermissions(), entity)
            .map(DMSPermission::getId)
            .forEach(entityIds::add);
      }
    }

    // Find child permissions on the same user/group.
    if (entity.getFile() != null && entity.getFile().getIsDirectory()) {
      processDMSFileChildren(
          entity,
          files -> {
            final DMSPermission basePermission = find(entity.getId());
            files.forEach(
                file ->
                    filterPermission(file.getPermissions(), basePermission)
                        .map(DMSPermission::getId)
                        .forEach(entityIds::add));
          });
    }

    JPA.em()
        .createQuery("DELETE FROM DMSPermission self WHERE self.id IN (:entityIds)")
        .setParameter("entityIds", entityIds)
        .executeUpdate();
  }

  private void processDMSFileChildren(DMSPermission entity, Consumer<List<DMSFile>> processor) {
    final JpaRepository<DMSFile> dmsFileRepo = JpaRepository.of(DMSFile.class);
    final Queue<Long> parentIds = new ArrayDeque<>();
    parentIds.add(entity.getFile().getId());

    while (!parentIds.isEmpty()) {
      final Long parentId = parentIds.remove();

      final Query<DMSFile> query =
          dmsFileRepo
              .all()
              .filter("self.parent.id = :parentId")
              .bind("parentId", parentId)
              .order("id");

      List<DMSFile> results;
      int offset = 0;

      while (!(results = query.fetch(DBHelper.getJdbcFetchSize(), offset)).isEmpty()) {
        processor.accept(results);
        results.stream()
            .filter(DMSFile::getIsDirectory)
            .map(DMSFile::getId)
            .forEach(parentIds::add);
        offset += results.size();
        JPA.flush();
        JPA.clear();
      }
    }
  }

  private Stream<DMSPermission> filterPermission(
      List<DMSPermission> permissions, DMSPermission basePermission) {
    return Optional.ofNullable(permissions).orElse(Collections.emptyList()).stream()
        .filter(
            permission ->
                permission.getUser() != null
                        && Objects.equals(permission.getUser(), basePermission.getUser())
                    || permission.getGroup() != null
                        && Objects.equals(permission.getGroup(), basePermission.getGroup()));
  }

  private int getValueLevel(DMSPermission entity) {
    return ImmutableMap.of("READ", 1, "WRITE", 2, "FULL", 3).getOrDefault(entity.getValue(), 0);
  }
}
