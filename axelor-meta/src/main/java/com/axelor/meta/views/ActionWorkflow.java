package com.axelor.meta.views;

import java.lang.reflect.Method;
import java.util.Map;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;

import com.axelor.meta.ActionHandler;
import com.google.common.collect.Maps;

@XmlType
public class ActionWorkflow extends Action {
	
	public static final String className = "com.axelor.wkf.workflow.WorkflowService",
			method = "run";
	
	@XmlAttribute
	private String model;
	
	public String getModel() {
		return model;
	}
	
	public void setModel(String model) {
		this.model = model;
	}
	
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
			result.putAll( (Map) m.invoke(obj, model.trim(), handler) );
			
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
