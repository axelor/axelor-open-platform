package com.axelor.meta.schema.actions;

import groovy.util.slurpersupport.GPathResult;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import wslite.soap.SOAPClient;
import wslite.soap.SOAPResponse;

import com.axelor.meta.ActionHandler;
import com.axelor.meta.MetaStore;
import com.axelor.meta.TemplateHelper;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;

@XmlType
public class ActionWS extends Action {

	@XmlAttribute
	private String service;
	
	@XmlElement(name = "action")
	private List<Method> methods;
	
	public String getService() {
		return service;
	}
	
	public List<Method> getMethods() {
		return methods;
	}
	
	private ActionWS getRef() {
		if (service == null || !service.startsWith("ref:"))
			return null;
		String refName = service.replaceFirst("^ref:", "");
		Action ref = MetaStore.getAction(refName);
		if (ref == null || !(ref instanceof ActionWS))
			throw new IllegalArgumentException("No such web service: " + refName);
		if (((ActionWS) ref).getService().startsWith("ref:"))
			throw new IllegalArgumentException("Invalid web service: " + refName);
		return (ActionWS) ref;
	}
	
	private Object send(String location, Method method, ActionHandler handler)
			throws IOException, FileNotFoundException, ClassNotFoundException {
		
		File template = new File(method.template);
		if (!template.isFile()) {
			throw new IllegalArgumentException("No such template: " + method.template);
		}
		
		String payload = handler.template(template);
		SOAPClient client = new SOAPClient(location);
		SOAPResponse response = client.send(payload);
		
		GPathResult gpath = (GPathResult) response.getBody();
		gpath = gpath.children();

		return TemplateHelper.serialize(gpath);
	}

	@Override
	public Object evaluate(ActionHandler handler) {

		ActionWS ref = getRef();
		String url = ref == null ? service : ref.getService();
		
		if (Strings.isNullOrEmpty(url))
			return null;
		
		if (ref != null) {
			ref.evaluate(handler);
		}
		
		List<Object> result = Lists.newArrayList();
		log.info("action-ws (name): " + getName());
		for(Method m : methods) {
			log.info("action-ws (method, template): " + m.getName() + ", " + m.template);
			try {
				Object res = this.send(url, m, handler);
				result.add(res);
			} catch (Exception e) {
				log.error("error: " + e);
			}
		}
		return result;
	}
	
	@Override
	public Object wrap(ActionHandler handler) {
		return evaluate(handler);
	}
	
	@XmlType
	public static class Method extends Element {
		
		@XmlAttribute
		private String template;
		
		public String getTemplate() {
			return template;
		}
	}
}
