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
package com.axelor.auth.pac4j;

import com.axelor.auth.AuthService;
import com.axelor.auth.db.Group;
import com.axelor.auth.db.Permission;
import com.axelor.auth.db.Role;
import com.axelor.auth.db.User;
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
    final String codeOrEmail = profileService.getCodeOrEmail(profile);

    if (codeOrEmail == null) {
      return;
    }

    final User user = userRepo.findByCodeOrEmail(codeOrEmail);

    if (user == null) {
      persistUser(codeOrEmail, profile);
    } else {
      updateUser(user, profile);
    }
  }

  @Transactional
  protected void persistUser(String code, CommonProfile profile) {
    final User user = new User(code, profileService.getName(profile));
    user.setPassword(UUID.randomUUID().toString());
    user.setEmail(profileService.getEmail(profile));
    user.setLanguage(profileService.getLanguage(profile, "en"));
    user.setGroup(profileService.getGroup(profile, "users"));
    profileService.getRoles(profile).forEach(user::addRole);
    profileService.getPermissions(profile).forEach(user::addPermission);

    try {
      user.setImage(profileService.getImage(profile));
    } catch (IOException e) {
      logger.error(e.getMessage(), e);
    }

    authService.encrypt(user);
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
}
