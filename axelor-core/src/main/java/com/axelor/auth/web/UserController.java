/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.auth.web;

import com.axelor.auth.AuthService;
import com.axelor.auth.AuthUtils;
import com.axelor.auth.MFAService;
import com.axelor.auth.db.User;
import com.axelor.auth.db.repo.UserRepository;
import com.axelor.auth.identity.IdentityVerificationService;
import com.axelor.auth.pac4j.local.ChangePasswordException;
import com.axelor.common.StringUtils;
import com.axelor.db.JPA;
import com.axelor.i18n.I18n;
import com.axelor.inject.Beans;
import com.axelor.meta.schema.actions.ActionView;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import java.util.Objects;

public class UserController {

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
}
