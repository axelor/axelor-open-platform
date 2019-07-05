/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2019 Axelor (<http://axelor.com>).
 *
 * This program is free software: you can redistribute it and/or  modify
 * it under the terms of the GNU Affero General Public License, version 3,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.axelor.auth.pac4j;

import com.axelor.event.Observes;
import com.axelor.events.PreLogin;
import com.google.inject.Singleton;
import java.util.Optional;
import javax.inject.Inject;
import org.pac4j.core.profile.CommonProfile;

@Singleton
public class AuthPac4jObserver {

  @Inject protected AuthPac4jUserService userService;

  public void onPreLogin(@Observes PreLogin event) {
    @SuppressWarnings("unchecked")
    final Optional<CommonProfile> profile = (Optional<CommonProfile>) event.getPrincipal();
    profile.ifPresent(userService::saveUser);
  }
}
