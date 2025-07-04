/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.auth.pac4j;

import com.axelor.auth.AuthUtils;
import com.axelor.auth.MFAService;
import com.axelor.auth.db.MFA;
import com.axelor.auth.db.MFAMethod;
import com.axelor.auth.db.User;
import com.axelor.auth.db.repo.MFARepository;
import com.axelor.auth.pac4j.local.AxelorFormClient;
import com.axelor.common.ObjectUtils;
import com.axelor.inject.Beans;
import io.buji.pac4j.profile.ShiroProfileManager;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.shiro.SecurityUtils;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.context.session.SessionStore;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.core.profile.UserProfile;

public class AxelorProfileManager extends ShiroProfileManager {
  public static final String PENDING_USER_NAME = "pendingUserName";
  public static final String PENDING_PROFILE = "pendingProfile";
  public static final String FULLY_AUTHENTICATED = "isFullyAuthenticated";
  public static final String AVAILABLE_MFA_METHODS = "availableMFAMethods";

  private static final String PENDING_CLIENT_NAME = "pendingClientName";

  public AxelorProfileManager(WebContext context, SessionStore sessionStore) {
    super(context, sessionStore);
  }

  @Override
  protected void saveAll(LinkedHashMap<String, UserProfile> profiles, boolean saveInSession) {
    Set<String> indirectClientNames = Beans.get(ClientListService.class).getIndirectClientNames();

    LinkedHashMap<String, UserProfile> extractedProfiles =
        profiles.entrySet().stream()
            .filter(entry -> shouldKeepProfile(entry.getValue(), indirectClientNames))
            .collect(
                Collectors.toMap(
                    Map.Entry::getKey, Map.Entry::getValue, (v1, v2) -> v1, LinkedHashMap::new));

    super.saveAll(extractedProfiles, saveInSession);

    if (ObjectUtils.isEmpty(profiles)) {
      removeSession();
    }
  }

  private void removeSession() {
    try {
      SecurityUtils.getSubject().logout();
    } catch (Exception e) {
      // ignore
    }
  }

  private boolean shouldKeepProfile(UserProfile profile, Set<String> indirectClientNames) {
    String clientName = profile.getClientName();

    if (!indirectClientNames.contains(clientName)) {
      return true;
    }

    boolean isFullyAuthenticated =
        sessionStore.get(context, FULLY_AUTHENTICATED).filter(Boolean.TRUE::equals).isPresent();

    if (isFullyAuthenticated) {
      if (AxelorFormClient.class.getSimpleName().equals(clientName)) {
        sessionStore
            .get(context, PENDING_CLIENT_NAME)
            .ifPresent(
                pendingClientName -> {
                  sessionStore.set(context, PENDING_CLIENT_NAME, null);
                  profile.setClientName(pendingClientName.toString());
                });
      }
      return true;
    }

    String username =
        profile instanceof CommonProfile commonProfile
            ? Beans.get(AuthPac4jProfileService.class).getUserIdentifier(commonProfile)
            : profile.getUsername();

    User user = AuthUtils.getUser(username);

    if (user == null) {
      return true;
    }

    MFA mfa = Beans.get(MFARepository.class).findByOwner(user);

    if (mfa == null || !Boolean.TRUE.equals(mfa.getEnabled())) {
      return true;
    }

    saveMfaContext(profile, user);
    return false;
  }

  private void saveMfaContext(UserProfile profile, User user) {
    MFAService mfaService = Beans.get(MFAService.class);
    List<MFAMethod> methods = mfaService.getMethods(user);

    // this can throw DisabledSessionException in case sessions are disabled.
    sessionStore.set(context, PENDING_USER_NAME, user.getCode());
    sessionStore.set(context, AVAILABLE_MFA_METHODS, methods);
    sessionStore.set(context, PENDING_PROFILE, profile);
    sessionStore.set(context, PENDING_CLIENT_NAME, profile.getClientName());
  }
}
