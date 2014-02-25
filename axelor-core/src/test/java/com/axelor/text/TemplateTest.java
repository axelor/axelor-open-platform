package com.axelor.text;

import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.axelor.AbstractTest;
import com.google.common.collect.Maps;

public class TemplateTest extends AbstractTest {

	private static final String GROOVY_TEMPLATE = "Hello: ${firstName} ${lastName} = ${nested.message}";

	private static final String STRING_TEMPLATE = "Hello: <firstName> <lastName> = <nested.message>";

	private static final String OUTPUT = "Hello: John Smith = Hello World!!!";

	private Map<String, Object> vars;

	@Before
	public void setUp() {
		vars = Maps.newHashMap();
		vars.put("message", "Hello World!!!");
		
		vars.put("firstName", "John");
		vars.put("lastName", "Smith");
		
		vars.put("nested", Maps.newHashMap(vars));
	}
	
	@Test
	public void testGroovyTemplate() {

		Templates templates = new GroovyTemplates();
		Template template = templates.fromText(GROOVY_TEMPLATE);

		String text = template.make(vars).render();
		Assert.assertEquals(OUTPUT, text);
	}
	
	@Test
	public void testStringTemplate() {

		Templates templates = new StringTemplates();
		Template template = templates.fromText(STRING_TEMPLATE);

		String text = template.make(vars).render();
		Assert.assertEquals(OUTPUT, text);
	}
}
