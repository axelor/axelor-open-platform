/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2022 Axelor (<http://axelor.com>).
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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.axelor.JpaTest;
import com.axelor.auth.AuthFilter.UsernamePasswordTokenWithParams;
import com.axelor.auth.db.Group;
import com.axelor.auth.db.Permission;
import com.axelor.auth.db.Role;
import com.axelor.auth.db.User;
import com.axelor.auth.db.repo.UserRepository;
import com.axelor.db.JpaSecurity.AccessType;
import com.google.inject.persist.Transactional;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.subject.Subject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class AuthTest extends JpaTest {

  @Inject private AuthService authService;

  @Inject private AuthSecurity authSecurity;

  @Inject private UserRepository users;

  @BeforeEach
  @Transactional
  public void setUp() {
    if (users.all().count() == 0) {
      createDemoData();
    }
  }

  @AfterAll
  public static void tearDown() {
    final Subject subject = AuthUtils.getSubject();
    try {
      subject.logout();
    } catch (Exception e) {
      // ignore
    }
  }

  private void login(String user, String password) {
    final Subject subject = AuthUtils.getSubject();
    final UsernamePasswordToken token =
        new UsernamePasswordTokenWithParams(user, password, false, null, null);

    subject.login(token);
  }

  private void createDemoData() {

    User admin = new User("admin", "Administrator");
    User demo = new User("demo", "Demo");
    User guest = new User("guest", "Guest");

    admin.setPassword("admin");
    demo.setPassword("demo");
    guest.setPassword("guest");

    Group adminGroup = new Group("admins", "Administrators");
    Group userGroup = new Group("users", "Users");

    admin.setGroup(adminGroup);
    demo.setGroup(userGroup);
    guest.setGroup(userGroup);

    authService.encrypt(admin);
    authService.encrypt(demo);
    authService.encrypt(guest);

    users.save(admin);
    users.save(demo);
    users.save(guest);

    Role superUserRole = new Role("super.user");
    Role normalUserRole = new Role("normal.user");
    Role guestUserRole = new Role("guest.user");

    Permission grantAll = new Permission("grant.all");
    grantAll.setObject("com.axelor.auth.db.*");
    grantAll.setCanCreate(true);
    grantAll.setCanRead(true);
    grantAll.setCanWrite(true);
    grantAll.setCanRemove(true);
    grantAll.setCanExport(true);

    Permission grantRead = new Permission("grant.read");
    grantRead.setObject("com.axelor.auth.db.User");
    grantRead.setCanCreate(false);
    grantRead.setCanRead(true);
    grantRead.setCanWrite(false);
    grantRead.setCanRemove(false);
    grantRead.setCanExport(false);

    Permission grantReadSelf = new Permission("grant.read.self");
    grantReadSelf.setObject("com.axelor.auth.db.User");
    grantReadSelf.setCanCreate(false);
    grantReadSelf.setCanRead(true);
    grantReadSelf.setCanWrite(false);
    grantReadSelf.setCanRemove(false);
    grantReadSelf.setCanExport(false);
    grantReadSelf.setCondition("self.code = ?");
    grantReadSelf.setConditionParams("__user__.code");

    Permission grantWriteSelf = new Permission("grant.write.self");
    grantWriteSelf.setObject("com.axelor.auth.db.User");
    grantWriteSelf.setCanCreate(false);
    grantWriteSelf.setCanRead(true);
    grantWriteSelf.setCanWrite(true);
    grantWriteSelf.setCanRemove(false);
    grantWriteSelf.setCanExport(false);
    grantWriteSelf.setCondition("self.code = ?");
    grantWriteSelf.setConditionParams("__user__.code");

    superUserRole.addPermission(grantAll);
    normalUserRole.addPermission(grantWriteSelf);
    normalUserRole.addPermission(grantReadSelf);
    guestUserRole.addPermission(grantReadSelf);

    adminGroup.addRole(superUserRole);
    userGroup.addRole(normalUserRole);

    guest.addRole(guestUserRole);

    users.save(admin);
    users.save(demo);
    users.save(guest);
  }

  @Test
  public void testSuperUser() {
    login("admin", "admin");
    superUserTest();
  }

  @Transactional
  public void superUserTest() {
    // ensure has super user role
    assertTrue(authSecurity.hasRole("super.user"));

    // check that super use has full permissions
    assertTrue(authSecurity.isPermitted(AccessType.READ, User.class));
    assertTrue(authSecurity.isPermitted(AccessType.WRITE, Group.class));
    assertTrue(authSecurity.isPermitted(AccessType.CREATE, Role.class));
    assertTrue(authSecurity.isPermitted(AccessType.REMOVE, Permission.class));
  }

  @Test
  public void testNormalUser() {
    login("demo", "demo");
    normalUserTest();
  }

  @Transactional
  public void normalUserTest() {
    // ensure has super user role
    assertTrue(authSecurity.hasRole("normal.user"));

    // but not super user role
    assertFalse(authSecurity.hasRole("super.user"));

    // check if has read access to User model
    assertTrue(authSecurity.isPermitted(AccessType.READ, User.class));

    // check if no other models are accessible
    assertFalse(authSecurity.isPermitted(AccessType.READ, Group.class));
    assertFalse(authSecurity.isPermitted(AccessType.READ, Role.class));
    assertFalse(authSecurity.isPermitted(AccessType.READ, Permission.class));

    // check if can update own user instance
    assertTrue(
        authSecurity.isPermitted(AccessType.WRITE, User.class, users.findByCode("demo").getId()));
    // but not others
    assertFalse(
        authSecurity.isPermitted(AccessType.WRITE, User.class, users.findByCode("admin").getId()));
  }

  @Test
  public void testGuestUser() {
    login("guest", "guest");
    guestUserTest();
  }

  @Transactional
  public void guestUserTest() {
    // ensure has super user role
    assertTrue(authSecurity.hasRole("guest.user"));

    // but not super user role
    assertFalse(authSecurity.hasRole("super.user"));

    // check if has read access to User model
    assertTrue(authSecurity.isPermitted(AccessType.READ, User.class));
    // and can only read self record
    assertTrue(
        authSecurity.isPermitted(AccessType.READ, User.class, users.findByCode("guest").getId()));
    // and not others
    assertFalse(
        authSecurity.isPermitted(AccessType.READ, User.class, users.findByCode("demo").getId()));

    // check if no other models are accessible
    assertFalse(authSecurity.isPermitted(AccessType.READ, Group.class));
    assertFalse(authSecurity.isPermitted(AccessType.READ, Role.class));
    assertFalse(authSecurity.isPermitted(AccessType.READ, Permission.class));

    // check if can update own user instance
    assertTrue(
        authSecurity.isPermitted(AccessType.WRITE, User.class, users.findByCode("guest").getId()));
    // but not others
    assertFalse(
        authSecurity.isPermitted(AccessType.WRITE, User.class, users.findByCode("demo").getId()));
  }

  @Test
  public void testEncrypt() {
    login("demo", "demo");
    encryptTest();
  }

  @Transactional
  public void encryptTest() {

    EntityManager em = getEntityManager();
    User user = users.all().filter("self.code = ?", "demo").fetchOne();

    assertNotNull(user);

    User current = AuthUtils.getUser();

    assertTrue(em.contains(current));

    em.clear();

    assertFalse(em.contains(current));

    User user2 = new User();
    user2.setCode("demo2");
    user2.setName("Demo2");
    user2.setPassword("demo2");

    authService.encrypt(user2);

    users.save(user2);

    assertNotNull(user2.getCreatedBy());

    authService.match("demo2", user2.getPassword());
  }
}
