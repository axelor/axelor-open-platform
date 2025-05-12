/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2025 Axelor (<http://axelor.com>).
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
package com.axelor.auth.web;

import com.axelor.auth.AuthUtils;
import com.axelor.auth.UserTokenService;
import com.axelor.auth.db.User;
import com.axelor.auth.db.UserToken;
import com.axelor.auth.db.repo.UserRepository;
import com.axelor.auth.db.repo.UserTokenRepository;
import com.axelor.i18n.I18n;
import com.axelor.meta.schema.actions.ActionView;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.google.inject.Inject;
import java.time.LocalDateTime;

public class UserTokenController {

  @Inject UserTokenService userTokenService;
  @Inject UserTokenRepository userTokenRepository;
  @Inject UserRepository userRepository;

  public void createToken(ActionRequest request, ActionResponse response) {
    try {
      UserToken userToken = request.getContext().asType(UserToken.class);
      User owner = request.getContext().getParent().asType(User.class);
      owner = userRepository.find(owner.getId());
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
      UserToken userToken = request.getContext().asType(UserToken.class);
      userToken = userTokenRepository.find(userToken.getId());
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
      UserToken userToken = request.getContext().asType(UserToken.class);
      userToken = userTokenRepository.find(userToken.getId());
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
