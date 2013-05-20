package com.axelor.meta.views;

import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import com.axelor.meta.ActionHandler;
import com.google.common.collect.Maps;

@XmlType
public class ActionWorkflow extends Action {
	
	public static final String className = "com.axelor.wkf.workflow.WorkflowService",
			method = "run";

	@XmlElement(name = "workflow")
	private List<WorkflowItem> workflows;
	
	public List<WorkflowItem> getWorkflows() {
		return workflows;
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public Object evaluate(ActionHandler handler) {

		Map<String, String> result = Maps.newHashMap();
		Iterator<WorkflowItem> iter = workflows.iterator();
		
		if (getName() != null) {
			log.debug("action-workflow: {}", getName());
		}

		try {
			
			Class<?> klass = Class.forName( className );
			Method m = klass.getMethod( method, String.class, ActionHandler.class );
			Object obj = handler.getInjector().getInstance(klass);
			
			while(iter.hasNext()) {
				Act act = iter.next();
				if (act.test(handler)) {
					String name = act.getName().trim();
					result.putAll( (Map) m.invoke(obj, name, handler) );
				}
			}
			
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
	
	public static class WorkflowItem extends Act {
		
	}

}
