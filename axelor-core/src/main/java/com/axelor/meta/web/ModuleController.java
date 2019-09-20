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
package com.axelor.meta.web;

import com.axelor.i18n.I18n;
import com.axelor.meta.CallMethod;
import com.axelor.meta.db.MetaModule;
import com.axelor.meta.db.repo.MetaModuleRepository;
import com.axelor.meta.loader.ModuleManager;
import com.axelor.rpc.ActionResponse;
import com.axelor.rpc.Response;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.google.inject.persist.Transactional;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Inject;

public class ModuleController {

  private static final String messageRestart =
      I18n.get("Restart the server for updates to take effect.");
  private static final String alertInstall =
      I18n.get("Following modules will be installed : <p>%s</p> Are you sure ?");
  private static final String alertUninstall =
      I18n.get("Following modules will be uninstalled : <p>%s</p> Are you sure ?");
  private static final String errorDepends =
      I18n.get("The module can't be uninstalled because other non-removable modules depend on it.");
  private static final String errorPending =
      I18n.get(
          "The module can't be uninstalled because other modules are pending. Please restart the server before.");

  @Inject private MetaModuleRepository modules;

  @Transactional
  protected void doInstall(String name) {
    final MetaModule module = modules.findByName(name);
    if (module == null) {
      throw new IllegalArgumentException("No such module: " + name);
    }

    for (String dep : resolve(module)) {
      MetaModule mod = modules.findByName(dep);
      mod.setInstalled(true);
      mod.setPending(true);
    }
  }

  @Transactional
  protected void doUninstall(String name) {
    final MetaModule module = modules.findByName(name);
    if (module == null) {
      throw new IllegalArgumentException("No such module: " + name);
    }
    for (String dep : resolveLink(module, getMainModule())) {
      MetaModule mod = modules.findByName(dep);
      mod.setInstalled(false);
      mod.setPending(true);
    }
  }

  private Set<String> resolve(MetaModule module) {
    final Set<String> all = new HashSet<>();
    final Set<MetaModule> depends = module.getDepends();

    all.add(module.getName());

    if (depends == null || depends.isEmpty()) {
      return all;
    }

    depends.stream()
        .filter(m -> m.getInstalled() != Boolean.TRUE)
        .map(MetaModule::getName)
        .forEach(all::add);

    return all;
  }

  private String getMainModule() {
    List<String> list = ModuleManager.getResolution();
    return list.get(list.size() - 1);
  }

  private Set<String> resolveLink(MetaModule module, String mainModule) {
    final Set<String> all = new HashSet<>();
    all.add(module.getName());
    for (MetaModule metaModule :
        modules.all().filter("?1 MEMBER OF self.depends", module.getId()).fetch()) {
      if (metaModule.getApplication() == Boolean.TRUE
          || metaModule.getInstalled() != Boolean.TRUE
          || mainModule.equals(metaModule.getName())) {
        continue;
      }
      if (metaModule.getPending() == Boolean.TRUE) {
        throw new IllegalArgumentException(errorPending);
      }
      if (metaModule.getRemovable() != Boolean.TRUE) {
        throw new IllegalArgumentException(errorDepends);
      }
      all.addAll(resolveLink(metaModule, mainModule));
    }
    return all;
  }

  @CallMethod
  public Response install(String name) {
    final ActionResponse response = new ActionResponse();
    try {
      doInstall(name);
      response.setFlash(messageRestart);
      response.setReload(true);
    } catch (Exception e) {
      response.setException(e);
    }
    return response;
  }

  @CallMethod
  @Transactional
  public Response uninstall(String name) {
    final ActionResponse response = new ActionResponse();
    try {
      doUninstall(name);
      response.setFlash(messageRestart);
      response.setReload(true);
    } catch (Exception e) {
      response.setException(e);
    }
    return response;
  }

  @CallMethod
  public Response validInstall(String name) {
    final ActionResponse response = new ActionResponse();
    Map<String, String> data = Maps.newHashMap();
    try {

      final MetaModule module = modules.findByName(name);
      if (module == null) {
        throw new IllegalArgumentException("No such module: " + name);
      }

      String text =
          resolve(module).stream().map(n -> "<li>" + n + "</li>").collect(Collectors.joining());

      text = "<ul>" + text + "</ul>";

      data.put("alert", String.format(alertInstall, text));
      response.setData(ImmutableList.of(data));
    } catch (Exception e) {
      response.setException(e);
    }
    return response;
  }

  @CallMethod
  public Response validUninstall(String name) {
    final ActionResponse response = new ActionResponse();
    Map<String, String> data = Maps.newHashMap();
    try {

      final MetaModule module = modules.findByName(name);
      if (module == null) {
        throw new IllegalArgumentException("No such module: " + name);
      }

      String text =
          resolveLink(module, getMainModule()).stream()
              .map(n -> "<li>" + n + "</li>")
              .collect(Collectors.joining());

      text = "<ul>" + text + "</ul>";

      data.put("alert", String.format(alertUninstall, text));
      response.setData(ImmutableList.of(data));
    } catch (Exception e) {
      response.setException(e);
    }
    return response;
  }
}
