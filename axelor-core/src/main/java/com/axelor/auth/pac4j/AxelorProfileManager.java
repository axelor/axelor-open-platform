/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.auth.pac4j;

import com.axelor.common.ObjectUtils;
import io.buji.pac4j.profile.ShiroProfileManager;
import java.util.LinkedHashMap;
import org.apache.shiro.SecurityUtils;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.context.session.SessionStore;
import org.pac4j.core.profile.UserProfile;

public class AxelorProfileManager extends ShiroProfileManager {

  public AxelorProfileManager(WebContext context, SessionStore sessionStore) {
    super(context, sessionStore);
  }

  @Override
  protected void saveAll(LinkedHashMap<String, UserProfile> profiles, boolean saveInSession) {
    super.saveAll(profiles, saveInSession);

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
}
