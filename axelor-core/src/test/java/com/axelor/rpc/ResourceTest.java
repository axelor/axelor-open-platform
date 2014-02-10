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
package com.axelor.rpc;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.joda.time.LocalDate;
import org.junit.Assert;
import org.junit.Test;

import com.axelor.db.JPA;
import com.axelor.test.db.Address;
import com.axelor.test.db.Circle;
import com.axelor.test.db.Contact;
import com.axelor.test.db.Title;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.inject.persist.Transactional;

public class ResourceTest extends RpcTest {

	@Inject
	Resource<Contact> resource;
	
	@Test
	public void testFields() throws Exception {

		Response res = resource.fields();
		
		Assert.assertNotNull(res);
		Assert.assertNotNull(res.getData());
		Assert.assertTrue(res.getData() instanceof Map);
	}

	@Test
	public void testSearch() throws Exception {

		Request req = fromJson("find3.js", Request.class);
		Response res = resource.search(req);
		
		Assert.assertNotNull(res);
		Assert.assertNotNull(res.getData());
		Assert.assertTrue(res.getData() instanceof List);
	}

	@Test @SuppressWarnings("all")
	@Transactional
	public void testAdd() throws Exception {

		Request req = fromJson("add2.js", Request.class);
		Response res = resource.save(req);
		
		Assert.assertNotNull(res);
		Assert.assertNotNull(res.getData());
		Assert.assertNotNull(res.getItem(0));
		Assert.assertTrue(res.getItem(0) instanceof Contact);

		Contact p = (Contact) res.getItem(0);

		Assert.assertEquals(Title.class, p.getTitle().getClass());
		Assert.assertEquals(Address.class, p.getAddresses().get(0).getClass());
		Assert.assertEquals(Circle.class, p.getCircle(0).getClass());
		Assert.assertEquals(LocalDate.class, p.getDateOfBirth().getClass());

		Assert.assertEquals("mr", p.getTitle().getCode());
		Assert.assertEquals("France", p.getAddresses().get(0).getCountry().getName());
		Assert.assertEquals("family", p.getCircle(0).getCode());
		Assert.assertEquals("1977-05-01", p.getDateOfBirth().toString());
	}
	
	@Test @SuppressWarnings("all")
	@Transactional
	public void testUpdate() throws Exception {

		Contact c = Contact.all().fetchOne();
		Map<String, Object> data = Maps.newHashMap();
		
		data.put("id", c.getId());
		data.put("version", c.getVersion());
		data.put("firstName", "jack");
		data.put("lastName", "sparrow");

		String json = toJson(ImmutableMap.of("data", data));

		Request req = fromJson(json, Request.class);
		Response res = resource.save(req);
		
		Assert.assertNotNull(res);
		Assert.assertNotNull(res.getData());
		Assert.assertNotNull(res.getItem(0));
		Assert.assertTrue(res.getItem(0) instanceof Contact);

		Contact contact = (Contact) res.getItem(0);
		
		Assert.assertEquals("jack", contact.getFirstName());
		Assert.assertEquals("sparrow", contact.getLastName());
	}
	

	@Test
	public void testCopy() {
		
		Contact c = Contact.all().filter("firstName = ?", "James").fetchOne();
		Contact n = JPA.copy(c, true);
		
		Assert.assertNotSame(c, n);
		Assert.assertNotSame(c.getAddresses(), n.getAddresses());
		Assert.assertEquals(c.getAddresses().size(),
							n.getAddresses().size());
		
		Assert.assertSame(c, c.getAddresses().get(0).getContact());
		Assert.assertSame(n, n.getAddresses().get(0).getContact());
		Assert.assertNotSame(c.getAddresses().get(0).getContact(), 
							 n.getAddresses().get(0).getContact());
	}
}
