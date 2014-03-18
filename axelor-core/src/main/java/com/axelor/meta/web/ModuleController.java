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

import javax.inject.Inject;

import com.axelor.meta.loader.ModuleManager;
import com.axelor.rpc.ActionResponse;
import com.axelor.rpc.Response;
import com.google.inject.persist.Transactional;

public class ModuleController {
	
	@Inject
	private ModuleManager loader;

	public Response install(final String name) {
		final ActionResponse response = new ActionResponse();
		try {
			loader.install(name, false, false);
			response.setReload(true);
		} catch (Exception e) {
			response.setException(e);
		}
		return response;
	}

	@Transactional
	public Response uninstall(final String name) {
		final ActionResponse response = new ActionResponse();
		try {
			loader.uninstall(name);
			response.setReload(true);
		} catch (Exception e) {
			response.setException(e);
		}
		return response;
	}
}
