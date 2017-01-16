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
package com.axelor.data.xml;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.axelor.data.ImportException;
import com.axelor.data.DataScriptHelper;
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
	private Boolean update;
	
	@XStreamAsAttribute
	private Boolean create;

	@XStreamAlias("eval")
	@XStreamAsAttribute
	private String expression;

	@XStreamAlias("if")
	@XStreamAsAttribute
	private String condition;

	@XStreamAlias("if-empty")
	@XStreamAsAttribute
	private Boolean conditionEmpty;

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

	public Boolean getUpdate() {
		return update;
	}
	
	public Boolean getCreate() {
		return create;
	}

	public String getExpression() {
		return expression;
	}

	public String getCondition() {
		return condition;
	}

	public Boolean getConditionEmpty() {
		return conditionEmpty;
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
			throw new ImportException(e);
		}
	}

	private static DataScriptHelper helper = new DataScriptHelper(100, 10, false);

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
