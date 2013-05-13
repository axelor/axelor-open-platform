package com.axelor.wkf.action;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.axelor.meta.ActionHandler;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.google.inject.Injector;

public class Action {
	
	protected static final Logger LOG = LoggerFactory.getLogger(Action.class);

	@Inject
	private Injector injector;

	public ActionResponse execute( String action, ActionRequest request ) {

		LOG.debug("Execute action : {}", action);
		request.setAction( action );
		ActionHandler handler = new ActionHandler( request, injector );
		
		ActionResponse response = handler.execute();
		LOG.debug("Response : {}", response.getData());
		
		return response;
		
	}

}
