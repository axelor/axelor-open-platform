/**
 * Axelor Business Solutions
 *
 * Copyright (C) 2012-2014 Axelor (<http://axelor.com>).
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

import groovy.util.slurpersupport.GPathResult;
import groovy.xml.XmlUtil;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import wslite.soap.SOAPClient;
import wslite.soap.SOAPResponse;

import com.axelor.meta.ActionHandler;
import com.axelor.meta.MetaStore;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

@XmlType
public class ActionWS extends Action {

	private static final int DEFAULT_READ_TIMEOUT = 300;
	private static final int DEFAULT_CONNECT_TIMEOUT = 60;

	@XmlAttribute
	private String service;

	@XmlAttribute(name = "connect-timeout")
	private Integer connectTimeout;

	@XmlAttribute(name = "read-timeout")
	private Integer readTimeout;

	@XmlElement(name = "action")
	private List<Method> methods;

	public String getService() {
		return service;
	}

	public Integer getConnectTimeout() {
		if (connectTimeout == null) {
			return DEFAULT_CONNECT_TIMEOUT;
		}
		return connectTimeout;
	}

	public Integer getReadTimeout() {
		if (readTimeout == null) {
			return DEFAULT_READ_TIMEOUT;
		}
		return readTimeout;
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

	private Object send(String location, String template, ActionHandler handler)
			throws IOException, FileNotFoundException, ClassNotFoundException {

		File templateFile = new File(template);
		if (!templateFile.isFile()) {
			throw new IllegalArgumentException("No such template: " + template);
		}

		String payload = handler.template(templateFile);
		Map<String, Object> params = Maps.newHashMap();

		params.put("connectTimeout", getConnectTimeout() * 1000);
		params.put("readTimeout", getReadTimeout() * 1000);

		SOAPClient client = new SOAPClient(location);
		SOAPResponse response = client.send(params, payload);

		GPathResult gpath = ((GPathResult) response.getBody()).children();
		String ns = gpath.lookupNamespace("");
		if (ns != null) {
			gpath.declareNamespace(ImmutableMap.of(":", ns));
		}

		return XmlUtil.serialize(gpath);
	}

	private String getService(ActionWS ref, ActionHandler handler) {
		String url = ref == null ? service : ref.getService();
		Object service = handler.evaluate(url);

		if(service == null) {
			log.error("No such service: " + url);
			return null;
		}

		return service.toString();
	}

	@Override
	public Object evaluate(ActionHandler handler) {

		ActionWS ref = getRef();
		String url = getService(ref, handler);

		if (Strings.isNullOrEmpty(url))
			return null;

		if (ref != null) {
			ref.evaluate(handler);
		}

		List<Object> result = Lists.newArrayList();
		log.info("action-ws (name): " + getName());
		for(Method m : methods) {
			Object template = handler.evaluate(m.template);
			if(template == null) {
				log.error("No such template: " + m.template);
				continue;
			}
			log.info("action-ws (method, template): " + m.getName() + ", " + template.toString());
			try {
				Object res = this.send(url, template.toString(), handler);
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
