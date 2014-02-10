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
package com.axelor.db;

import org.junit.Assert;
import org.junit.Test;

import com.axelor.MyTest;
import com.axelor.db.mapper.Mapper;
import com.axelor.db.mapper.Property;
import com.axelor.db.mapper.PropertyType;
import com.axelor.test.db.Address;
import com.axelor.test.db.Contact;

public class PropertyTest extends MyTest {

	private Mapper mapper = Mapper.of(Contact.class);
	
	@Test
	public void test() {
		
		Property p = mapper.getProperty("firstName");
		Assert.assertEquals(Contact.class, p.getEntity());
		Assert.assertEquals("firstName", p.getName());
		Assert.assertEquals(PropertyType.STRING, p.getType());
		
		p = mapper.getProperty("addresses");
		Assert.assertEquals("addresses", p.getName());
		Assert.assertEquals(PropertyType.ONE_TO_MANY, p.getType());
		Assert.assertEquals("contact", p.getMappedBy());
		Assert.assertEquals(Address.class, p.getTarget());
		
		Assert.assertTrue(mapper.getProperties().length > 0);
		
		// virtual column
		p = mapper.getProperty("fullName");
		Assert.assertEquals(Contact.class, p.getEntity());
		Assert.assertEquals("fullName", p.getName());
		Assert.assertEquals(PropertyType.STRING, p.getType());
		
		// binary column
		p = mapper.getProperty("image");
		Assert.assertEquals(Contact.class, p.getEntity());
		Assert.assertEquals("image", p.getName());
		Assert.assertEquals(PropertyType.BINARY, p.getType());
		
		// multiline text
		p = mapper.getProperty("notes");
		Assert.assertEquals(Contact.class, p.getEntity());
		Assert.assertEquals("notes", p.getName());
		Assert.assertEquals(PropertyType.TEXT, p.getType());
	}
}
