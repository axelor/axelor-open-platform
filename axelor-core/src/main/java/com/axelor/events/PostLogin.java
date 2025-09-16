/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.events;

import com.axelor.auth.db.User;
import org.apache.shiro.authc.AuthenticationToken;

public class PostLogin extends LoginEvent {

  public static final String SUCCESS = "success";
  public static final String FAILURE = "failure";

  private final User user;

  private final Throwable error;

  public PostLogin(AuthenticationToken token, User user, Throwable error) {
    super(token);
    this.user = user;
    this.error = error;
  }

  public User getUser() {
    return user;
  }

  public Throwable getError() {
    return error;
  }

  public boolean isSuccess() {
    return user != null && error == null;
  }

  public boolean isFailure() {
    return !isSuccess();
  }
}
