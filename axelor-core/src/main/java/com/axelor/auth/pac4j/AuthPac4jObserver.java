/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2023 Axelor (<http://axelor.com>).
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

import com.axelor.app.AppSettings;
import com.axelor.app.AvailableAppSettings;
import com.axelor.event.Observes;
import com.axelor.events.PreLogin;
import java.util.Optional;
import java.util.function.Consumer;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.pac4j.core.profile.CommonProfile;

@Singleton
public class AuthPac4jObserver {

  private final Consumer<CommonProfile> profileConsumer;

  @Inject
  public AuthPac4jObserver(AuthPac4jUserService userService) {
    final AppSettings settings = AppSettings.get();
    final String userProvisioning =
        settings.get(AvailableAppSettings.AUTH_USER_PROVISIONING, "create");

    // User provisioning
    switch (userProvisioning) {
      case "create":
        // Create and update users
        profileConsumer = userService::saveUser;
        break;
      case "link":
        // Update users (must exist locally beforehand)
        profileConsumer = userService::updateUser;
        break;
      default:
        profileConsumer = profile -> {};
    }
  }

  /**
   * Observes pre-login events in order to create and update users.
   *
   * @param event
   */
  public void onPreLogin(@Observes PreLogin event) {
    @SuppressWarnings("unchecked")
    final Optional<CommonProfile> profile = (Optional<CommonProfile>) event.getPrincipal();
    profile.ifPresent(profileConsumer);
  }
}
