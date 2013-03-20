package com.axelor.auth;

import junit.framework.Assert;

import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.subject.Subject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.axelor.auth.db.User;
import com.axelor.db.JPA;
import com.axelor.test.GuiceModules;
import com.axelor.test.GuiceRunner;
import com.google.inject.persist.Transactional;

@RunWith(GuiceRunner.class)
@GuiceModules({ TestModule.class })
public class AuthTest {
	
	@Before
	@Transactional
	public void setUp() {
		if (User.all().filter("self.code = ?", "demo").count() > 0) {
			return;
		}
		
		User user = new User();
		user.setCode("demo");
		user.setName("Demo");
		user.setPassword("demo");
		
		user.save();
		
		Subject subject = AuthUtils.getSubject();
		UsernamePasswordToken token = new UsernamePasswordToken("demo", "demo");

		subject.login(token);
	}
	
	@Test
	@Transactional
	public void test () {

		User user = User.all().filter("self.code = ?", "demo").fetchOne();
		
		Assert.assertNotNull(user);
		
		User current = AuthUtils.getUser();
		
		Assert.assertTrue(JPA.em().contains(current));

		JPA.clear();

		Assert.assertFalse(JPA.em().contains(current));

		User user2 = new User();
		user2.setCode("demo2");
		user2.setName("Demo2");
		user2.setPassword("demo2");
		
		user2.save();
		
		Assert.assertNotNull(user2.getCreatedBy());
		
	}
}
