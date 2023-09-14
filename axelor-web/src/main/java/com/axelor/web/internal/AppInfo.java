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
package com.axelor.web.internal;

import com.axelor.app.AppSettings;
import com.axelor.app.AvailableAppSettings;
import com.axelor.app.internal.AppFilter;
import com.axelor.auth.AuthUtils;
import com.axelor.auth.db.Group;
import com.axelor.auth.db.User;
import com.axelor.auth.db.ViewCustomizationPermission;
import com.axelor.auth.pac4j.AuthPac4jInfo;
import com.axelor.auth.pac4j.AxelorSecurityLogic;
import com.axelor.common.VersionUtils;
import com.axelor.db.mapper.Mapper;
import com.axelor.db.mapper.Property;
import com.axelor.inject.Beans;
import com.axelor.web.service.InfoService;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import javax.inject.Singleton;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.pac4j.core.profile.ProfileManager;
import org.pac4j.core.profile.factory.ProfileManagerFactory;
import org.pac4j.jee.context.JEEContext;
import org.pac4j.jee.context.session.JEESessionStore;

@Singleton
@Deprecated(since = "7.0.0")
public class AppInfo {

  private static final AppSettings SETTINGS = AppSettings.get();

  public Map<String, Object> info(final ServletContext context) {
    return info();
  }

  public Map<String, Object> info() {
    final User user = AuthUtils.getUser();
    return user != null ? info(user) : Collections.emptyMap();
  }

  public Map<String, Object> info(User user) {
    final Map<String, Object> map = new HashMap<>();
    final Group group = user.getGroup();
    final InfoService infoService = Beans.get(InfoService.class);

    // if name field is overridden
    final Property nameField = Mapper.of(User.class).getNameField();
    final Object nameValue = nameField.get(user);

    map.put("user.id", user.getId());
    map.put("user.name", nameValue);
    map.put("user.login", user.getCode());
    map.put("user.nameField", nameField.getName());
    map.put("user.lang", AppFilter.getLocale().getLanguage());
    map.put("user.action", user.getHomeAction());
    map.put("user.singleTab", user.getSingleTab());
    map.put("user.noHelp", Boolean.TRUE.equals(user.getNoHelp()));
    map.put("user.theme", user.getTheme());

    if (user.getImage() != null) {
      map.put("user.image", infoService.getLink(user, null));
    }

    if (group != null) {
      map.put("user.navigator", group.getNavigation());
      map.put("user.technical", group.getTechnicalStaff());
      map.put("user.group", group.getCode());
      map.put("user.canViewCollaboration", group.getCanViewCollaboration());
      map.put(
          "user.viewCustomizationPermission",
          Optional.ofNullable(group.getViewCustomizationPermission())
              .map(ViewCustomizationPermission::getValue));
    }
    if (user.getHomeAction() == null && group != null) {
      map.put("user.action", group.getHomeAction());
    }

    map.put("application.name", SETTINGS.get(AvailableAppSettings.APPLICATION_NAME));
    map.put("application.description", SETTINGS.get(AvailableAppSettings.APPLICATION_DESCRIPTION));
    map.put("application.version", SETTINGS.get(AvailableAppSettings.APPLICATION_VERSION));
    map.put("application.author", SETTINGS.get(AvailableAppSettings.APPLICATION_AUTHOR));
    map.put("application.copyright", SETTINGS.get(AvailableAppSettings.APPLICATION_COPYRIGHT));
    map.put("application.theme", infoService.getTheme());
    map.put("application.logo", infoService.getLogo());
    map.put("application.icon", infoService.getIcon());
    map.put("application.home", SETTINGS.get(AvailableAppSettings.APPLICATION_HOME));
    map.put("application.help", SETTINGS.get(AvailableAppSettings.APPLICATION_HELP));
    map.put("application.mode", SETTINGS.get(AvailableAppSettings.APPLICATION_MODE, "dev"));

    map.put(
        "api.pagination.max-per-page",
        SETTINGS.getInt(AvailableAppSettings.API_PAGINATION_MAX_PER_PAGE, 500));
    map.put(
        "api.pagination.default-per-page",
        SETTINGS.getInt(AvailableAppSettings.API_PAGINATION_DEFAULT_PER_PAGE, 40));
    map.put("data.upload.max-size", SETTINGS.get(AvailableAppSettings.FILE_UPLOAD_SIZE, "5"));
    map.put("application.sdk", VersionUtils.getVersion().version);

    Map<String, String> viewsProps = SETTINGS.getPropertiesStartingWith("view.");
    for (Map.Entry<String, String> entry : viewsProps.entrySet()) {
      Object value = entry.getValue();
      if ("true".equals(value) || "false".equals(value)) {
        value = Boolean.parseBoolean(value.toString());
      }
      map.put(entry.getKey(), value);
    }

    // find central client name
    final JEEContext jeeContext =
        new JEEContext(Beans.get(HttpServletRequest.class), Beans.get(HttpServletResponse.class));
    final ProfileManagerFactory profileManagerFactory =
        Beans.get(AxelorSecurityLogic.class).getProfileManagerFactory();
    final ProfileManager profileManager =
        profileManagerFactory.apply(jeeContext, JEESessionStore.INSTANCE);

    profileManager
        .getProfile()
        .filter(
            profile ->
                Beans.get(AuthPac4jInfo.class)
                    .getCentralClients()
                    .contains(profile.getClientName()))
        .ifPresent(profile -> map.put("auth.central.client", profile.getClientName()));

    return map;
  }
}
