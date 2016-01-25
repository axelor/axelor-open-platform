/**
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2016 Axelor (<http://axelor.com>).
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

import com.axelor.meta.db.MetaFilter;
import com.axelor.meta.service.MetaFilterService;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;

public class MetaFilterController {

	@Inject
	private MetaFilterService service;
	
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
