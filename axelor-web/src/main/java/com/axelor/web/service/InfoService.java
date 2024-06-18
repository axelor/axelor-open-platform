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
package com.axelor.web.service;

import com.axelor.app.AppSettings;
import com.axelor.app.AvailableAppSettings;
import com.axelor.app.internal.AppFilter;
import com.axelor.auth.AuthPasswordResetService;
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
import com.axelor.db.tenants.TenantInfo;
import com.axelor.db.tenants.TenantResolver;
import com.axelor.i18n.I18n;
import com.axelor.inject.Beans;
import com.axelor.meta.db.MetaFile;
import com.axelor.meta.db.repo.MetaThemeRepository;
import com.axelor.script.CompositeScriptHelper;
import com.axelor.script.ScriptBindings;
import com.axelor.script.ScriptHelper;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

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
    map.put("description", I18n.get(SETTINGS.get(AvailableAppSettings.APPLICATION_DESCRIPTION)));
    map.put(
        "copyright",
        SETTINGS.format(
            I18n.get(SETTINGS.getProperties().get(AvailableAppSettings.APPLICATION_COPYRIGHT))));
    map.put("theme", SETTINGS.get(AvailableAppSettings.APPLICATION_THEME, null));
    map.put("lang", AppFilter.getLocale().toLanguageTag());

    final Map<String, Object> signIn = signInInfo();

    if (ObjectUtils.notEmpty(signIn)) {
      map.put("signIn", signIn);
    }

    map.put("resetPasswordEnabled", Beans.get(AuthPasswordResetService.class).isEnabled());

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
    map.put(
        "pollingInterval", SETTINGS.getInt(AvailableAppSettings.APPLICATION_POLLING_INTERVAL, 10));

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

  private Map<String, Object> signInInfo() {
    return buildMap(
        AvailableAppSettings.APPLICATION_SIGN_IN_PREFIX, List.of("title", "placeholder", "footer"));
  }

  @SuppressWarnings("unchecked")
  private static Map<String, Object> buildMap(
      String keyPrefix, Collection<String> translatableKeys) {
    final Map<String, String> config = SETTINGS.getPropertiesStartingWith(keyPrefix);
    return config.entrySet().stream()
        .collect(
            HashMap::new,
            (map, entry) -> {
              final String key =
                  inflector.camelize(entry.getKey().substring(keyPrefix.length()), true);
              final String str = entry.getValue();

              final String[] keys = key.split("\\.");
              Map<String, Object> currentMap = map;

              for (int i = 0; i < keys.length - 1; ++i) {
                currentMap =
                    (Map<String, Object>) currentMap.computeIfAbsent(keys[i], k -> new HashMap<>());
              }

              final String currentKey = keys[keys.length - 1];
              Object value = str;

              if (translatableKeys.contains(currentKey)) {
                value = I18n.get(str);
              } else if ("true".equalsIgnoreCase(str) || "false".equalsIgnoreCase(str)) {
                value = Boolean.parseBoolean(str);
              } else {
                try {
                  value = Long.parseLong(str);
                } catch (NumberFormatException e) {
                  // Ignore
                }
              }

              currentMap.put(currentKey, value);
            },
            Map::putAll);
  }

  protected Map<String, Object> authInfo(HttpServletRequest request, HttpServletResponse response) {
    final Map<String, Object> map = new HashMap<>();
    final TenantInfo tenantInfo = TenantResolver.getTenantInfo();
    final Map<String, String> tenants = tenantInfo.getTenants();
    String tenant = tenantInfo.getHostTenant();

    if (tenant == null) {
      final Set<String> tenantIds = tenants.keySet();
      final String tenantId = TenantResolver.currentTenantIdentifier();
      tenant =
          tenantIds.contains(tenantId) ? tenantId : tenantIds.stream().findFirst().orElse(null);
    }

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
    map.put("lang", AppFilter.getLocale().toLanguageTag());

    if (user.getImage() != null) {
      map.put("image", getLink(user));
    }

    map.put("action", user.getHomeAction());
    map.put("singleTab", user.getSingleTab());
    map.put("noHelp", Boolean.TRUE.equals(user.getNoHelp()));
    map.put("theme", Beans.get(MetaThemeRepository.class).fromIdentifierToName(user.getTheme()));

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
   * <p>The returned image can be a resource path string, a URL string or a MetaFile
   *
   * @param mode light or dark
   * @return user specific logo
   */
  public Object getLogo(String mode) {
    return getImage(
        SETTINGS.get(AvailableAppSettings.CONTEXT_APP_LOGO),
        mode,
        "appLogo",
        () ->
            isDark(mode)
                ? SETTINGS.get(
                    AvailableAppSettings.APPLICATION_LOGO_DARK,
                    SETTINGS.get(AvailableAppSettings.APPLICATION_LOGO, "img/axelor-dark.png"))
                : SETTINGS.get(AvailableAppSettings.APPLICATION_LOGO, "img/axelor.png"));
  }

  public Object getSignInLogo(String mode) {
    final String signInlogo =
        isDark(mode)
            ? SETTINGS.get(AvailableAppSettings.APPLICATION_SIGN_IN_PREFIX + "logo-dark")
            : SETTINGS.get(AvailableAppSettings.APPLICATION_SIGN_IN_PREFIX + "logo");
    return Optional.<Object>ofNullable(signInlogo).orElseGet(() -> getLogo(mode));
  }

  /**
   * Gets user specific application icon, or falls back to default application icon.
   *
   * <p>The returned image can be a resource path string, a URL string or a MetaFile
   *
   * @param mode light or dark
   * @return user specific application icon
   */
  public Object getIcon(String mode) {
    return getImage(
        SETTINGS.get(AvailableAppSettings.CONTEXT_APP_ICON),
        mode,
        "appIcon",
        () ->
            isDark(mode)
                ? SETTINGS.get(
                    AvailableAppSettings.APPLICATION_ICON_DARK,
                    SETTINGS.get(AvailableAppSettings.APPLICATION_ICON, "ico/favicon.ico"))
                : SETTINGS.get(AvailableAppSettings.APPLICATION_ICON, "ico/favicon.ico"));
  }

  private Object getImage(
      String contextImage, String mode, String config, Supplier<String> defaultValue) {
    Object result;

    try {
      result = getImage(contextImage, mode);
      if (ObjectUtils.notEmpty(result)) {
        return result;
      }
      return defaultValue.get();
    } catch (Exception e) {
      // Ignore
    }

    try {
      result = getConfigImage(config);
      if (ObjectUtils.notEmpty(result)) {
        return result;
      }
    } catch (Exception e) {
      // Ignore
    }

    return defaultValue.get();
  }

  private Object getImage(String imageCall, String mode) throws Exception {
    if (StringUtils.isBlank(imageCall)) {
      return null;
    }

    final String[] parts = imageCall.split("\\:", 2);

    if (parts.length != 2) {
      return null;
    }

    final String className = parts[0];
    final String methodName = parts[1];

    final Class<?> klass = Class.forName(className);
    final Method method = klass.getMethod(methodName, String.class);
    final Object bean = Beans.get(klass);

    return method.invoke(bean, mode);
  }

  // Legacy way to retrieve context image without passing mode
  @Deprecated(forRemoval = true)
  private Object getConfigImage(String name) {
    final ScriptBindings bindings = new ScriptBindings(new HashMap<>());
    final ScriptHelper scriptHelper = new CompositeScriptHelper(bindings);
    return scriptHelper.eval("__config__." + name);
  }

  private boolean isDark(String mode) {
    return "dark".equalsIgnoreCase(mode);
  }

  public String getLink(Object value) {
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
    return null;
  }
}
