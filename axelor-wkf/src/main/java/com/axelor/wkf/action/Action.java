/**
 * Copyright (c) 2012-2013 Axelor. All Rights Reserved.
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
 * Copyright (c) 2012-2013 Axelor. All Rights Reserved.
 */
package com.axelor.wkf.action;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.axelor.meta.ActionHandler;
import com.axelor.meta.db.MetaAction;
import com.google.common.collect.Maps;

public class Action {
	
	private Logger log = LoggerFactory.getLogger( getClass() );
	
	private Map<Object, Object> data = Maps.newHashMap();

	@SuppressWarnings({ "rawtypes" })
	public void execute( MetaAction action, ActionHandler actionHandler ) {

		log.debug("Execute action : {}", action);
		actionHandler.getRequest().setAction( action.getName() );
		
		for ( Object data : (List) actionHandler.execute().getData()) { updateData( this.data, data ); }

		log.debug( "Response : {}", data );
		
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void updateData( Map<Object, Object> data, Object values ){
		
		log.debug("Update data : {}", values);
		
		Map data2 = (Map) values;
		
		for ( Object key : data2.keySet()) {

			if ( !data.containsKey(key) ) { data.put(key, data2.get(key)); }
			else {
				if ( data.get(key) instanceof Map ) { updateData( (Map) data.get(key), data2.get(key) ); }
				else { data.put(key, data2.get(key)); }
			}
		}
		
	}

	@SuppressWarnings({ "rawtypes" })
	public boolean isInError( ){
		
		if ( data.containsKey("errors") && data.get("errors") != null && !( (Map) data.get("errors") ).isEmpty() ) {
			data.remove("errors");
			return true; 
		}

		return false;
	}
	
	public Map<Object, Object> getData (){
		return this.data;
	}

}
