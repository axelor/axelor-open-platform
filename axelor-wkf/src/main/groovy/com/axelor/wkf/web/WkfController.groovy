package com.axelor.wkf.web

import com.axelor.rpc.ActionRequest
import com.axelor.rpc.ActionResponse
import com.axelor.wkf.WkfRunner;
import com.axelor.wkf.db.Instance
import com.axelor.wkf.db.Workflow

class WkfController {

	/**
	 * Open all instances of the wkf.
	 *
	 * @param request
	 * @param response
	 */
	def openInstances (ActionRequest request, ActionResponse response) {
	   
		def context = request.context as Workflow
				
		response.view = [
			title: "${context.name}",
			resource: Instance.class.name,
			domain: "self.workflow.id = ${context.id}"
		]
		
   }

	/**
	 * Open all instances of the wkf.
	 *
	 * @param request
	 * @param response
	 */
	def run (ActionRequest request, ActionResponse response) {
	   
		def context = request.context as Workflow
		
		WkfRunner.runClass(Class.forName(context.metaModel.fullName));
		
   }
	
}
