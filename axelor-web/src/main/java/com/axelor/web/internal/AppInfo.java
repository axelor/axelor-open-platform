/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2018 Axelor (<http://axelor.com>).
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
package com.axelor.web.internal;

import com.axelor.app.AppSettings;
import com.axelor.app.internal.AppFilter;
import com.axelor.auth.AuthUtils;
import com.axelor.auth.db.Group;
import com.axelor.auth.db.User;
import com.axelor.common.StringUtils;
import com.axelor.common.VersionUtils;
import com.axelor.db.mapper.Mapper;
import com.axelor.db.mapper.Property;
import com.axelor.meta.db.MetaFile;
import com.axelor.script.CompositeScriptHelper;
import com.axelor.script.ScriptBindings;
import com.axelor.script.ScriptHelper;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.ServletContext;
import javax.servlet.jsp.JspException;

public class AppInfo {

  private static final AppSettings SETTINGS = AppSettings.get();
  private static final String APP_THEME = SETTINGS.get("application.theme", null);

  public Map<String, Object> info(final ServletContext context) {

    final Map<String, Object> map = new HashMap<>();
    final User user = AuthUtils.getUser();

    if (user == null) {
      return map;
    }

    final Group group = user.getGroup();

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
    map.put("user.noHelp", user.getNoHelp() == Boolean.TRUE);

    if (user.getImage() != null) {
      map.put("user.image", getLink(user, null));
    } else {
      map.put("user.image", "img/user.png");
    }

    if (group != null) {
      map.put("user.navigator", group.getNavigation());
      map.put("user.technical", group.getTechnicalStaff());
      map.put("user.group", group.getCode());
    }
    if (user.getHomeAction() == null && group != null) {
      map.put("user.action", group.getHomeAction());
    }

    map.put("application.name", SETTINGS.get("application.name"));
    map.put("application.description", SETTINGS.get("application.description"));
    map.put("application.version", SETTINGS.get("application.version"));
    map.put("application.author", SETTINGS.get("application.author"));
    map.put("application.copyright", SETTINGS.get("application.copyright"));
    map.put("application.home", SETTINGS.get("application.home"));
    map.put("application.help", SETTINGS.get("application.help"));
    map.put("application.mode", SETTINGS.get("application.mode", "dev"));

    map.put("file.upload.size", SETTINGS.get("file.upload.size", "5"));
    map.put("application.sdk", VersionUtils.getVersion().version);

    for (String key : SETTINGS.getProperties().stringPropertyNames()) {
      if (key.startsWith("view.")) {
        Object value = SETTINGS.get(key);
        if ("true".equals(value) || "false".equals(value)) {
          value = Boolean.parseBoolean(value.toString());
        }
        map.put(key, value);
      }
    }

    final List<String> themes = new ArrayList<>();
    for (String path : context.getResourcePaths("/css")) {
      try {
        if (path.endsWith("/") && context.getResource(path + "theme.css") != null) {
          path = path.replace("/css/", "").replace("/", "");
          themes.add(path);
        }
      } catch (MalformedURLException e) {
      }
    }

    Collections.sort(themes);

    map.put("application.themes", themes);

    return map;
  }

  public String getStyle() {
    if (SETTINGS.get("context.appStyle") != null) {
      final ScriptBindings bindings = new ScriptBindings(new HashMap<>());
      final ScriptHelper helper = new CompositeScriptHelper(bindings);
      try {
        Object style = helper.eval("__config__.appStyle");
        if (style instanceof String) {
          return style.toString();
        }
      } catch (Exception e) {
      }
    }
    return null;
  }

  public String getLogo() throws JspException, IOException {
    final String logo = SETTINGS.get("application.logo", "img/axelor-logo.png");
    if (SETTINGS.get("context.appLogo") != null) {
      final ScriptBindings bindings = new ScriptBindings(new HashMap<>());
      final ScriptHelper helper = new CompositeScriptHelper(bindings);
      try {
        return getLink(helper.eval("__config__.appLogo"), logo);
      } catch (Exception e) {
      }
    }
    return logo;
  }

  public String getPageLang() {
    String lang = AppFilter.getLocale().getLanguage();
    return lang == null ? "en" : lang.substring(0, 2).toLowerCase();
  }

  public String getTheme() {
    final User user = AuthUtils.getUser();
    if (user == null || StringUtils.isBlank(user.getTheme()) || "default".equals(user.getTheme())) {
      return APP_THEME;
    }
    return user.getTheme();
  }

  private String getLink(Object value, String defaultValue) {
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
