/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2019 Axelor (<http://axelor.com>).
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
package com.axelor.auth;

import com.axelor.JpaTestModule;
import com.axelor.auth.db.Group;
import com.axelor.auth.db.User;
import com.axelor.auth.db.repo.GroupRepository;
import com.axelor.auth.db.repo.UserRepository;
import com.axelor.auth.ldap.AuthLdapService;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.persist.Transactional;
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

@RunWith(FrameworkRunner.class)
@CreateLdapServer(
    transports = {@CreateTransport(protocol = "LDAP"), @CreateTransport(protocol = "LDAPS")})
@CreateDS(
    allowAnonAccess = true,
    name = "axelor",
    partitions = {@CreatePartition(name = "test", suffix = "dc=test,dc=com")})
public class LdapTest extends AbstractLdapTestUnit {

  private static LdapServer ldapServer = getLdapServer();

  public static class LdapTestModule extends JpaTestModule {

    @Override
    protected void configure() {

      Properties properties = new Properties();

      properties.put(AuthLdapService.LDAP_SERVER_URL, "ldap://localhost:" + ldapServer.getPort());
      properties.put(AuthLdapService.LDAP_SYSTEM_USER, "uid=admin,ou=system");
      properties.put(AuthLdapService.LDAP_SYSTEM_PASSWORD, "secret");

      properties.put(AuthLdapService.LDAP_GROUP_BASE, "ou=groups,dc=test,dc=com");
      properties.put(AuthLdapService.LDAP_USER_BASE, "ou=users,dc=test,dc=com");
      properties.put(AuthLdapService.LDAP_GROUP_OBJECT_CLASS, "groupOfUniqueNames");

      properties.put(AuthLdapService.LDAP_GROUP_FILTER, "(uniqueMember=uid={0})");
      properties.put(AuthLdapService.LDAP_USER_FILTER, "(uid={0})");

      AuthLdapService ldap = new AuthLdapService(properties);
      bind(AuthLdapService.class).toInstance(ldap);

      super.configure();
    }
  }

  @Transactional
  public static class LdapTestRunner {

    @Inject private AuthLdapService authLdap;

    @Inject private UserRepository users;

    @Inject private GroupRepository groups;

    public void test() {

      Group testGroup = new Group("test", "Test");
      Group myGroup = new Group("my", "My");

      groups.save(testGroup);
      groups.save(myGroup);

      // make sure there are no users
      ensureUsers(0);

      // attempt login with wrong password
      loginFailed();

      // it should not create user
      ensureUsers(0);

      // attempt login with good password
      loginSuccess();

      // it should create an user
      ensureUsers(1);

      // attempt login again with good password
      loginSuccess();

      // it should not create new user, as it's already created
      ensureUsers(1);

      // make sure groups are create on ldap server
      Assert.assertTrue(authLdap.ldapGroupExists("(cn={0})", "test"));
      Assert.assertTrue(authLdap.ldapGroupExists("(cn={0})", "my"));
    }

    void ensureUsers(int count) {
      Assert.assertEquals(count, users.all().count());
    }

    void loginFailed() {
      try {
        authLdap.login("jsmith", "Secret");
      } catch (AuthenticationException e) {
      }
      User user = users.findByCode("jsmith");
      Assert.assertNull(user);
    }

    void loginSuccess() {
      authLdap.login("jsmith", "secret");
      User user = users.findByCode("jsmith");
      Assert.assertNotNull(user);
      Assert.assertEquals("John Smith", user.getName());
      Assert.assertNotNull(user.getGroup());
      Assert.assertEquals("admins", user.getGroup().getCode());
    }
  }

  private Injector injector;

  @Inject private LdapTestRunner testRunner;

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
