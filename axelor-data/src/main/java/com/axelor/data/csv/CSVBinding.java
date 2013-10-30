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
package com.axelor.data.csv;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.codehaus.groovy.runtime.InvokerHelper;

import com.axelor.data.ScriptHelper;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.google.common.base.Objects.ToStringHelper;
import com.google.common.base.Strings;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.google.inject.Injector;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamImplicit;

@XStreamAlias("bind")
public class CSVBinding {

	public static Pattern pattern = Pattern.compile("^(call|eval):\\s*(.*)");

	@XStreamAsAttribute
	private String column;

	@XStreamAlias("to")
	@XStreamAsAttribute
	private String field;

	@XStreamAsAttribute
	private String type;

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

	@XStreamAlias("if-empty")
	@XStreamAsAttribute
	private Boolean conditionEmpty;

	@XStreamImplicit(itemFieldName = "bind")
	private List<CSVBinding> bindings;

	@XStreamAsAttribute
	private String adapter;

	public String getColumn() {
		return column;
	}

	public void setColumn(String column) {
		this.column = column;
	}

	public String getField() {
		return field;
	}

	public void setField(String field) {
		this.field = field;
	}

	public String getType() {
		return type;
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

	public void setExpression(String expression) {
		this.expression = expression;
	}

	public String getCondition() {
		return condition;
	}

	public void setCondition(String condition) {
		this.condition = condition;
	}

	public Boolean getConditionEmpty() {
		return conditionEmpty;
	}

	public void setConditionEmpty(Boolean conditionEmpty) {
		this.conditionEmpty = conditionEmpty;
	}

	public List<CSVBinding> getBindings() {
		return bindings;
	}

	public String getAdapter() {
		return adapter;
	}

	public static CSVBinding getBinding(final String column, final String field, Set<String> cols) {
		CSVBinding cb = new CSVBinding();
		cb.field = field;
		cb.column = column;

		if (cols == null || cols.isEmpty()) {
			if (cb.column == null)
				cb.column = field;
			return cb;
		}

		for(String col : cols) {
			if (cb.bindings == null)
				cb.bindings = Lists.newArrayList();
			cb.bindings.add(CSVBinding.getBinding(field + "." + col, col, null));
		}

		cb.update = true;
		cb.search = Joiner.on(" AND ").join(Collections2.transform(cols, new Function<String, String>(){

			@Override
			public String apply(String input) {
				return String.format("self.%s = :%s_%s_", input, field, input);
			}
		}));

		return cb;
	}

	private static ScriptHelper helper = new ScriptHelper(100, 10, false);

	public Object evaluate(Map<String, Object> context, Injector injector) {
		if (Strings.isNullOrEmpty(expression)) {
			return handleGroovy(context);
		}

		String kind = null;
		String expr = expression;
		Matcher matcher = pattern.matcher(expression);

		if (matcher.matches()) {
			kind = matcher.group(1);
			expr = matcher.group(2);
		} else {
			return handleGroovy(context);
		}

		if ("call".equals(kind)) {
			return handleCall(context, expr, injector);
		}

		return handleGroovy(context);
	}

	private Object handleGroovy(Map<String, Object> context) {
		if (Strings.isNullOrEmpty(expression)) {
			return context.get(column);
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

	private Object handleCall(Map<String, Object> context, String expr, Injector injector) {
		if(Strings.isNullOrEmpty(expr)) {
			return null;
		}

		try {

			String className = expr.split("\\:")[0];
			String method = expr.split("\\:")[1];

			Class<?> klass = Class.forName(className);
			Object object = injector.getInstance(klass);

			Pattern p = Pattern.compile("(\\w+)\\((.*?)\\)");
			Matcher m = p.matcher(method);

			if (!m.matches()) return null;

			String methodName = m.group(1);
			String params = "[" + m.group(2) + "] as Object[]";
			Object[] arguments = (Object[]) helper.eval(params, context);

			return InvokerHelper.invokeMethod(object, methodName, arguments);
		}
		catch(Exception e) {
			System.err.println("EEE: " + e);
			return null;
		}
	}

	@Override
	public String toString() {

		ToStringHelper ts = Objects.toStringHelper(this);

		if (column != null) ts.add("column", column);
		if (field != null) ts.add("field", field);
		if (type != null) ts.add("type", type);
		if (bindings != null) ts.add("bindings", bindings).toString();

		return ts.toString();
	}
}
