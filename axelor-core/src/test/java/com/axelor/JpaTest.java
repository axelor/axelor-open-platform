/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor;

import com.axelor.auth.AuthService;
import com.axelor.auth.AuthUtils;
import com.axelor.auth.db.User;
import com.axelor.auth.db.repo.UserRepository;
import com.axelor.db.JPA;
import com.axelor.db.JpaSupport;
import com.axelor.db.Model;
import com.axelor.test.GuiceExtension;
import com.axelor.test.GuiceModules;
import com.axelor.test.db.Contact;
import com.axelor.test.fixture.Fixture;
import com.google.inject.persist.Transactional;
import jakarta.inject.Inject;
import java.io.IOException;
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

  @Inject private Fixture fixture;

  @Inject private AuthService authService;

  @Inject private UserRepository users;

  @BeforeEach
  @Transactional
  public void setUp() throws IOException {
    if (all(Contact.class).count() == 0) {
      fixture("demo-data.yml");
    }
  }

  @Transactional
  protected void fixture(String name) {
    try {
      fixture.load(name, JPA::model, bean -> JPA.manage((Model) bean));
    } catch (IOException e) {
      throw new RuntimeException("Failed to load fixture: " + name, e);
    }
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
