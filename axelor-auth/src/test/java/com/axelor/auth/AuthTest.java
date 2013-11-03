/**
 * Copyright (c) 2012-2013 Axelor. All Rights Reserved.
 *
 * The contents of this file are subject to the Common Public
 * Attribution License Version 1.0 (the “License”); you may not use
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
 * Software distributed under the License is distributed on an “AS IS”
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
 * Copyright (c) 2012-2013 Axelor. All Rights Reserved.
 */
package com.axelor.auth;

import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.subject.Subject;
import org.junit.Assert;
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

		AuthService.getInstance().encrypt(user);

		user.save();

		Subject subject = AuthUtils.getSubject();
		UsernamePasswordToken token = new UsernamePasswordToken("demo", "demo");

		subject.login(token);
	}

	@Test
	public void test() {

	}
	@Transactional
	public void test2() {

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

		AuthService.getInstance().encrypt(user2);

		user2.save();

		Assert.assertNotNull(user2.getCreatedBy());

		AuthService.getInstance().match("demo2", user2.getPassword());
	}
}
