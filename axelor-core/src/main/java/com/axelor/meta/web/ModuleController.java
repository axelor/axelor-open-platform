/**
 * Copyright (c) 2012-2014 Axelor. All Rights Reserved.
 *
 * The contents of this file are subject to the Common Public
 * Attribution License Version 1.0 (the "License"); you may not use
 * this file except in compliance with the License. You may obtain a
 * copy of the License at:
 *
 * http://license.axelor.com/.
 *
 * The License is based on the Mozilla Public License Version 1.1 but
 * Sections 14 and 15 have been added to cover use of software over a
 * computer network and provide for limited attribution for the
 * Original Developer. In addition, Exhibit A has been modified to be
 * consistent with Exhibit B.
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 *
 * The Original Code is part of "Axelor Business Suite", developed by
 * Axelor exclusively.
 *
 * The Original Developer is the Initial Developer. The Initial Developer of
 * the Original Code is Axelor.
 *
 * All portions of the code written by Axelor are
 * Copyright (c) 2012-2014 Axelor. All Rights Reserved.
 */
package com.axelor.meta.web;

import javax.inject.Inject;

import com.axelor.db.JPA;
import com.axelor.meta.db.MetaAction;
import com.axelor.meta.db.MetaMenu;
import com.axelor.meta.db.MetaModule;
import com.axelor.meta.db.MetaSelect;
import com.axelor.meta.db.MetaView;
import com.axelor.meta.loader.ModuleManager;
import com.axelor.rpc.ActionResponse;
import com.axelor.rpc.Response;
import com.google.common.base.Throwables;

public class ModuleController {
	
	@Inject
	private ModuleManager loader;

	public void doInstall(String name) {
		MetaModule module = MetaModule.findByName(name);
		if (module == null || module.getInstalled() == Boolean.TRUE) {
			return;
		}
		
		module.setInstalled(true);
		try {
			loader.install(module.getName(), true, false);
			module.save();
		} catch (Exception e) {
			module.setInstalled(false);
			Throwables.propagate(e);
		}
	}

	private void doUninstall(String name) {
		MetaModule module = MetaModule.findByName(name);
		if (module == null || module.getInstalled() == Boolean.FALSE) {
			return;
		}
		module.setInstalled(false);
		try {
			MetaView.findByModule(name).remove();
			MetaSelect.findByModule(name).remove();
			MetaMenu.findByModule(name).remove();
			MetaAction.findByModule(name).remove();
			module.save();
		} catch (Exception e) {
			module.setInstalled(true);
			Throwables.propagate(e);
		}
	}
	
	public Response install(final String name) {
		
		final ActionResponse response = new ActionResponse();
		
		JPA.runInTransaction(new Runnable() {
			
			@Override
			public void run() {
				try {
					doInstall(name);
					response.setReload(true);
				} catch (Exception e) {
					response.setException(e);
				}
			}
		});
		return response;
	}
	
	public Response uninstall(final String name) {
		
		final ActionResponse response = new ActionResponse();
		
		JPA.runInTransaction(new Runnable() {
			
			@Override
			public void run() {
				try {
					doUninstall(name);
					response.setReload(true);
				} catch (Exception e) {
					response.setException(e);
				}
			}
		});
		return response;
	}
}
