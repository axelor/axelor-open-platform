package com.axelor.meta.web;

import java.util.Map;

import javax.inject.Inject;

import com.axelor.meta.db.MetaFilter;
import com.axelor.meta.db.MetaUser;
import com.axelor.meta.service.MetaUserService;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.google.common.collect.Maps;

public class MetaUserController {

	@Inject
	private MetaUserService service;
	
	public void load(ActionRequest request, ActionResponse response) {
		MetaUser prefs = service.getPreferences();
		Map<String, Object> values = Maps.newHashMap();
		
		values.put("id", prefs.getId());
		
		response.setValues(values);
		response.setReload(true);
		response.setStatus(ActionResponse.STATUS_SUCCESS);
	}
	
	public void saveFilter(ActionRequest request, ActionResponse response) {
		MetaFilter ctx = request.getContext().asType(MetaFilter.class);
		if (ctx != null) {
			ctx = service.saveFilter(ctx);
			response.setData(ctx);
		}
	}
	
	public void removeFilter(ActionRequest request, ActionResponse response) {
		MetaFilter ctx = request.getContext().asType(MetaFilter.class);
		if (ctx != null) {
			ctx = service.removeFilter(ctx);
			response.setData(ctx);
		}
	}

	public void findFilters(ActionRequest request, ActionResponse response) {
		MetaFilter ctx = request.getContext().asType(MetaFilter.class);
		if (ctx != null && ctx.getFilterView() != null) {
			response.setData(service.getFilters(ctx.getFilterView()));
		}
	}
}
