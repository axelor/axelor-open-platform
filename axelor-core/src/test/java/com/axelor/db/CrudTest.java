package com.axelor.db;

import javax.persistence.OptimisticLockException;

import junit.framework.Assert;

import org.junit.Test;

import com.axelor.BaseTest;
import com.axelor.test.db.Address;
import com.axelor.test.db.Contact;
import com.axelor.test.db.Country;
import com.axelor.test.db.Title;
import com.google.common.collect.Lists;
import com.google.inject.persist.Transactional;

public class CrudTest  extends BaseTest {

	@Test
	public void testCreate() {
		
		final Contact contact = new Contact();
		contact.setFirstName("My");
		contact.setLastName("Name");
		contact.setEmail("my.name@gmail.com");
		
		final Title title = new Title();
		title.setCode("mister");
		title.setName("Mister");
		contact.setTitle(title);
		
		final Country country = new Country();
		country.setCode("my");
		country.setName("My Country");
		
		final Address addr1 = new Address();
		addr1.setStreet("my");
		addr1.setArea("home");
		addr1.setCity("city");
		addr1.setZip("123456");
		addr1.setCountry(country);
		addr1.setContact(contact);
		
		final Address addr2 = new Address();
		addr2.setStreet("my");
		addr2.setArea("office");
		addr2.setCity("city");
		addr2.setZip("123456");
		addr2.setCountry(country);
		addr2.setContact(contact);
		
		contact.setAddresses(Lists.newArrayList(addr1, addr2));
		
		for(Model e : Lists.newArrayList(contact, title, addr1, addr2, country)) {
			Assert.assertNull(e.getId());
			Assert.assertNull(e.getVersion());
		}
		
		JPA.runInTransaction(new Runnable() {
			
			@Override
			public void run() {
				JPA.save(contact);
			}
		});
		
		for(Model e : Lists.newArrayList(contact, title, addr1, addr2, country)) {
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
		JPA.runInTransaction(new Runnable() {
			@Override
			public void run() {
				contact.setPhone("9876543210");
				JPA.save(contact);
			}
		});
		Integer versionNext = contact.getVersion();
		
		Assert.assertTrue(versionNext > versionPrev);
		
		// test optimistic concurrency check
		
		JPA.clear(); 			// clear the jpa context, will detach all the persisted objects
		contact.setVersion(0);  // manipulate version
		
		JPA.runInTransaction(new Runnable() {
			
			@Override
			public void run() {
				contact.setPhone("0123456789");
				JPA.save(contact); // this throws OptimisticLockException
			}
		});
	}
	
	@Test(expected = OptimisticLockException.class)
	public void testDeleteUpdated() {
		final Contact contact = JPA.all(Contact.class).filter("self.firstName = ?1", "My").fetchOne();
		Assert.assertNotNull(contact);
		
		JPA.clear();
		contact.setVersion(0);
		
		JPA.runInTransaction(new Runnable() {
			@Override
			public void run() {
				JPA.remove(contact);
			}
		});
	}
	
	@Test(expected = OptimisticLockException.class)
	public void testDelete() {
		final Contact contact = JPA.all(Contact.class).filter("self.firstName = ?1", "My").fetchOne();
		Assert.assertNotNull(contact);
		
		JPA.runInTransaction(new Runnable() {
			@Override
			public void run() {
				JPA.remove(contact);
			}
		});
		
		Contact c1 = JPA.find(Contact.class, contact.getId());
		Assert.assertNull(c1);

		// try to save deleted record
		JPA.runInTransaction(new Runnable() {

			@Override
			public void run() {
				JPA.save(contact);
			}
		});
	}
	
	@Test
	@Transactional
	public void testCopy() {
		Contact c1 = Contact.all().filter("self.addresses is not empty").fetchOne();
		
		Assert.assertNotNull(c1);
		
		int numItems = c1.getAddresses().size();
		
		Contact c2 = JPA.copy(c1, true);
		c2 = JPA.save(c2);

		Assert.assertNotNull(c2);
		Assert.assertFalse(c1.getId() == c2.getId());
		
		Assert.assertEquals(numItems, c1.getAddresses().size());
		Assert.assertEquals(numItems, c2.getAddresses().size());
	}
	
}
