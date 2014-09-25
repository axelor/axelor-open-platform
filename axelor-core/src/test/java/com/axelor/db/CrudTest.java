/**
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2014 Axelor (<http://axelor.com>).
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
package com.axelor.db;

import javax.persistence.OptimisticLockException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.axelor.JpaTest;
import com.axelor.test.db.Address;
import com.axelor.test.db.Circle;
import com.axelor.test.db.Contact;
import com.axelor.test.db.Country;
import com.axelor.test.db.Title;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.inject.persist.Transactional;

@Transactional
public class CrudTest  extends JpaTest {
	
	@Before
	public void setUp() {
		if (Contact.all().count() > 0) {
			return;
		}
		
		final Contact contact = new Contact();
		contact.setFirstName("My");
		contact.setLastName("Name");
		contact.setEmail("my.name@gmail.com");
		
		final Title title = new Title();
		title.setCode("mr");
		title.setName("Mr.");
		contact.setTitle(title);
		
		final Country country = new Country();
		country.setCode("FR");
		country.setName("France");
		
		final Address addr1 = new Address();
		addr1.setStreet("My");
		addr1.setArea("Home");
		addr1.setCity("Paris");
		addr1.setZip("123456");
		addr1.setCountry(country);
		addr1.setContact(contact);
		
		final Address addr2 = new Address();
		addr2.setStreet("My");
		addr2.setArea("Office");
		addr2.setCity("Paris");
		addr2.setZip("123456");
		addr2.setCountry(country);
		addr2.setContact(contact);
		
		contact.setAddresses(Lists.newArrayList(addr1, addr2));
		JPA.save(contact);
	}
	
	@Test
	public void testCreate() {
		
		final Contact contact = new Contact();
		contact.setFirstName("Teen");
		contact.setLastName("Teen");
		contact.setEmail("teen.teen@gmail.com");
		
		Title title = new Title();
		title.setCode("miss");
		title.setName("Miss.");
		contact.setTitle(title);
		
		Country country = new Country();
		country.setCode("UK");
		country.setName("United Kingdom");
		
		Address addr1 = new Address();
		addr1.setStreet("My");
		addr1.setArea("Home");
		addr1.setCity("London");
		addr1.setZip("123456");
		addr1.setCountry(country);
		addr1.setContact(contact);
		
		contact.setAddresses(Lists.newArrayList(addr1));

		JPA.save(contact);
		
		for(Model e : Lists.newArrayList(contact, title, addr1, country)) {
			Assert.assertNotNull(e.getId());
			Assert.assertNotNull(e.getVersion());
		}
	}

	@Test
	public void testRead() {
		Contact contact = JPA.all(Contact.class).filter("self.firstName = ?1", "My").fetchOne();
		Assert.assertNotNull(contact);
		
		Contact c1 = JPA.find(Contact.class, contact.getId());
		Assert.assertSame(contact, c1);
		
		JPA.clear(); // clear the context
		
		Contact c2 = JPA.find(Contact.class, contact.getId());
		Assert.assertNotSame(contact, c2);
	}
	
	@Test(expected = OptimisticLockException.class)
	public void testUpdate() {
		final Contact contact = JPA.all(Contact.class).filter("self.firstName = ?1", "My").fetchOne();
		Assert.assertNotNull(contact);
		
		Integer versionPrev = contact.getVersion();
		
		contact.setPhone("9876543210");
		JPA.save(contact);

		Integer versionNext = contact.getVersion();

		Assert.assertTrue(versionNext > versionPrev);
		
		// test optimistic concurrency check
		
		JPA.clear(); 			// clear the jpa context, will detach all the persisted objects
		contact.setVersion(0);  // manipulate version
		
		contact.setPhone("0123456789");
		JPA.save(contact); // this throws OptimisticLockException
	}
	
	@Test(expected = OptimisticLockException.class)
	public void testDeleteUpdated() {
		final Contact contact = JPA.all(Contact.class).filter("self.firstName = ?1", "My").fetchOne();
		Assert.assertNotNull(contact);
		
		JPA.clear();
		contact.setVersion(0);

		JPA.remove(contact);
	}
	
	@Test(expected = OptimisticLockException.class)
	public void testDelete() {
		final Contact contact = JPA.all(Contact.class).filter("self.firstName = ?1", "My").fetchOne();
		Assert.assertNotNull(contact);
		
		JPA.remove(contact);
		
		Contact c1 = JPA.find(Contact.class, contact.getId());
		Assert.assertNull(c1);

		// try to save deleted record
		JPA.save(contact);
	}
	
	@Test
	public void testCopy() {
		Contact c1 = Contact.all().filter("self.addresses is not empty").fetchOne();
		Circle g1 = new Circle();

		g1.setCode("group_x");
		g1.setName("Group X");
		
		g1 = JPA.save(g1);

		Assert.assertNotNull(c1);
		
		if (c1.getCircles() == null) {
			c1.setCircles(Sets.<Circle>newHashSet());
		}
		
		c1.getCircles().add(g1);
		c1 = JPA.save(c1);

		int numItems = c1.getAddresses().size();
		
		Contact c2 = JPA.copy(c1, true);
		c2 = JPA.save(c2);

		Assert.assertNotNull(c1.getCircles());
		Assert.assertNotNull(c2.getCircles());
		
		int numGroups = c1.getCircles().size();

		c2.getCircles().clear();
		c2 = JPA.save(c2);

		Assert.assertNotNull(c2);
		Assert.assertFalse(c1.getId() == c2.getId());

		Assert.assertEquals(numGroups, c1.getCircles().size());
		Assert.assertEquals(0, c2.getCircles().size());
		
		Assert.assertEquals(numItems, c1.getAddresses().size());
		Assert.assertEquals(numItems, c2.getAddresses().size());
	}
	
}
