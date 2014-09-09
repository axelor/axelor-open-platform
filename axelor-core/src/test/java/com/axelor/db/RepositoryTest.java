package com.axelor.db;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import javax.inject.Inject;

import org.junit.Test;

import com.axelor.JpaTest;
import com.axelor.JpaTestModule;
import com.axelor.inject.Beans;
import com.axelor.test.GuiceModules;
import com.axelor.test.db.Contact;
import com.axelor.test.db.ContactRepository;
import com.axelor.test.db.ContactRepositoryEx;

@GuiceModules(RepositoryTest.MyModule.class)
public class RepositoryTest extends JpaTest {

	public static class MyModule extends JpaTestModule {

		@Override
		protected void configure() {
			bind(ContactRepository.class).to(ContactRepositoryEx.class);
			super.configure();
		}
	}

	@Inject
	private ContactRepository contacts;

	@Test
	public void test() {
		assertNotNull(contacts);
		assertTrue(contacts instanceof ContactRepositoryEx);

		Query<Contact> q = contacts.all();
		assertNotNull(q);

		// test manual instantiation
		ContactRepository repo = Beans.get(ContactRepository.class);
		assertNotNull(repo);
		assertTrue(repo instanceof ContactRepositoryEx);
	}
}
