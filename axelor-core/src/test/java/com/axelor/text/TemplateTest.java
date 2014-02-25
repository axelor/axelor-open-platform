/**
 * Copyright (c) 2012-2014 Axelor. All Rights Reserved.
 *
 * The contents of this file are subject to the Common Public
 * Attribution License Version 1.0 (the "License"); you may not use
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
 * Software distributed under the License is distributed on an "AS IS"
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
