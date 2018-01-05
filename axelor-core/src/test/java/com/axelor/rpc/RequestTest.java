/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2017 Axelor (<http://axelor.com>).
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
package com.axelor.rpc;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.junit.Assert;
import org.junit.Test;

import com.axelor.db.Query;
import com.axelor.test.db.Address;
import com.axelor.test.db.Circle;
import com.axelor.test.db.Contact;
import com.axelor.test.db.Title;
import com.axelor.test.db.repo.ContactRepository;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.inject.persist.Transactional;

public class RequestTest extends RpcTest {

	@Inject
	private ContactRepository contacts;

	@Test
	public void testObjects() {

		Request req = fromJson("find.json", Request.class);

		Assert.assertTrue(req.getData() instanceof Map);
		Assert.assertTrue(req.getData().get("criteria") instanceof List);

		for (Object o : (List<?>) req.getData().get("criteria")) {
			Assert.assertTrue(o instanceof Map);
		}
	}

	@Test
	public void testFind() {

		Request req = fromJson("find.json", Request.class);
		
		Criteria c = Criteria.parse(req);
		Query<Contact> q = c.createQuery(Contact.class);

		String actual = q.toString();
		Assert.assertNotNull(actual);
	}

	@Test
	public void testFind2() {

		Request req = fromJson("find2.json", Request.class);

		Criteria c = Criteria.parse(req);
		Query<Contact> q = c.createQuery(Contact.class);
		
		String actual = q.toString();

		Assert.assertEquals("SELECT self FROM Contact self WHERE (self.archived is null OR self.archived = false)", actual);
	}

	@Test
	public void testFind3() {

		Request req = fromJson("find3.json", Request.class);

		Criteria c = Criteria.parse(req);
		Query<Contact> q = c.createQuery(Contact.class);
		
		Assert.assertTrue(q.count() > 0);
	}

	@Test
	public void testAdd() {

		Request req = fromJson("add.json", Request.class);

		Assert.assertTrue(req.getData() instanceof Map);

		Map<String, Object> data = req.getData();

		Assert.assertEquals("some", data.get("firstName"));
		Assert.assertEquals("thing", data.get("lastName"));
		Assert.assertEquals("some@thing.com", data.get("email"));

	}

	@Test @Transactional
	public void testAdd2() {

		Request req = fromJson("add2.json", Request.class);

		Assert.assertTrue(req.getData() instanceof Map);

		Map<String, Object> data = req.getData();

		Assert.assertEquals("Jack", data.get("firstName"));
		Assert.assertEquals("Sparrow", data.get("lastName"));
		Assert.assertEquals("jack.sparrow@gmail.com", data.get("email"));

		Contact p = contacts.edit(data);

		Assert.assertEquals(Title.class, p.getTitle().getClass());
		Assert.assertEquals(Address.class, p.getAddresses().get(0).getClass());
		Assert.assertEquals(Circle.class, p.getCircle(0).getClass());
		Assert.assertEquals(LocalDate.class, p.getDateOfBirth().getClass());

		Assert.assertEquals("mr", p.getTitle().getCode());
		Assert.assertEquals("France", p.getAddresses().get(0).getCountry().getName());
		Assert.assertEquals("family", p.getCircle(0).getCode());
		Assert.assertEquals("1977-05-01", p.getDateOfBirth().toString());
		
		contacts.manage(p);
	}
	
	@Test
	@Transactional
	public void testUpdate() {
		
		Contact c = contacts.all().fetchOne();
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

		Contact o = contacts.edit(data);
		
		o = contacts.manage(o);
	}
}
