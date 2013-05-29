package com.axelor.wkf.web

import groovy.util.logging.Slf4j;

import javax.inject.Inject

import com.axelor.auth.AuthUtils
import com.axelor.rpc.ActionRequest
import com.axelor.rpc.ActionResponse
import com.axelor.wkf.WkfSettings
import com.axelor.wkf.db.Instance
import com.axelor.wkf.db.Workflow
import com.axelor.wkf.workflow.WorkflowImporter
import com.axelor.wkf.workflow.WorkflowService

@Slf4j
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
	 * Open editor
	 * 
	 * @param request
	 * @param response
	 */
	def openEditor( ActionRequest request, ActionResponse response ) {

		def context = request.context as Workflow
		def resource = "${WkfSettings.get().get('wkf.editor.url', '')}/p/editor?id=${context.id}&name=${context.name}&model=${context.metaModel.id}&url=${WkfSettings.get().get('application.url', '')}&lang=${WkfSettings.get().get('wkf.editor.lang', '')}&sessionId=${AuthUtils.subject.session.id.toString()}"
		
		log.debug("SIGNAVIO URL : ${resource}")
		
		response.view = [
			title : "Editor : ${context.name}",
			resource : resource,
			viewType : "html"
		]
	}
}
