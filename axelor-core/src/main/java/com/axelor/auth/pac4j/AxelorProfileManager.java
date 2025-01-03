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
