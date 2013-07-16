package com.axelor.wkf.web

import javax.inject.Inject

import com.axelor.rpc.ActionRequest
import com.axelor.rpc.ActionResponse
import com.axelor.wkf.db.Instance
import com.axelor.wkf.helper.DiagramHelper

class InstanceController {

	@Inject
	protected DiagramHelper diagramHelper;
	
	def loadDiagram(ActionRequest request, ActionResponse response){
		
		response.values = [
			"diagram" : diagramHelper.getDiagram( request.context as Instance )
		]
		
	}
	
}
