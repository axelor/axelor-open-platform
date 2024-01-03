/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2024 Axelor (<http://axelor.com>).
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.axelor;

import com.axelor.auth.AuthService;
import com.axelor.auth.AuthUtils;
import com.axelor.auth.db.User;
import com.axelor.auth.db.repo.UserRepository;
import com.axelor.db.JpaFixture;
import com.axelor.db.JpaSupport;
import com.axelor.test.GuiceExtension;
import com.axelor.test.GuiceModules;
import com.axelor.test.db.Contact;
import com.google.inject.persist.Transactional;
import javax.inject.Inject;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.subject.Subject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GuiceExtension.class)
@GuiceModules({JpaTestModule.class})
@TestMethodOrder(MethodOrderer.MethodName.class)
public abstract class JpaTest extends JpaSupport {

  @Inject private JpaFixture fixture;

  @Inject private AuthService authService;

  @Inject private UserRepository users;

  @BeforeEach
  @Transactional
  public void setUp() {
    if (all(Contact.class).count() == 0) {
      fixture.load("demo-data.yml");
    }
  }

  protected void fixture(String name) {
    fixture.load(name);
  }

  protected void login(String username, String password) {
    Subject subject = AuthUtils.getSubject();
    UsernamePasswordToken token = new UsernamePasswordToken(username, password, false, null);
    subject.login(token);
  }

  protected void ensureAuth(String username, String password) {
    User user = users.findByCode(username);
    if (user == null) {
      user = new User(username, username);
      user.setPassword(password);
      authService.encrypt(user);
      users.save(user);
    }
    login(username, password);
  }
}
