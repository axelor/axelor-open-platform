/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2020 Axelor (<http://axelor.com>).
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
package com.axelor.auth.pac4j;

import com.axelor.app.AppSettings;
import com.axelor.app.AvailableAppSettings;
import com.axelor.auth.AuthService;
import com.axelor.auth.db.Group;
import com.axelor.auth.db.Permission;
import com.axelor.auth.db.Role;
import com.axelor.auth.db.User;
import com.axelor.auth.db.repo.GroupRepository;
import com.axelor.auth.db.repo.UserRepository;
import com.axelor.common.ObjectUtils;
import com.axelor.common.StringUtils;
import com.google.inject.persist.Transactional;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;
import javax.inject.Inject;
import org.pac4j.core.profile.CommonProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuthPac4jUserService {

  @Inject protected AuthService authService;
  @Inject protected AuthPac4jProfileService profileService;

  @Inject protected UserRepository userRepo;
  @Inject protected GroupRepository groupRepo;

  private static final String DEFAULT_GROUP_CODE;

  static {
    final AppSettings settings = AppSettings.get();
    DEFAULT_GROUP_CODE = settings.get(AvailableAppSettings.AUTH_USER_DEFAULT_GROUP, "users");
  }

  private static final Logger logger =
      LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Nullable
  public User getUser(CommonProfile profile) {
    final String codeOrEmail = profileService.getCodeOrEmail(profile);

    if (codeOrEmail != null) {
      return userRepo.findByCodeOrEmail(codeOrEmail);
    }

    return null;
  }

  public void saveUser(CommonProfile profile) {
    process(profile, true);
  }

  public void updateUser(CommonProfile profile) {
    process(profile, false);
  }

  private void process(CommonProfile profile, boolean withCreate) {
    final User user = getUser(profile);

    if (user == null) {
      if (withCreate) {
        persistUser(profile);
      }
    } else {
      updateUser(user, profile);
    }
  }

  @Transactional
  protected void persistUser(CommonProfile profile) {
    final User user =
        new User(profileService.getCodeOrEmail(profile), profileService.getName(profile));
    user.setPassword(authService.encrypt(UUID.randomUUID().toString()));

    updateUser(user, profile);

    if (user.getGroup() == null) {
      user.setGroup(groupRepo.findByCode(getDefaultGroupCode()));
    }

    userRepo.persist(user);
    logger.info("User(code={}) created from {}", user.getCode(), profile);
  }

  @Transactional
  protected void updateUser(User user, CommonProfile profile) {
    if (StringUtils.notBlank(profile.getDisplayName())) {
      user.setName(profile.getDisplayName());
    }

    if (StringUtils.notBlank(profile.getEmail())) {
      user.setEmail(profile.getEmail());
    }

    final String language = profileService.getLanguage(profile);
    if (StringUtils.notBlank(language)) {
      user.setLanguage(language);
    }

    final Group group = profileService.getGroup(profile);
    if (group != null) {
      user.setGroup(group);
    }

    final Set<Role> roles = profileService.getRoles(profile);
    if (!roles.isEmpty()) {
      user.clearRoles();
      roles.forEach(user::addRole);
    }

    final Set<Permission> permissions = profileService.getPermissions(profile);
    if (!permissions.isEmpty()) {
      user.clearPermissions();
      permissions.forEach(user::addPermission);
    }

    try {
      final byte[] image = profileService.getImage(profile);
      if (ObjectUtils.notEmpty(image)) {
        user.setImage(image);
      }
    } catch (IOException e) {
      logger.error(e.getMessage(), e);
    }
  }

  protected String getDefaultGroupCode() {
    return DEFAULT_GROUP_CODE;
  }
}
