/**
 * Copyright (c) 2012-2014 Axelor. All Rights Reserved.
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
 * Copyright (c) 2012-2014 Axelor. All Rights Reserved.
 */
package com.axelor.meta.schema.actions;

import java.util.regex.Pattern;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.axelor.meta.ActionHandler;
import com.google.common.base.Objects;
import com.google.common.base.Strings;

@XmlType(name = "AbstractAction")
public abstract class Action {
	
	protected final transient Logger log = LoggerFactory.getLogger(getClass());
	
	@XmlAttribute
	private String name;
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public abstract Object wrap(ActionHandler handler);
	
	public abstract Object evaluate(ActionHandler handler);
	
	@Override
	public String toString() {
		return Objects.toStringHelper(getClass()).add("name", getName()).toString();
	}
	
	@XmlType
	public static abstract class Element {
		
		@XmlAttribute(name = "if")
		private String condition;

		@XmlAttribute
		private String name;
		
		@XmlAttribute(name = "expr")
		private String expression;
		
		public String getCondition() {
			return condition;
		}
		
		public void setCondition(String condition) {
			this.condition = condition;
		}
		
		public String getName() {
			return name;
		}
		
		public void setName(String name) {
			this.name = name;
		}
		
		public String getExpression() {
			return expression;
		}
		
		public void setExpression(String expression) {
			this.expression = expression;
		}
		
		boolean test(ActionHandler handler, String expression) {
			if (Strings.isNullOrEmpty(expression)) // if expression is not given always return true
				return true;
			if (expression.matches("true"))
				return true;
			if (expression.equals("false"))
				return false;
			Pattern pattern = Pattern.compile("^(eval|select|action):");
			if (expression != null && !pattern.matcher(expression).find()) {
				expression = "eval:" + expression;
			}
			Object result = handler.evaluate(expression);
			if (result == null)
				return false;
			if (result instanceof Number && result.equals(0))
				return false;
			if (result instanceof Boolean)
				return (Boolean) result;
			return true;
		}
		
		boolean test(ActionHandler handler) {
			return test(handler, getCondition());
		}
	}
}
