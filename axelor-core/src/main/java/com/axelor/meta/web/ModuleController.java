/**
 * Axelor Business Solutions
 *
 * Copyright (C) 2012-2014 Axelor (<http://axelor.com>).
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

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import com.axelor.i18n.I18n;
import com.axelor.meta.db.MetaModule;
import com.axelor.meta.loader.ModuleManager;
import com.axelor.rpc.ActionResponse;
import com.axelor.rpc.Response;
import com.google.inject.persist.Transactional;

public class ModuleController {
	
	@Inject
	private ModuleManager loader;
	
	private String doAction(String module, boolean uninstall) {
		final List<String> previous = new ArrayList<>();
		final List<String> updated = new ArrayList<>();
		final StringBuilder sb = new StringBuilder();
		
		for (MetaModule m : MetaModule.all().filter("installed = true").fetch()) {
			previous.add(m.getName());
		}
		
		if (uninstall) {
			loader.uninstall(module);
			sb.append(I18n.get("Following modules have been uninstalled:"));
		} else {
			loader.install(module, false, false);
			sb.append(I18n.get("Following modules have been installed:"));
		}
		
		for (MetaModule m : MetaModule.all().filter("installed = true").fetch()) {
			String name = m.getName();
			if (uninstall) {
				previous.remove(name);
			} else if (!previous.contains(name)) {
				updated.add(name);
			}
		}
		
		if (uninstall) {
			updated.clear();
			updated.addAll(previous);
		}
		
		sb.append("<br><br>");
		sb.append("<ul>");
		for (String mn : updated) {
			sb.append("<li>").append(mn).append("</li>");
		}
		sb.append("</ul>");
		
		return sb.toString();
	}

	public Response install(final String name) {
		final ActionResponse response = new ActionResponse();
		try {
			response.setFlash(doAction(name, false));
			response.setSignal("refresh-app", true);
		} catch (Exception e) {
			response.setException(e);
		}
		return response;
	}

	@Transactional
	public Response uninstall(final String name) {
		final ActionResponse response = new ActionResponse();
		try {
			response.setFlash(doAction(name, true));
			response.setSignal("refresh-app", true);
		} catch (Exception e) {
			response.setException(e);
		}
		return response;
	}
}
