package com.axelor.db;

import javax.inject.Inject;

import com.axelor.MyModule;
import com.axelor.db.Fixture;
import com.axelor.domain.Contact;
import com.axelor.test.GuiceModules;
import com.axelor.test.GuiceRunner;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(GuiceRunner.class)
@GuiceModules(MyModule.class)
public class FixtureTest {

	@Inject
	Fixture fixture;

	@Before
	public void setUp() {
		if (Contact.all().count() == 0) {
			fixture.load("demo");
		}
	}

	@Test
	public void testInjected() {
		Assert.assertNotNull(fixture);
	}

	@Test
	public void testCount() {
		Assert.assertTrue(Contact.all().count() > 0);
	}

}
