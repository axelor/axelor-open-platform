package com.axelor.wkf.action;

import javax.inject.Inject;

import com.axelor.meta.ActionHandler;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.Response;
import com.google.inject.Injector;

public class ActionWorkflow {

	@Inject
	private Injector injector;

	public Response execute( String action, ActionRequest request ) {
		
		request.setAction(action);
		ActionHandler handler = new ActionHandler( request, injector );
		return handler.execute();
		
	}

}
