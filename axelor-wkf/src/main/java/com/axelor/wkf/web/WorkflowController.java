/**
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2017 Axelor (<http://axelor.com>).
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
package com.axelor.wkf.web;

import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.axelor.app.AppSettings;
import com.axelor.app.internal.AppFilter;
import com.axelor.auth.AuthUtils;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.axelor.wkf.db.Instance;
import com.axelor.wkf.db.Workflow;
import com.axelor.wkf.service.WorkflowImporter;
import com.google.common.collect.Maps;

public class WorkflowController {

	protected Logger log = LoggerFactory.getLogger( getClass() );
	
	private static final String CONFIG_WKF_BASE = "workflow.editor.base";

	@Inject
	private WorkflowImporter workflowImporter;

	public void importWorkflow(ActionRequest request, ActionResponse response){

		Workflow workflow = request.getContext().asType( Workflow.class );
		workflowImporter.run( workflow.getBpmn() );
		response.setReload(true);
		
	}

	/**
	 * Open all instances of the wkf.
	 *
	 * @param request
	 * @param response
	 */
	public void openInstances (ActionRequest request, ActionResponse response) {

		Workflow workflow = request.getContext().asType( Workflow.class );
		
		Map<String,Object> view = Maps.newHashMap();
		view.put( "title", workflow.getName() );
		view.put( "resource", Instance.class.getName() );
		view.put( "domain", String.format( "self.workflow.id = %d", workflow.getId() ) );
		
		response.setView( view );

	}

	/**
	 * Open editor
	 * 
	 * @param request
	 * @param response
	 */
	public void openEditor( ActionRequest request, ActionResponse response ) {

		Workflow workflow = request.getContext().asType( Workflow.class );
				
		String resource = String.format(
			"%s/p/editor?id=%s&name=%s&model=%s&url=%s&lang=%s&sessionId=%s}", 
			AppSettings.get().get(CONFIG_WKF_BASE, "http://localhost:8080/axelor-bpm"),
			workflow.getId(),
			workflow.getName(),
			workflow.getMetaModel().getFullName(),
			AppSettings.get().getBaseURL(),
			AppFilter.getLocale().getLanguage(),
			AuthUtils.getSubject().getSession().getId()
		);			
		
		log.debug("SIGNAVIO URL : {}", resource);

		Map<String,Object> view = Maps.newHashMap();
		view.put( "title", String.format("Editor : %s", workflow.getName()) );
		view.put( "resource", resource );
		view.put( "viewType", "html" );
		
		response.setView( view );
	}
}
