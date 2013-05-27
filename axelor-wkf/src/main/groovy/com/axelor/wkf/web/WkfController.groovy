package com.axelor.wkf.web

import javax.inject.Inject

import com.axelor.rpc.ActionRequest
import com.axelor.rpc.ActionResponse
import com.axelor.wkf.WkfSettings
import com.axelor.wkf.db.Instance
import com.axelor.wkf.db.Workflow
import com.axelor.wkf.workflow.WorkflowImporter;
import com.axelor.wkf.workflow.WorkflowService

class WkfController {
	
	@Inject
	WorkflowService workflowService
	
	@Inject
	WorkflowImporter workflowImporter
	
	def importWorkflow(ActionRequest request, ActionResponse response){
		
		def context = request.context as Workflow
		
		workflowImporter.run(context.bpmn)
		response.reload = true
		
	}

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
	 * Open ORYX editor
	 * 
	 * @param request
	 * @param response
	 */
	def showEditor(ActionRequest request, ActionResponse response) {
	   
		def context = request.context
				
		if (context?.id){
			response.view = [title: "Oryx : ${context.name}", resource: "${WkfSettings.get().get('wkf.oryx.url', '')}/model/${context.id}", viewType: "html"]
		}
		else {
			response.view = [title: "Oryx", resource: "${WkfSettings.get().get('wkf.oryx.url', '')}", viewType: "html"]
		}
	
	}
	
}
