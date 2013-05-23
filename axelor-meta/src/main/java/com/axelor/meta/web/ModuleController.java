package com.axelor.meta.web;

import javax.inject.Inject;

import com.axelor.meta.MetaLoader;
import com.axelor.meta.db.MetaModule;
import com.axelor.meta.db.MetaSelect;
import com.axelor.meta.db.MetaView;
import com.axelor.rpc.ActionResponse;
import com.axelor.rpc.Response;
import com.google.inject.persist.Transactional;

public class ModuleController {
	
	@Inject
	private MetaLoader loader;

	@Transactional
	public Response install(String name) {
		ActionResponse response = new ActionResponse();
		MetaModule module = MetaModule.findByName(name);
		if (module == null) {
			return response;
		}
		
		module.setInstalled(true);
		try {
			loader.loadModule(module);
			module = module.save();
		} catch (Exception e) {
			e.printStackTrace();
			module.refresh();
		}
		
		response.setReload(true);
		return response;
	}
	
	@Transactional
	public Response uninstall(String name) {
		ActionResponse response = new ActionResponse();
		MetaModule module = MetaModule.findByName(name);
		if (module == null) {
			return response;
		}
		
		module.setInstalled(false);
		try {
			MetaView.all().filter("self.module = ?1", name).remove();
			MetaSelect.all().filter("self.module = ?1", name).remove();
			module = module.save();
		} catch (Exception e) {
			module.refresh();
		}

		response.setReload(true);
		return response;
	}
}
