package com.axelor.wkf.action;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.axelor.meta.ActionHandler;
import com.google.common.collect.Maps;

public class Action {
	
	private Logger log = LoggerFactory.getLogger( getClass() );
	
	private Map<String, String> data = Maps.newHashMap();

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void execute( String action, ActionHandler actionHandler ) {

		log.debug("Execute action : {}", action);
		actionHandler.getRequest().setAction(action);
		
		for ( Object data2 : (List) actionHandler.execute().getData()) {
			
			data.putAll( (Map) data2 );
			
		}

		log.debug( "Response : {}", data );
		
	}

	public boolean isInError( ){
		
		if ( data.containsKey("errors") ) {
			data.remove("errors");
			return true; 
		}
		
		return false;
	}
	
	public Map<String, String> getData (){
		return this.data;
	}

}
