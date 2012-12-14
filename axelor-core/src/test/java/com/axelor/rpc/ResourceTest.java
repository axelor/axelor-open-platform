package com.axelor.rpc;

import java.sql.Date;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.junit.Assert;
import org.junit.Test;

import com.axelor.BaseTest;
import com.axelor.db.JPA;
import com.axelor.test.db.Address;
import com.axelor.test.db.Contact;
import com.axelor.test.db.Group;
import com.axelor.test.db.Title;

public class ResourceTest extends BaseTest {

	@Inject
	Resource<Contact> resource;
	
	@Test
	public void testFields() throws Exception {

		Response res = resource.fields();
		
		Assert.assertNotNull(res);
		Assert.assertNotNull(res.getData());
		Assert.assertTrue(res.getData() instanceof List);
		
//		for(Object field : (List<?>) res.getData())
//			System.err.println(toJson(field));
	}

	@Test
	public void testSearch() throws Exception {

		Request req = fromJson("find3.js", Request.class);
		Response res = resource.search(req);
		
		Assert.assertNotNull(res);
		Assert.assertNotNull(res.getData());
		Assert.assertTrue(res.getData() instanceof List);
		
//		for(Object field : (List<?>) res.getData())
//			System.err.println(toJson(field));
	}

	@Test @SuppressWarnings("all")
	public void testAdd() throws Exception {

		Request req = fromJson("add2.js", Request.class);
		Response res = resource.save(req);
		
		Assert.assertNotNull(res);
		Assert.assertNotNull(res.getData());

		Map<String, Object> data = (Map) res.getItem(0);
		
		Contact p = Contact.edit(data);

		Assert.assertEquals(Title.class, p.getTitle().getClass());
		Assert.assertEquals(Address.class, p.getAddresses().get(0).getClass());
		Assert.assertEquals(Group.class, p.getGroups().get(0).getClass());
		Assert.assertEquals(Date.class, p.getDateOfBirth().getClass());

		Assert.assertEquals("mr", p.getTitle().getCode());
		Assert.assertEquals("France", p.getAddresses().get(0).getCountry().getName());
		Assert.assertEquals("family", p.getGroups().get(0).getName());
		Assert.assertEquals("1977-05-01", p.getDateOfBirth().toString());
	}
	
	@Test @SuppressWarnings("all")
	public void testUpdate() throws Exception {

		Request req = fromJson("update.js", Request.class);
		Response res = resource.save(req);
		
		Assert.assertNotNull(res);
		Assert.assertNotNull(res.getData());

		Map<String, Object> data = (Map) res.getItem(0);
		
		Assert.assertEquals("Some", data.get("firstName"));
		Assert.assertEquals("thing", data.get("lastName"));
		Assert.assertEquals("some@thing.com", data.get("email"));
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
