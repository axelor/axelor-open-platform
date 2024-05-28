/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2024 Axelor (<http://axelor.com>).
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
package com.axelor.web.service;

import com.axelor.app.AppSettings;
import com.axelor.app.AvailableAppSettings;
import com.axelor.app.internal.AppFilter;
import com.axelor.auth.AuthUtils;
import com.axelor.auth.db.Group;
import com.axelor.auth.db.User;
import com.axelor.auth.db.ViewCustomizationPermission;
import com.axelor.auth.pac4j.AuthPac4jInfo;
import com.axelor.common.Inflector;
import com.axelor.common.ObjectUtils;
import com.axelor.common.StringUtils;
import com.axelor.common.VersionUtils;
import com.axelor.db.mapper.Mapper;
import com.axelor.db.mapper.Property;
import com.axelor.db.tenants.TenantResolver;
import com.axelor.meta.db.MetaFile;
import com.axelor.script.CompositeScriptHelper;
import com.axelor.script.ScriptBindings;
import com.axelor.script.ScriptHelper;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class InfoService extends AbstractService {

  private final AuthPac4jInfo pac4jInfo;

  protected static final AppSettings SETTINGS = AppSettings.get();

  private static final Inflector inflector = Inflector.getInstance();

  @Inject
  public InfoService(AuthPac4jInfo pac4jInfo) {
    this.pac4jInfo = pac4jInfo;
  }

  /**
   * Retrieves either application login information or session information if the user is logged in.
   */
  public Map<String, Object> info(HttpServletRequest request, HttpServletResponse response) {
    final User user = AuthUtils.getUser();
    final Map<String, Object> map = new HashMap<>();
    map.put("application", appInfo());
    map.put("authentication", authInfo(request, response));
    if (user != null) {
      map.put("user", userInfo());
      map.put("view", viewInfo());
      map.put("api", apiInfo());
      map.put("data", dataInfo());
      map.put("features", featuresInfo());
    }
    return map;
  }

  private Map<String, Object> appInfo() {
    final Map<String, Object> map = new HashMap<>();

    map.put("name", SETTINGS.get(AvailableAppSettings.APPLICATION_NAME));
    map.put("author", SETTINGS.get(AvailableAppSettings.APPLICATION_AUTHOR));
    map.put("description", SETTINGS.get(AvailableAppSettings.APPLICATION_DESCRIPTION));
    map.put("copyright", SETTINGS.get(AvailableAppSettings.APPLICATION_COPYRIGHT));
    map.put("theme", getTheme());
    map.put("logo", getLogo());
    map.put("icon", getIcon());
    map.put("lang", AppFilter.getLocale().getLanguage());

    if (AuthUtils.getUser() == null) {
      return map;
    }

    map.put("version", SETTINGS.get(AvailableAppSettings.APPLICATION_VERSION));
    map.put("home", SETTINGS.get(AvailableAppSettings.APPLICATION_HOME));
    map.put("help", SETTINGS.get(AvailableAppSettings.APPLICATION_HELP));
    map.put("mode", SETTINGS.get(AvailableAppSettings.APPLICATION_MODE, "dev"));
    map.put("aopVersion", VersionUtils.getVersion().version);
    map.put("aopBuildDate", VersionUtils.getBuildDate());
    map.put("aopGitHash", VersionUtils.getGitHash());

    map.put("swaggerUI", swaggerUIInfo());

    return map;
  }

  private Map<String, Object> swaggerUIInfo() {
    final boolean enabled =
        SETTINGS.getBoolean(AvailableAppSettings.APPLICATION_SWAGGER_UI_ENABLED, true);
    final boolean allowTryItOut =
        SETTINGS.getBoolean(AvailableAppSettings.APPLICATION_SWAGGER_UI_ALLOW_TRY_IT_OUT, false);
    return Map.of("enabled", enabled, "allowTryItOut", allowTryItOut);
  }

  protected Map<String, Object> authInfo(HttpServletRequest request, HttpServletResponse response) {
    final Map<String, Object> map = new HashMap<>();
    final Map<String, String> tenants = TenantResolver.getTenants();
    final Set<String> tenantIds = tenants.keySet();
    final String tenantId = TenantResolver.currentTenantIdentifier();
    final String tenant =
        tenantIds.contains(tenantId) ? tenantId : tenantIds.stream().findFirst().orElse(null);

    map.put("callbackUrl", pac4jInfo.getCallbackUrl());

    if (ObjectUtils.notEmpty(tenants)) {
      map.put("tenants", tenants);
    }

    if (StringUtils.notBlank(tenant)) {
      map.put("tenant", tenant);
    }

    return map;
  }

  private Map<String, Object> userInfo() {
    final User user = AuthUtils.getUser();
    if (user == null) {
      return null;
    }
    final Group group = user.getGroup();

    final Property nameField = Mapper.of(User.class).getNameField();
    final Object nameValue = nameField.get(user);

    final Map<String, Object> map = new HashMap<>();

    map.put("id", user.getId());
    map.put("login", user.getCode());
    map.put("name", nameValue);
    map.put("nameField", nameField.getName());
    map.put("lang", AppFilter.getLocale().getLanguage());

    if (user.getImage() != null) {
      map.put("image", getLink(user, null));
    }

    map.put("action", user.getHomeAction());
    map.put("singleTab", user.getSingleTab());
    map.put("noHelp", Boolean.TRUE.equals(user.getNoHelp()));
    map.put("theme", user.getTheme());

    if (group != null) {
      if (user.getHomeAction() == null) {
        map.put("action", group.getHomeAction());
      }

      map.put("group", group.getCode());
      map.put("navigator", group.getNavigation());
      map.put("technical", group.getTechnicalStaff());
      map.put(
          "viewCustomizationPermission",
          Optional.ofNullable(group.getViewCustomizationPermission())
              .map(ViewCustomizationPermission::getValue));
    }

    return map;
  }

  private Map<String, Object> viewInfo() {
    final Map<String, Object> map = new HashMap<>();

    map.put("singleTab", SETTINGS.getBoolean(AvailableAppSettings.VIEW_SINGLE_TAB, false));
    map.put("maxTabs", SETTINGS.getInt(AvailableAppSettings.VIEW_TABS_MAX, -1));

    final Map<String, Object> form = new HashMap<>();
    form.put(
        "checkVersion", SETTINGS.getBoolean(AvailableAppSettings.VIEW_FORM_CHECK_VERSION, false));
    map.put("form", form);

    final Map<String, Object> grid = new HashMap<>();
    grid.put("selection", SETTINGS.get(AvailableAppSettings.VIEW_GRID_SELECTION));
    map.put("grid", grid);

    final Map<String, Object> advancedSearch = new HashMap<>();
    advancedSearch.put(
        "exportFull", SETTINGS.getBoolean(AvailableAppSettings.VIEW_ADV_SEARCH_EXPORT_FULL, true));
    advancedSearch.put(
        "share", SETTINGS.getBoolean(AvailableAppSettings.VIEW_ADV_SEARCH_SHARE, true));
    map.put("advancedSearch", advancedSearch);

    map.put(
        "allowCustomization", SETTINGS.getBoolean(AvailableAppSettings.VIEW_CUSTOMIZATION, true));

    return map;
  }

  private Map<String, Object> apiInfo() {
    final Map<String, Object> map = new HashMap<>();
    final Map<String, Object> pagination = new HashMap<>();

    pagination.put(
        "maxPerPage", SETTINGS.getInt(AvailableAppSettings.API_PAGINATION_MAX_PER_PAGE, 500));
    pagination.put(
        "defaultPerPage",
        SETTINGS.getInt(AvailableAppSettings.API_PAGINATION_DEFAULT_PER_PAGE, 40));

    map.put("pagination", pagination);

    return map;
  }

  private Map<String, Object> dataInfo() {
    final Map<String, Object> map = new HashMap<>();

    final Map<String, Object> upload = new HashMap<>();
    upload.put("maxSize", SETTINGS.getInt(AvailableAppSettings.FILE_UPLOAD_SIZE, 5));

    map.put("upload", upload);

    return map;
  }

  private Object featuresInfo() {
    return SETTINGS.getPropertiesKeysStartingWith(AvailableAppSettings.FEATURE_PREFIX).stream()
        .collect(
            Collectors.toMap(
                key ->
                    inflector.camelize(
                        key.substring(AvailableAppSettings.FEATURE_PREFIX.length()), true),
                value -> SETTINGS.getBoolean(value, false)));
  }

  /**
   * Gets user specific application logo, or falls back to default application logo.
   *
   * @return user specific logo link
   */
  public String getLogo() {
    final String logo = SETTINGS.get(AvailableAppSettings.APPLICATION_LOGO, "img/axelor.png");
    if (SETTINGS.get(AvailableAppSettings.CONTEXT_APP_LOGO) != null) {
      final ScriptBindings bindings = new ScriptBindings(new HashMap<>());
      final ScriptHelper helper = new CompositeScriptHelper(bindings);
      try {
        return getLink(helper.eval("__config__.appLogo"), logo);
      } catch (Exception e) {
        // Ignore
      }
    }
    return logo;
  }

  /**
   * Gets user specific application icon, or falls back to default application icon.
   *
   * @return user specific application icon
   */
  public String getIcon() {
    final String icon = SETTINGS.get(AvailableAppSettings.APPLICATION_ICON, "ico/favicon.ico");
    if (SETTINGS.get(AvailableAppSettings.CONTEXT_APP_ICON) != null) {
      final ScriptBindings bindings = new ScriptBindings(new HashMap<>());
      final ScriptHelper helper = new CompositeScriptHelper(bindings);
      try {
        return getLink(helper.eval("__config__.appIcon"), icon);
      } catch (Exception e) {
        // Ignore
      }
    }
    return icon;
  }

  public String getTheme() {
    final User user = AuthUtils.getUser();
    if (user == null || StringUtils.isBlank(user.getTheme()) || "default".equals(user.getTheme())) {
      return SETTINGS.get(AvailableAppSettings.APPLICATION_THEME, null);
    }
    return user.getTheme();
  }

  public String getLink(Object value, String defaultValue) {
    if (value == null) {
      return defaultValue;
    }
    if (value instanceof String) {
      return (String) value;
    }
    if (value instanceof MetaFile) {
      return "ws/rest/"
          + MetaFile.class.getName()
          + "/"
          + ((MetaFile) value).getId()
          + "/content/download?v="
          + ((MetaFile) value).getVersion();
    }
    if (value instanceof User) {
      return "ws/rest/"
          + User.class.getName()
          + "/"
          + ((User) value).getId()
          + "/image/download?image=true&v="
          + ((User) value).getVersion();
    }
    return defaultValue;
  }
}
