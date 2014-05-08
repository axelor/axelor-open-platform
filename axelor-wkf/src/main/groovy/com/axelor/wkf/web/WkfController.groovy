/**
 * Axelor Business Solutions
 *
 * Copyright (C) 2012-2014 Axelor (<http://axelor.com>).
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
package com.axelor.wkf.web

import groovy.util.logging.Slf4j

import javax.inject.Inject

import com.axelor.app.AppSettings
import com.axelor.auth.AuthUtils
import com.axelor.rpc.ActionRequest
import com.axelor.rpc.ActionResponse
import com.axelor.wkf.db.Instance
import com.axelor.wkf.db.Workflow
import com.axelor.wkf.workflow.WorkflowImporter
import com.axelor.wkf.workflow.WorkflowService

@Slf4j
class WkfController {
	
	private static final String CONFIG_WKF_BASE = "workflow.editor.base";
	private static final String CONFIG_WKF_LANG = "workflow.editor.lang";

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
		def appUrl = AppSettings.get().getBaseURL();
		def wkfUrl = AppSettings.get().get(CONFIG_WKF_BASE, '')
		def wkfLang = AppSettings.get().get(CONFIG_WKF_LANG, 'fr');

		if (wkfUrl) {
			wkfUrl = appUrl.substring(appUrl.lastIndexOf('/')) + wkfUrl;
		}

		def context = request.context as Workflow
		def resource = "${wkfUrl}/p/editor?id=${context.id}&name=${context.name}&model=${context.metaModel.fullName}&url=${appUrl}&lang=${wkfLang}&sessionId=${AuthUtils.subject.session.id.toString()}"
		
		log.debug("SIGNAVIO URL : ${resource}")
		
		response.view = [
			title : "Editor : ${context.name}",
			resource : resource,
			viewType : "html"
		]
	}
}
