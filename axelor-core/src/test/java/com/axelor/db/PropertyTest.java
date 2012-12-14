package com.axelor.db;

import org.junit.Assert;
import org.junit.Test;

import com.axelor.BaseTest;
import com.axelor.db.mapper.Mapper;
import com.axelor.db.mapper.Property;
import com.axelor.db.mapper.PropertyType;
import com.axelor.test.db.Address;
import com.axelor.test.db.Contact;

public class PropertyTest extends BaseTest {

	Mapper mapper = Mapper.of(Contact.class);
	
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
