/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.auth.web;

import com.axelor.auth.AuthService;
import com.axelor.auth.AuthSessionService;
import com.axelor.auth.AuthUtils;
import com.axelor.auth.MFAService;
import com.axelor.auth.UserAuthenticationEvent;
import com.axelor.auth.db.AuthenticationEvent;
import com.axelor.auth.db.User;
import com.axelor.auth.db.repo.UserRepository;
import com.axelor.auth.events.AuthenticationEventService;
import com.axelor.auth.identity.IdentityVerificationService;
import com.axelor.auth.pac4j.local.ChangePasswordException;
import com.axelor.common.StringUtils;
import com.axelor.db.JPA;
import com.axelor.db.JpaRepository;
import com.axelor.i18n.I18n;
import com.axelor.inject.Beans;
import com.axelor.meta.schema.actions.ActionView;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import java.util.List;
import java.util.Objects;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.session.Session;
import org.apache.shiro.subject.Subject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UserController {

  private static final Logger log = LoggerFactory.getLogger(UserController.class);

  private static final int AUTHENTICATION_EVENTS_PAGE_SIZE = 20;

  public void onSave(ActionRequest request, ActionResponse response) {
    var mfaService = Beans.get(MFAService.class);
    var user = request.getContext().asType(User.class);
    var mfa = mfaService.getRelatedMfa(user, false);

    if (mfa != null
        && Boolean.TRUE.equals(mfa.getIsEmailValidated())
        && !Objects.equals(user.getEmail(), mfa.getEmail())) {
      mfaService.removeEmail(mfa);

      response.setAlert(
          I18n.get(
              "You have changed your email address. Please reconfigure it for multi-factor"
                  + " authentication."));
    }
  }

  public void showPasswordRequirements(ActionRequest request, ActionResponse response) {
    response.setValue(
        "_passwordRequirements", AuthService.getInstance().getPasswordPolicyDescriptions());
  }

  public void showChangePassword(ActionRequest request, ActionResponse response) {
    var identityVerificationService = Beans.get(IdentityVerificationService.class);

    if (identityVerificationService.requiresIdentityCheck()) {
      response.setRequestIdentityCheck(request.getAction());
      return;
    }

    User user = request.getContext().asType(User.class);

    response.setView(
        ActionView.define(I18n.get("Change Password"))
            .model(User.class.getName())
            .add("form", "user-change-password-form")
            .param("popup", "true")
            .param("popup-save", "false")
            .param("show-toolbar", "false")
            .param("forceEdit", "true")
            .context("_showRecord", user.getId())
            .map());
  }

  public void changePassword(ActionRequest request, ActionResponse response) {
    var identityVerificationService = Beans.get(IdentityVerificationService.class);

    if (identityVerificationService.requiresIdentityCheck()) {
      response.setRequestIdentityCheck(request.getAction());
      return;
    }

    User currentUser = AuthUtils.getUser();
    Long targetUserId = request.getContext().asType(User.class).getId();
    User targetUser = Beans.get(UserRepository.class).find(targetUserId);

    if (currentUser == null
        || targetUser == null
        || (!Objects.equals(currentUser.getId(), targetUser.getId())
            && !AuthUtils.isAdmin(currentUser))) {
      response.setError(I18n.get("You are not authorized to change this user password."));
      return;
    }

    String newPassword = (String) request.getContext().get("newPassword");
    String chkPassword = (String) request.getContext().get("chkPassword");

    if (StringUtils.isBlank(newPassword)) {
      response.setError(I18n.get("New password is required."));
      return;
    }

    if (!newPassword.equals(chkPassword)) {
      response.setError(I18n.get("Confirm password doesn't match with new password."));
      return;
    }

    try {
      JPA.runInTransaction(() -> AuthService.getInstance().changePassword(targetUser, newPassword));
    } catch (ChangePasswordException e) {
      response.setError(e.getMessage());
      return;
    }

    identityVerificationService.clearIdentityCheck();
    response.setNotify(I18n.get("Password changed successfully."));
    response.setCanClose(true);
  }

  /**
   * Loads the active sessions for the requested user and sets them on the response.
   *
   * <p>Only the user themselves or an admin can load sessions.
   */
  public void loadSessions(ActionRequest request, ActionResponse response) {
    Long userId = (Long) request.getContext().get("id");

    if (userId == null || userId <= 0) {
      return;
    }

    User user = JPA.find(User.class, userId);
    if (isSelfOrAdmin(user.getCode())) {
      response.setValue(
          "_xActiveSessions", Beans.get(AuthSessionService.class).getSessionsData(user));
    }
  }

  /**
   * Revokes a specific session by ID.
   *
   * <p>The caller must either own the session or be an admin. The current session cannot be
   * revoked.
   */
  public void terminateSession(ActionRequest request, ActionResponse response) {
    String sessionId = (String) request.getContext().get("_sessionId");

    if (sessionId == null) {
      return;
    }

    Subject target =
        new Subject.Builder(SecurityUtils.getSecurityManager()).sessionId(sessionId).buildSubject();

    if (isCurrentSession(target) || !isSelfOrAdmin((String) target.getPrincipal())) {
      response.setError(I18n.get("You are not authorized to revoke this session."));
      return;
    }

    try {
      Beans.get(AuthSessionService.class).terminateSession(target);
      response.setNotify(I18n.get("Session has been revoked"));
      response.setReload(true);
    } catch (Exception e) {
      response.setError(
          I18n.get("Unable to revoke this session. Please try again or contact an administrator."));
    }
  }

  /**
   * Checks whether the current user is the given target user or an admin.
   *
   * <p>Used to restrict access to a user's security data, such as their sessions and authentication
   * events, to the user themselves and to admins.
   *
   * @param targetCode the unique code of the target user
   * @return true if the current user is an admin or the target user; false otherwise.
   */
  private boolean isSelfOrAdmin(String targetCode) {
    User currentUser = AuthUtils.getUser();
    if (StringUtils.isBlank(targetCode) || currentUser == null) {
      return false;
    }
    return AuthUtils.isAdmin(currentUser) || currentUser.getCode().equals(targetCode);
  }

  /**
   * Checks if the target subject is part of the current session.
   *
   * @param target the subject whose session is to be compared with the current session
   * @return true if the target subject's session matches the current subject's session; false
   *     otherwise
   */
  private boolean isCurrentSession(Subject target) {
    Subject currentSubject = AuthUtils.getSubject();
    if (currentSubject == null) {
      return false;
    }
    Session currentSession = currentSubject.getSession(false);
    Session targetSession = target.getSession(false);
    if (currentSession == null || targetSession == null) {
      return false;
    }
    return currentSession.getId().equals(targetSession.getId());
  }

  /**
   * Loads a page of authentication events for the requested user and sets it on the response.
   *
   * <p>The page starts at the {@code _offset} context value (defaults to {@code 0}). Only the user
   * themselves or an admin can load authentication events, and only with the read permission on
   * {@link AuthenticationEvent}.
   */
  public void loadAuthenticationEvents(ActionRequest request, ActionResponse response) {
    Long userId = (Long) request.getContext().get("id");

    if (userId == null || userId <= 0) {
      return;
    }

    User user = JPA.find(User.class, userId);
    if (!isSelfOrAdmin(user.getCode())
        || !JpaRepository.of(AuthenticationEvent.class).isPermitted()) {
      return;
    }

    final int offset = getOffset(request.getContext().get("_offset"));

    // fetch one more event than the page size to know whether there is a next page
    final List<UserAuthenticationEvent> events =
        Beans.get(AuthenticationEventService.class)
            .loadAuthenticationEvents(user, offset, AUTHENTICATION_EVENTS_PAGE_SIZE);
    final boolean hasNext = events.size() > AUTHENTICATION_EVENTS_PAGE_SIZE;

    response.setValue(
        "_xAuthenticationEvents",
        hasNext ? events.subList(0, AUTHENTICATION_EVENTS_PAGE_SIZE) : events);
    response.setValue("_xPageOffset", offset);
    response.setValue("_xPageSize", AUTHENTICATION_EVENTS_PAGE_SIZE);
    response.setValue("_xPageHasNext", hasNext);
  }

  private int getOffset(Object value) {
    if (value instanceof Number number) {
      return Math.max(0, number.intValue());
    }
    if (value instanceof String text && StringUtils.notBlank(text)) {
      try {
        return Math.max(0, Integer.parseInt(text.trim()));
      } catch (NumberFormatException e) {
        // ignore, use first page
      }
    }
    return 0;
  }
}
