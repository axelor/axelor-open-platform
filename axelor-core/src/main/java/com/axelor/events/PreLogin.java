/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.events;

import org.apache.shiro.authc.AuthenticationToken;

public class PreLogin extends LoginEvent {

  public PreLogin(AuthenticationToken token) {
    super(token);
  }
}
