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
package com.axelor.script;

/**
 * The DataScriptHelper interface to implement dynamic script evaluation support.
 *
 */
public interface ScriptHelper {

	/**
	 * Get current script bindings.
	 *
	 * @return bindings
	 */
	ScriptBindings getBindings();

	/**
	 * Set script bindings.
	 *
	 * @param bindings
	 *            new script bindings.
	 */
	void setBindings(ScriptBindings bindings);

	/**
	 * Evaluate the given expression.
	 *
	 * @param expr
	 *            the expression to evaluate
	 * @return expression result
	 */
	Object eval(String expr);

	/**
	 * Evaluate a boolean expression.
	 *
	 * @param expr
	 *            a boolean expression
	 * @return true or false
	 */
	boolean test(String expr);

	/**
	 * Call a method on the given object with the provided arguments.
	 *
	 * @param obj
	 *            the object on which method should be called
	 * @param method
	 *            the name of the method
	 * @param args
	 *            method arguments
	 *
	 * @return return value of the method
	 */
	Object call(Object obj, String method, Object... args);

	/**
	 * Call a method on the given object.
	 *
	 * The methodCall is a string expression containing arguments to be passed.
	 * For example:
	 *
	 * <pre>
	 * scriptHelper.call(bean, &quot;test(var1, var2, var3)&quot;);
	 * </pre>
	 *
	 * This is a convenient method to call:
	 *
	 * <pre>
	 * scriptHelper.call(bean, &quot;test&quot;, new Object[] { var1, var2, var3 });
	 * </pre>
	 *
	 * @param obj
	 *            the object on which method should be called
	 * @param methodCall
	 *            method call expression
	 *
	 * @return return value of the method
	 */
	Object call(Object obj, String methodCall);
}
