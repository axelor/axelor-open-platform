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
package com.axelor.data.xml;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.axelor.data.ScriptHelper;
import com.google.common.base.Strings;
import com.google.common.collect.Sets;
import com.google.inject.Injector;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamImplicit;

@XStreamAlias("bind")
public class XMLBind {

	@XStreamAsAttribute
	private String node;
	
	@XStreamAlias("to")
	@XStreamAsAttribute
	private String field;
	
	@XStreamAsAttribute
	private String alias;
	
	@XStreamAlias("type")
	@XStreamAsAttribute
	private String typeName;

	@XStreamAsAttribute
	private String search;
	
	@XStreamAsAttribute
	private boolean update;
	
	@XStreamAlias("eval")
	@XStreamAsAttribute
	private String expression;
	
	@XStreamAlias("if")
	@XStreamAsAttribute
	private String condition;
	
	@XStreamAlias("call")
	@XStreamAsAttribute
	private String callable;
	
	@XStreamAsAttribute
	private String adapter;

	@XStreamImplicit(itemFieldName = "bind")
	private List<XMLBind> bindings;

	public String getNode() {
		return node;
	}
	
	public String getField() {
		return field;
	}
	
	public String getAlias() {
		return alias;
	}
	
	public String getAliasOrName() {
		if (alias == null || "".equals(alias.trim()))
			return node;
		return alias;
	}
	
	public String getTypeName() {
		return typeName;
	}
	
	public Class<?> getType() {
		try {
			return Class.forName(typeName);
		} catch (ClassNotFoundException e) {
		}
		return null;
	}

	public String getSearch() {
		return search;
	}
	
	public boolean isUpdate() {
		return update;
	}
	
	public String getExpression() {
		return expression;
	}
	
	public String getCondition() {
		return condition;
	}
	
	public String getCallable() {
		return callable;
	}
	
	public String getAdapter() {
		return adapter;
	}
	
	public List<XMLBind> getBindings() {
		return bindings;
	}
	
	private Set<String> multiples;
	
	public boolean isMultiple(XMLBind bind) {
		if (multiples == null) {
			multiples = Sets.newHashSet();
			Set<String> found = Sets.newHashSet();
			for (XMLBind b : bindings) {
				if (found.contains(b.getNode())) {
					multiples.add(b.getNode());
				}
				found.add(b.getNode());
			}
		}
		return multiples.contains(bind.getNode());
	}
	
	private Object callObject;
	private Method callMethod;
	
	@SuppressWarnings("unchecked")
	public <T> T call(T object, Map<String, Object> context, Injector injector) throws Exception {
		
		if (Strings.isNullOrEmpty(callable))
			return object;
		
		if (callObject == null) {
			
			String className = callable.split("\\:")[0];
			String method = callable.split("\\:")[1];
			
			Class<?> klass = Class.forName(className);
			
			callMethod = klass.getMethod(method, Object.class, Map.class);
			callObject = injector.getInstance(klass);
		}
		
		try {
			return (T) callMethod.invoke(callObject, new Object[]{ object, context });
		} catch (Exception e) {
			System.err.println("EEE: " + e);
		}
		return object;
	}

	private static ScriptHelper helper = new ScriptHelper(100, 10, false);
	
	public Object eval(Map<String, Object> context) {
		if (Strings.isNullOrEmpty(expression)) {
			return context.get(this.getAliasOrName());
		}
		return helper.eval(expression, context);
	}
	
	public boolean validate(Map<String, Object> context) {
		if (Strings.isNullOrEmpty(condition)) {
			return true;
		}
		String expr = condition + " ? true : false";
		return (Boolean) helper.eval(expr, context);
	}

	@Override
	public String toString() {
		
		StringBuilder sb = new StringBuilder("<bind");

		if (node != null)
			sb.append(" node='").append(node).append("'");
		if (field != null)
			sb.append(" to=='").append(field).append("'");
		if (typeName != null)
			sb.append(" type='").append(typeName).append("'");
		if (alias != null)
			sb.append(" alias='").append(alias).append("'");

		return sb.append(" ... >").toString();
	}
}
