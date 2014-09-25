/**
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2014 Axelor (<http://axelor.com>).
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
package com.axelor.meta.schema.actions;

import java.lang.reflect.Method;
import java.util.Map;

import javax.xml.bind.annotation.XmlType;

import com.axelor.meta.ActionHandler;
import com.google.common.collect.Maps;

@XmlType
public class ActionWorkflow extends Action {
	
	public static final String className = "com.axelor.wkf.service.WorkflowService",
			method = "run";
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public Object evaluate(ActionHandler handler) {

		Map<String, String> result = Maps.newHashMap();
		
		if (getName() != null) {
			log.debug("action-workflow: {}", getName());
		}

		try {
			
			Class<?> klass = Class.forName( className );
			Method m = klass.getMethod( method, String.class, ActionHandler.class );
			Object obj = handler.getInjector().getInstance(klass);
			result.putAll( (Map) m.invoke(obj, getModel().trim(), handler) );
			
		} catch (Exception e) { 
			log.error( "{}", e);
		}

		log.debug("Result : {}", result);
		return result;
	}
	
	@Override
	public Object wrap(ActionHandler handler) {
		return evaluate(handler);
	}

}
