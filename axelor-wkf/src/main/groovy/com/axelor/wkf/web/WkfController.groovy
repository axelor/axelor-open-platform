/**
 * Copyright (c) 2012-2014 Axelor. All Rights Reserved.
 *
 * The contents of this file are subject to the Common Public
 * Attribution License Version 1.0 (the “License”); you may not use
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
 * Software distributed under the License is distributed on an “AS IS”
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
		def resource = "${WkfSettings.get().get('wkf.editor.url', '')}/p/editor?id=${context.id}&name=${context.name}&model=${context.metaModel.fullName}&url=${WkfSettings.get().get('application.url', '')}&lang=${WkfSettings.get().get('wkf.editor.lang', '')}&sessionId=${AuthUtils.subject.session.id.toString()}"
		
		log.debug("SIGNAVIO URL : ${resource}")
		
		response.view = [
			title : "Editor : ${context.name}",
			resource : resource,
			viewType : "html"
		]
	}
}
