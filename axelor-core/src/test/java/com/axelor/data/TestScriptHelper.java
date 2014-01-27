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
package com.axelor.data;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;

import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Test the Groovy dynamic script evaluation.
 *
 */
public class TestScriptHelper {

	private static final int MAX_COUNT = 1000;

	private ScriptHelper helper = new ScriptHelper(100, 1, false);
	
	@BeforeClass
	public static void doInit() {
		Binding binding = new Binding();
		binding.setVariable("count", 1000);
		new GroovyShell(binding).evaluate("x = count * 100");
	}
	
	@Test
	public void test_createShell() {
		int count = MAX_COUNT;
		while(--count > 0) {
			String expr = "count * " +  (count % 100);
			Binding binding = new Binding();
			binding.setVariable("count", count);
			new GroovyShell(binding).evaluate(expr);
		}
	}
	
	@Test
	public void test_useCache() {
		int count = MAX_COUNT;
		while(--count > 0) {
			String expr = "count * " + (count % 100);
			Binding binding = new Binding();
			binding.setVariable("count", count);
			helper.eval(expr, binding);
		}
	}
	
	@Test
	public void test_single_createShell() {
		int count = MAX_COUNT;
		while(--count > 0) {
			String expr = "count * 100";
			Binding binding = new Binding();
			binding.setVariable("count", count);
			new GroovyShell(binding).evaluate(expr);
		}
	}
	
	@Test
	public void test_single_useCache() {
		int count = MAX_COUNT;
		while(--count > 0) {
			String expr = "count * 100";
			Binding binding = new Binding();
			binding.setVariable("count", count);
			helper.eval(expr, binding);
		}
	}
}
