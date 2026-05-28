/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.auth.pac4j;

import com.axelor.app.AppSettings;
import com.axelor.app.AvailableAppSettings;
import com.axelor.event.Observes;
import com.axelor.events.PreLogin;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.Optional;
import java.util.function.Consumer;
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
