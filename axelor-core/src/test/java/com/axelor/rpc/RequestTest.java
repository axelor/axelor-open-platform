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

import org.joda.time.LocalDate;
import org.junit.Assert;
import org.junit.Test;

import com.axelor.db.JPA;
import com.axelor.db.Query;
import com.axelor.test.db.Address;
import com.axelor.test.db.Circle;
import com.axelor.test.db.Contact;
import com.axelor.test.db.Title;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.inject.persist.Transactional;

public class RequestTest extends RpcTest {

	@Test
	public void testObjects() {

		Request req = fromJson("find.js", Request.class);

		Assert.assertTrue(req.getData() instanceof Map);
		Assert.assertTrue(req.getData().get("criteria") instanceof List);

		for (Object o : (List<?>) req.getData().get("criteria")) {
			Assert.assertTrue(o instanceof Map);
		}
	}

	@Test
	public void testFind() {

		Request req = fromJson("find.js", Request.class);
		
		Criteria c = Criteria.parse(req);
		Query<Contact> q = c.createQuery(Contact.class);

		String actual = q.toString();
		Assert.assertNotNull(actual);
	}

	@Test
	public void testFind2() {

		Request req = fromJson("find2.js", Request.class);

		Criteria c = Criteria.parse(req);
		Query<Contact> q = c.createQuery(Contact.class);
		
		String actual = q.toString();

		Assert.assertEquals("SELECT self FROM Contact self WHERE (self.archived is null OR self.archived = false)", actual);
	}

	@Test
	public void testFind3() {

		Request req = fromJson("find3.js", Request.class);

		Criteria c = Criteria.parse(req);
		Query<Contact> q = c.createQuery(Contact.class);
		
		Assert.assertTrue(q.count() > 0);
	}

	@Test
	public void testAdd() {

		Request req = fromJson("add.js", Request.class);

		Assert.assertTrue(req.getData() instanceof Map);

		Map<String, Object> data = req.getData();

		Assert.assertEquals("some", data.get("firstName"));
		Assert.assertEquals("thing", data.get("lastName"));
		Assert.assertEquals("some@thing.com", data.get("email"));

	}

	@Test @Transactional
	public void testAdd2() {

		Request req = fromJson("add2.js", Request.class);

		Assert.assertTrue(req.getData() instanceof Map);

		Map<String, Object> data = req.getData();

		Assert.assertEquals("Jack", data.get("firstName"));
		Assert.assertEquals("Sparrow", data.get("lastName"));
		Assert.assertEquals("jack.sparrow@gmail.com", data.get("email"));

		Contact p = Contact.edit(data);

		Assert.assertEquals(Title.class, p.getTitle().getClass());
		Assert.assertEquals(Address.class, p.getAddresses().get(0).getClass());
		Assert.assertEquals(Circle.class, p.getCircle(0).getClass());
		Assert.assertEquals(LocalDate.class, p.getDateOfBirth().getClass());

		Assert.assertEquals("mr", p.getTitle().getCode());
		Assert.assertEquals("France", p.getAddresses().get(0).getCountry().getName());
		Assert.assertEquals("family", p.getCircle(0).getCode());
		Assert.assertEquals("1977-05-01", p.getDateOfBirth().toString());
		
		JPA.manage(p);
	}
	
	@Test
	@Transactional
	public void testUpdate() {
		
		Contact c = Contact.all().fetchOne();
		Map<String, Object> data = Maps.newHashMap();

		data.put("id", c.getId());
		data.put("version", c.getVersion());
		data.put("firstName", "Some");
		data.put("lastName", "thing");

		String json = toJson(ImmutableMap.of("data", data));
		Request req = fromJson(json, Request.class);

		Assert.assertTrue(req.getData() instanceof Map);

		data = req.getData();

		Assert.assertEquals("Some", data.get("firstName"));
		Assert.assertEquals("thing", data.get("lastName"));

		Contact o = Contact.edit(data);
		
		o = JPA.manage(o);
	}
}
