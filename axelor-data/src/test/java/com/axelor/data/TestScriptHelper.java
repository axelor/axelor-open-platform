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
