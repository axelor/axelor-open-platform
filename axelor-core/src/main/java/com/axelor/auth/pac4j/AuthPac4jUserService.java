/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
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
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.Set;
import java.util.UUID;
import org.pac4j.core.profile.CommonProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class AuthPac4jUserService {

  @Inject private AuthService authService;

  @Inject private AuthPac4jProfileService profileService;

  @Inject private UserRepository userRepo;

  @Inject private GroupRepository groupRepo;

  private static final String DEFAULT_GROUP_CODE;

  static {
    DEFAULT_GROUP_CODE =
        AppSettings.get().get(AvailableAppSettings.AUTH_USER_DEFAULT_GROUP, "users");
  }

  protected final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Nullable
  public User getUser(CommonProfile profile) {
    final String userIdentifier = profileService.getUserIdentifier(profile);

    if (userIdentifier != null) {
      return userRepo.findByCode(userIdentifier);
    }

    return null;
  }

  public void saveUser(CommonProfile profile) {
    process(profile, true);
  }

  public void updateUser(CommonProfile profile) {
    process(profile, false);
  }

  protected void process(CommonProfile profile, boolean withCreate) {
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
        new User(profileService.getUserIdentifier(profile), profileService.getName(profile));
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
      logger.error("Cannot get image for user '{}': {}", user.getCode(), e.toString());
    }
  }

  protected String getDefaultGroupCode() {
    return DEFAULT_GROUP_CODE;
  }
}
