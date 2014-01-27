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
package com.axelor.meta.script;

/**
 * The ScriptHelper interface to implement dynamic script evaluation support.
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
