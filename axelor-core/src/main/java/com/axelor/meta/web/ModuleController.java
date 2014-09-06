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

import java.util.List;

import com.axelor.i18n.I18n;
import com.axelor.meta.db.MetaModule;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.axelor.rpc.Response;
import com.google.inject.persist.Transactional;

public class ModuleController {
	
	private static final String message = I18n.get("Restart the server for updates to take effect.");
	
	@Transactional
	protected void doAction(String name, boolean uninstall) {
		final MetaModule module = MetaModule.findByName(name);
		if (module == null) {
			throw new IllegalArgumentException("No such module: " + name);
		}
		module.setInstalled(!uninstall);
		module.setPending(true);
	}

	public Response install(final String name) {
		final ActionResponse response = new ActionResponse();
		try {
			doAction(name, false);
			response.setFlash(message);
			response.setReload(true);
		} catch (Exception e) {
			response.setException(e);
		}
		return response;
	}

	public void install(ActionRequest request, ActionResponse response) {
		List<?> ids = null;
		try {
			ids = (List<?>) request.getContext().get("_ids");
		} catch (Exception e) {
		}
		
		if (ids == null || ids.isEmpty()) {
			return;
		}
		try {
			for (MetaModule m : MetaModule.all().filter("id in (:ids)").bind("ids", ids).fetch()) {
				if (m.getInstalled() != Boolean.TRUE) {
					doAction(m.getName(), false);
				}
			}
			response.setFlash(message);
			response.setReload(true);
		} catch (Exception e) {
			response.setException(e);
		}
	}

	@Transactional
	public Response uninstall(final String name) {
		final ActionResponse response = new ActionResponse();
		try {
			doAction(name, true);
			response.setFlash(message);
			response.setReload(true);
		} catch (Exception e) {
			response.setException(e);
		}
		return response;
	}
}
