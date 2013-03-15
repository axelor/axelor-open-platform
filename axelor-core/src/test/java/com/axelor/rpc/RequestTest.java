package com.axelor.rpc;

import java.util.List;
import java.util.Map;

import org.joda.time.LocalDate;
import org.junit.Assert;
import org.junit.Test;

import com.axelor.BaseTest;
import com.axelor.db.JPA;
import com.axelor.db.Query;
import com.axelor.test.db.Address;
import com.axelor.test.db.Contact;
import com.axelor.test.db.Group;
import com.axelor.test.db.Title;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.inject.persist.Transactional;

public class RequestTest extends BaseTest {

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

		Assert.assertEquals("SELECT self FROM Contact self", actual);
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
		Assert.assertEquals(Group.class, p.getGroup(0).getClass());
		Assert.assertEquals(LocalDate.class, p.getDateOfBirth().getClass());

		Assert.assertEquals("mr", p.getTitle().getCode());
		Assert.assertEquals("France", p.getAddresses().get(0).getCountry().getName());
		Assert.assertEquals("family", p.getGroup(0).getName());
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
