/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.auth.web;

import com.axelor.auth.AuthUtils;
import com.axelor.auth.UserTokenService;
import com.axelor.auth.db.User;
import com.axelor.auth.db.UserToken;
import com.axelor.auth.db.repo.UserTokenRepository;
import com.axelor.db.JPA;
import com.axelor.i18n.I18n;
import com.axelor.meta.schema.actions.ActionView;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.google.inject.Inject;
import java.time.LocalDateTime;

public class UserTokenController {

  @Inject UserTokenService userTokenService;
  @Inject UserTokenRepository userTokenRepository;

  public void loadUserTokens(ActionRequest request, ActionResponse response) {
    Long userId = (Long) request.getContext().get("id");

    if (userId == null || userId <= 0) {
      return;
    }

    User user = JPA.find(User.class, userId);
    if (!isNotAuthorized(user)) {
      response.setValue("_xActiveUserTokens", userTokenService.getTokenData(user));
    }
  }

  public void createToken(ActionRequest request, ActionResponse response) {
    try {
      UserToken userToken = request.getContext().asType(UserToken.class);
      User owner =
          JPA.find(User.class, Long.valueOf(request.getContext().get("_xUserId").toString()));
      if (owner == null) {
        response.setError(I18n.get("API key should be attached to a valid user"));
        return;
      }
      if (isInvalidExpirationDate(userToken)) {
        response.setError(I18n.get("Expiration date is invalid"));
        return;
      }
      if (isNotAuthorized(owner)) {
        response.setError(I18n.get("You are not authorized to create API key for this user"));
        return;
      }

      userToken =
          userTokenService.createUserToken(userToken.getName(), userToken.getExpiresAt(), owner);
      response.setCanClose(true);
      response.setView(
          ActionView.define("API key")
              .model(UserToken.class.getName())
              .add("form", "user-token-api-key-form")
              .param("popup", "true")
              .param("show-toolbar", "false")
              .param("show-confirm", "false")
              .param("popup-save", "false")
              .context("_apiKey", userToken.getApiKey())
              .map());
    } catch (Exception e) {
      response.setError(I18n.get("Unable to create API key"));
    }
  }

  public void revokeToken(ActionRequest request, ActionResponse response) {
    try {
      Long userTokenId = Long.valueOf(request.getContext().get("_userTokenId").toString());
      UserToken userToken = userTokenRepository.find(userTokenId);
      if (userToken == null) {
        response.setError(I18n.get("API key not found"));
        return;
      }
      if (isNotAuthorized(userToken.getOwner())) {
        response.setError(I18n.get("You are not authorized to revoke this API key"));
        return;
      }
      userTokenService.revokeUserToken(userToken);

      response.setReload(true);
    } catch (Exception e) {
      response.setError(I18n.get("Unable to revoke API key"));
    }
  }

  public void rotateToken(ActionRequest request, ActionResponse response) {
    try {
      Long userTokenId = Long.valueOf(request.getContext().get("_userTokenId").toString());
      UserToken userToken = userTokenRepository.find(userTokenId);
      if (userToken == null) {
        response.setError(I18n.get("API key not found"));
        return;
      }
      if (isNotAuthorized(userToken.getOwner())) {
        response.setError(I18n.get("You are not authorized to rotate this API key"));
        return;
      }
      userToken = userTokenService.rotateUserToken(userToken);
      response.setView(
          ActionView.define("API key")
              .model(UserToken.class.getName())
              .add("form", "user-token-api-key-form")
              .param("popup", "true")
              .param("show-toolbar", "false")
              .param("show-confirm", "false")
              .param("popup-save", "false")
              .context("_apiKey", userToken.getApiKey())
              .map());
    } catch (Exception e) {
      response.setError(I18n.get("Unable to rotate API key"));
    }
  }

  public void showApiKey(ActionRequest request, ActionResponse response) {
    Object apiKey = request.getContext().get("_apiKey");
    if (apiKey != null) {
      response.setValue("apiKey", apiKey.toString());
    }
  }

  private boolean isNotAuthorized(User user) {
    User currentUser = AuthUtils.getUser();
    if (currentUser == null || user == null) {
      return true;
    }
    return !currentUser.getId().equals(user.getId()) && !AuthUtils.isAdmin(currentUser);
  }

  private boolean isInvalidExpirationDate(UserToken userToken) {
    return userToken.getExpiresAt() == null
        || userToken.getExpiresAt().isBefore(LocalDateTime.now());
  }
}
