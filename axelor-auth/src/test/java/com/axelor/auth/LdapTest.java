package com.axelor.auth;

import java.util.Properties;

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
import com.google.inject.name.Names;
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
			super.configure();

			Properties properties = new Properties();

			properties.put(AuthLdap.LDAP_SERVER_URL, "ldap://localhost:" + ldapServer.getPort());
			properties.put(AuthLdap.LDAP_SYSTEM_USER, "uid=admin,ou=system");
			properties.put(AuthLdap.LDAP_SYSTEM_PASSWORD, "secret");

			properties.put(AuthLdap.LDAP_GROUP_BASE, "ou=groups,dc=test,dc=com");
			properties.put(AuthLdap.LDAP_USER_BASE, "ou=users,dc=test,dc=com");
			properties.put(AuthLdap.LDAP_GROUP_OBJECT_CLASS, "groupOfUniqueNames");

			properties.put(AuthLdap.LDAP_GROUP_FILTER, "(uniqueMember=uid={0})");
			properties.put(AuthLdap.LDAP_USER_FILTER, "(uid={0})");

			bind(Properties.class).annotatedWith(Names.named("auth.ldap.config")).toInstance(properties);
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
