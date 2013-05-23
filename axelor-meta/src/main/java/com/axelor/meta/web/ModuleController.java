package com.axelor.meta.web;

import javax.inject.Inject;

import com.axelor.db.JPA;
import com.axelor.meta.MetaLoader;
import com.axelor.meta.db.MetaAction;
import com.axelor.meta.db.MetaMenu;
import com.axelor.meta.db.MetaModule;
import com.axelor.meta.db.MetaSelect;
import com.axelor.meta.db.MetaView;
import com.axelor.rpc.ActionResponse;
import com.axelor.rpc.Response;
import com.google.common.base.Throwables;

public class ModuleController {
	
	@Inject
	private MetaLoader loader;

	public void doInstall(String name) {
		MetaModule module = MetaModule.findByName(name);
		if (module == null || module.getInstalled() == Boolean.TRUE) {
			return;
		}
		
		module.setInstalled(true);
		try {
			loader.loadModule(module);
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
