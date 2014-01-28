/**
 * Copyright (c) 2012-2014 Axelor. All Rights Reserved.
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
 * Copyright (c) 2012-2014 Axelor. All Rights Reserved.
 */
package com.axelor.auth;

import javax.inject.Inject;

import org.apache.directory.server.annotations.CreateLdapServer;
import org.apache.directory.server.annotations.CreateTransport;
import org.apache.directory.server.core.annotations.ApplyLdifFiles;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.annotations.CreatePartition;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.apache.directory.server.ldap.LdapServer;
import org.apache.shiro.authc.AuthenticationException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.axelor.auth.db.Group;
import com.axelor.auth.db.User;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.persist.Transactional;

@RunWith(FrameworkRunner.class)
@CreateLdapServer(transports = {
	@CreateTransport(protocol = "LDAP"),
	@CreateTransport(protocol = "LDAPS") })
@CreateDS(allowAnonAccess = true, name="axelor", partitions = {
	@CreatePartition(name = "test", suffix = "dc=test,dc=com")
})
public class LdapTest extends AbstractLdapTestUnit {

	private static LdapServer ldapServer = getLdapServer();

	public static class LdapTestModule extends TestModule {

		@Override
		protected void configure() {

			properties.put(AuthLdap.LDAP_SERVER_URL, "ldap://localhost:" + ldapServer.getPort());
			properties.put(AuthLdap.LDAP_SYSTEM_USER, "uid=admin,ou=system");
			properties.put(AuthLdap.LDAP_SYSTEM_PASSWORD, "secret");

			properties.put(AuthLdap.LDAP_GROUP_BASE, "ou=groups,dc=test,dc=com");
			properties.put(AuthLdap.LDAP_USER_BASE, "ou=users,dc=test,dc=com");
			properties.put(AuthLdap.LDAP_GROUP_OBJECT_CLASS, "groupOfUniqueNames");

			properties.put(AuthLdap.LDAP_GROUP_FILTER, "(uniqueMember=uid={0})");
			properties.put(AuthLdap.LDAP_USER_FILTER, "(uid={0})");

			super.configure();
		}
	}

	@Transactional
	public static class LdapTestRunner {

		@Inject
		private AuthLdap authLdap;

		public void test() {

			new Group("test", "Test").save();
			new Group("my", "My").save();

			ensureUsers(0);
			loginFailed();
			ensureUsers(0);
			loginSuccess();
			ensureUsers(1);
			loginSuccess();
			ensureUsers(1);

			// make sure groups are create on ldap server
			Assert.assertTrue(authLdap.ldapGroupExists("(cn={0})", "test"));
			Assert.assertTrue(authLdap.ldapGroupExists("(cn={0})", "my"));
		}

		void ensureUsers(int count) {
			Assert.assertEquals(count, User.all().count());
		}

		void loginFailed() {
			try {
				authLdap.login("jsmith", "Secret");
			} catch (AuthenticationException e) {}
			User user = User.findByCode("jsmith");
			Assert.assertNull(user);
		}

		void loginSuccess() {
			authLdap.login("jsmith", "secret");
			User user = User.findByCode("jsmith");
			Assert.assertNotNull(user);
			Assert.assertEquals("John Smith", user.getName());
			Assert.assertNotNull(user.getGroup());
			Assert.assertEquals("admins", user.getGroup().getCode());
		}
	}

	private Injector injector;

	@Inject
	private LdapTestRunner testRunner;

	@Before
	public void setUp() {
		if (injector == null) {
			injector = Guice.createInjector(new LdapTestModule());
			injector.injectMembers(this);
		}
	}

	@Test
	@ApplyLdifFiles("test.ldif")
	public void test() {
		testRunner.test();
	}
}
