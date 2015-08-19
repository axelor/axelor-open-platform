/**
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2015 Axelor (<http://axelor.com>).
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
package com.axelor.auth.web;

import org.joda.time.LocalDateTime;

import com.axelor.auth.db.PermissionAssistant;
import com.axelor.auth.service.PermissionAssistantService;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.axelor.rpc.Context;
import com.google.inject.Inject;

public class PermissionAssitantController {

	@Inject
	PermissionAssistantService assistantService;

	public void createFile(ActionRequest request, ActionResponse response){

		PermissionAssistant permissionAssistant = request.getContext().asType(PermissionAssistant.class);
		assistantService.createFile(permissionAssistant);
		response.setReload(true);

	}

	public void importPermissions(ActionRequest request, ActionResponse response){

		Context context =  request.getContext();
		PermissionAssistant permissionAssistant = context.asType(PermissionAssistant.class);
		String errors = assistantService.importPermissions(permissionAssistant);
		response.setValue("importDate", new LocalDateTime().now());
		response.setValue("log", errors);

		if(errors.equals("")){
			response.setFlash("Imported completed succesfully");
		}
		else{
			response.setFlash("Error in import. Please check log");
		}

	}

}
