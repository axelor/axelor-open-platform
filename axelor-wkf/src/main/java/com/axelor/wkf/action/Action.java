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
