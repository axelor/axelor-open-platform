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
package com.axelor.meta.loader;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.google.common.collect.Lists;

public class TestResolver {

	private Resolver resolver = new Resolver();

	@Test
	public void test() {
		
		resolver.add("axelor-auth", 	"axelor-core");
		resolver.add("axelor-wkf", 		"axelor-auth", "axelor-core");
		resolver.add("axelor-meta", 	"axelor-data");
		
		resolver.add("axelor-x");
		
		resolver.add("axelor-sale", 	"axelor-contact" );
		resolver.add("axelor-data", 	"axelor-auth", "axelor-core");
		resolver.add("axelor-contact", 	"axelor-auth", "axelor-core","axelor-meta");
		resolver.add("axelor-project", 	"axelor-sale");

		List<String> expected = Lists.newArrayList(
				"axelor-core",
				"axelor-auth",
				"axelor-data",
				"axelor-meta",
				"axelor-contact",
				"axelor-sale");

		Assert.assertEquals(expected, resolver.resolve("axelor-sale"));

		List<String> all = resolver.names();
		
		Assert.assertEquals("axelor-core", all.get(0));
		Assert.assertEquals("axelor-project", all.get(all.size() - 1));
		
	}
}
