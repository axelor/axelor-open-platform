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
package com.axelor.web.service;

import com.axelor.app.AppSettings;
import com.axelor.app.AvailableAppSettings;
import com.axelor.app.internal.AppFilter;
import com.axelor.auth.AuthUtils;
import com.axelor.auth.db.Group;
import com.axelor.auth.db.User;
import com.axelor.auth.db.ViewCustomizationPermission;
import com.axelor.auth.pac4j.AuthPac4jInfo;
import com.axelor.auth.pac4j.AxelorSecurityLogic;
import com.axelor.auth.pac4j.ClientListProvider;
import com.axelor.common.MimeTypesUtils;
import com.axelor.common.ObjectUtils;
import com.axelor.common.StringUtils;
import com.axelor.common.VersionUtils;
import com.axelor.db.mapper.Mapper;
import com.axelor.db.mapper.Property;
import com.axelor.inject.Beans;
import com.axelor.meta.db.MetaFile;
import com.axelor.script.CompositeScriptHelper;
import com.axelor.script.ScriptBindings;
import com.axelor.script.ScriptHelper;
import com.google.inject.servlet.RequestScoped;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.inject.Inject;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.pac4j.core.profile.ProfileManager;
import org.pac4j.core.profile.factory.ProfileManagerFactory;
import org.pac4j.jee.context.JEEContext;
import org.pac4j.jee.context.session.JEESessionStore;

@RequestScoped
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Path("/public/app")
public class InfoService extends AbstractService {

  @Context private HttpServletRequest request;

  private final AuthPac4jInfo pac4jInfo;

  private final String defaultClient;
  private final boolean exclusive;

  private static final AppSettings SETTINGS = AppSettings.get();

  @Inject
  public InfoService(AuthPac4jInfo pac4jInfo, ClientListProvider clientListProvider) {
    this.pac4jInfo = pac4jInfo;
    this.defaultClient = clientListProvider.getDefaultClientName();
    this.exclusive = clientListProvider.isExclusive();
  }

  /**
   * Retrieves either application login information or session information if the user is logged in.
   */
  @GET
  @Path("info")
  public Map<String, Object> info() {
    final User user = AuthUtils.getUser();
    final Map<String, Object> map = new HashMap<>();
    map.put("application", appInfo());
    map.put("authentication", authInfo());
    if (user != null) {
      map.put("user", userInfo());
      map.put("view", viewInfo());
      map.put("api", apiInfo());
      map.put("data", dataInfo());
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

    return map;
  }

  private Map<String, Object> authInfo() {
    final Map<String, Object> map = new HashMap<>();

    map.put("callbackUrl", pac4jInfo.getCallbackUrl());

    if (ObjectUtils.notEmpty(pac4jInfo.getCentralClients())) {
      map.put("clients", clientsInfo());
    }

    if (StringUtils.notEmpty(defaultClient)) {
      map.put("defaultClient", defaultClient);
    }

    if (exclusive) {
      map.put("exclusive", exclusive);
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
        .ifPresent(profile -> map.put("currentClient", profile.getClientName()));

    return map;
  }

  private List<Object> clientsInfo() {
    final List<Object> clients = new ArrayList<>();

    for (final String client : pac4jInfo.getCentralClients()) {
      final Map<String, Object> clientMap = new HashMap<>();
      final Map<String, String> info = pac4jInfo.getClientInfo(client);
      if (info == null) {
        continue;
      }

      clientMap.put("name", client);

      final String icon = info.get("icon");
      if (StringUtils.notEmpty(icon)) {
        clientMap.put("icon", icon);
      }

      final String title = info.get("title");
      if (StringUtils.notEmpty(title)) {
        clientMap.put("title", title);
      }

      clients.add(clientMap);
    }

    return clients;
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
      map.put("canViewCollaboration", group.getCanViewCollaboration());
    }

    return map;
  }

  private Map<String, Object> viewInfo() {
    final Map<String, Object> map = new HashMap<>();

    map.put("singleTab", SETTINGS.getBoolean(AvailableAppSettings.VIEW_SINGLE_TAB, false));
    map.put("maxTabs", SETTINGS.getInt(AvailableAppSettings.VIEW_TABS_MAX, -1));

    final Map<String, Object> menubar = new HashMap<>();
    menubar.put("location", SETTINGS.get(AvailableAppSettings.VIEW_MENUBAR_LOCATION));
    map.put("menubar", menubar);

    final Map<String, Object> toolbar = new HashMap<>();
    toolbar.put("showTitles", SETTINGS.getBoolean(AvailableAppSettings.VIEW_TOOLBAR_TITLES, false));
    map.put("toolbar", toolbar);

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

    final Map<String, Object> collaboration = new HashMap<>();
    collaboration.put(
        "enabled", SETTINGS.getBoolean(AvailableAppSettings.VIEW_COLLABORATION_ENABLED, true));
    map.put("collaboration", collaboration);

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

  @GET
  @Path("logo")
  public Response getLogoContent() {
    return getImageContent(getLogo());
  }

  @GET
  @Path("icon")
  public Response getIconContent() {
    return getImageContent(getIcon());
  }

  private Response getImageContent(String pathString) {
    if (StringUtils.notEmpty(pathString)) {
      final ServletContext context = request.getServletContext();
      try (final InputStream inputStream = context.getResourceAsStream(pathString)) {
        if (inputStream != null) {
          final byte[] imageData = inputStream.readAllBytes();
          final String mediaType = MimeTypesUtils.getContentType(pathString);
          return Response.ok(imageData).type(mediaType).build();
        }
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    }

    return Response.status(Response.Status.NOT_FOUND).build();
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
