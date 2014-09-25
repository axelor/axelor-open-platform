/**
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2014 Axelor (<http://axelor.com>).
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

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import com.axelor.common.StringUtils;
import com.axelor.i18n.I18n;
import com.axelor.meta.db.MetaModule;
import com.axelor.meta.db.repo.MetaModuleRepository;
import com.axelor.meta.loader.ModuleManager;
import com.axelor.rpc.ActionResponse;
import com.axelor.rpc.Response;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.google.inject.persist.Transactional;

public class ModuleController {

	private static final String messageRestart = I18n.get("Restart the server for updates to take effect.");
	private static final String alertInstall = I18n.get("Following modules will be installed : <br/> %s <br/> Are you sure ?");
	private static final String alertUninstall = I18n.get("Following modules will be uninstalled : <br/> %s <br/> Are you sure ?");
	private static final String errorDepends = I18n.get("The module can't be uninstalled because other non-removable modules depend on it.");
	private static final String errorPending = I18n.get("The module can't be uninstalled because other modules are pending. Please restart the server before.");

	@Inject
	private MetaModuleRepository modules;

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
		all.add(module.getName());

		final String depends = module.getDepends();
		if (StringUtils.isBlank(depends)) {
			return all;
		}
		for (String name : depends.trim().split("\\s*,\\s*")) {
			MetaModule mod = modules.findByName(name);
			if (mod != null && mod.getInstalled() != Boolean.TRUE) {
				all.add(name);
			}
		}
		return all;
	}

	private String getMainModule() {
		List<String> list = ModuleManager.getResolution();
		return list.get(list.size() - 1);
	}

	private Set<String> resolveLink(MetaModule module, String mainModule) {
		final Set<String> all = new HashSet<>();
		all.add(module.getName());

		for (MetaModule metaModule : modules.all().filter("self.depends LIKE ?1", "%" + module.getName() + "%").fetch()) {
			if(metaModule.getInstalled() != Boolean.TRUE || mainModule.equals(metaModule.getName())) {
				continue;
			}
			if(metaModule.getPending() == Boolean.TRUE) {
				throw new IllegalArgumentException(errorPending);
			}
			if(metaModule.getRemovable() != Boolean.TRUE) {
				throw new IllegalArgumentException(errorDepends);
			}
			all.addAll(resolveLink(metaModule, mainModule));
		}
		return all;
	}

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

	public Response validInstall(String name) {
		final ActionResponse response = new ActionResponse();
		Map<String, String> data = Maps.newHashMap();
		try {

			final MetaModule module = modules.findByName(name);
			if (module == null) {
				throw new IllegalArgumentException("No such module: " + name);
			}

			data.put("alert", String.format(alertInstall, Joiner.on("<br/>").join(resolve(module))));
			response.setData(ImmutableList.of(data));
		} catch (Exception e) {
			response.setException(e);
		}
		return response;
	}

	public Response validUninstall(String name) {
		final ActionResponse response = new ActionResponse();
		Map<String, String> data = Maps.newHashMap();
		try {

			final MetaModule module = modules.findByName(name);
			if (module == null) {
				throw new IllegalArgumentException("No such module: " + name);
			}

			data.put("alert", String.format(alertUninstall, Joiner.on("<br/>").join(resolveLink(module, getMainModule()))));
			response.setData(ImmutableList.of(data));
		} catch (Exception e) {
			response.setException(e);
		}
		return response;
	}
}
