/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.auth.pac4j.local;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.axelor.JpaTest;
import com.axelor.auth.UserTokenService;
import com.axelor.auth.db.User;
import com.axelor.auth.db.UserToken;
import com.axelor.auth.db.repo.UserRepository;
import com.axelor.db.JPA;
import com.axelor.inject.Beans;
import jakarta.inject.Inject;
import java.lang.reflect.Proxy;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.pac4j.core.context.CallContext;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.credentials.Credentials;
import org.pac4j.core.credentials.TokenCredentials;
import org.pac4j.core.exception.AccountNotFoundException;

class AxelorApiKeyAuthenticatorTest extends JpaTest {

  private static final String OWNER_CODE = "api-key-owner";

  @Inject private AxelorApiKeyAuthenticator authenticator;
  @Inject private UserTokenService userTokenService;

  private static User owner;

  @BeforeAll
  public static void createOwner() {
    JPA.runInTransaction(
        () -> {
          owner = Beans.get(UserRepository.class).findByCode(OWNER_CODE);
          if (owner == null) {
            owner = new User(OWNER_CODE, "API key owner");
            owner = JPA.save(owner);
          }
        });
  }

  @Test
  void testNoRestriction() {
    UserToken userToken = createUserToken(null);
    assertNull(userToken.getAllowedIps());
    assertTrue(validate(userToken, "203.0.113.1").isPresent());
  }

  @Test
  void testBlankRestriction() {
    UserToken userToken = createUserToken(" , ");
    assertNull(userToken.getAllowedIps());
    assertTrue(validate(userToken, "203.0.113.1").isPresent());
    assertTrue(validate(userToken, null).isPresent());
  }

  @Test
  void testAllowedAddress() {
    UserToken userToken = createUserToken("192.0.2.10, 198.51.100.0/24");
    assertTrue(validate(userToken, "192.0.2.10").isPresent());
    assertTrue(validate(userToken, "198.51.100.42").isPresent());
  }

  @Test
  void testDisallowedAddress() {
    UserToken userToken = createUserToken("192.0.2.10, 2001:db8::/32");

    var e = assertThrows(AccountNotFoundException.class, () -> validate(userToken, "192.0.2.11"));
    assertEquals(AxelorApiKeyAuthenticator.INVALID_API_KEY, e.getMessage());

    assertThrows(AccountNotFoundException.class, () -> validate(userToken, "2001:db9::1"));
    assertThrows(AccountNotFoundException.class, () -> validate(userToken, null));
  }

  @Test
  void testAllowedIpsNormalized() {
    UserToken userToken = createUserToken(" 192.0.2.10/24,2001:DB8::1 , 192.0.2.0/24, ");
    assertEquals("192.0.2.0/24, 2001:db8::1", userToken.getAllowedIps());
  }

  @Test
  void testInvalidAllowedIps() {
    assertThrows(IllegalArgumentException.class, () -> createUserToken("example.com"));
    assertThrows(IllegalArgumentException.class, () -> createUserToken("10.0.0.0/33"));
    assertThrows(IllegalArgumentException.class, () -> createUserToken("192.0.2.10 192.0.2.11"));
    assertThrows(IllegalArgumentException.class, () -> createUserToken("192.0.2.10\n192.0.2.11"));
  }

  private UserToken createUserToken(String allowedIps) {
    return userTokenService.createUserToken(
        "test", LocalDateTime.now().plusDays(1), allowedIps, owner);
  }

  private Optional<Credentials> validate(UserToken userToken, String remoteAddr) {
    WebContext webContext =
        (WebContext)
            Proxy.newProxyInstance(
                WebContext.class.getClassLoader(),
                new Class<?>[] {WebContext.class},
                (proxy, method, args) ->
                    "getRemoteAddr".equals(method.getName()) ? remoteAddr : null);
    return authenticator.validate(
        new CallContext(webContext, null), new TokenCredentials(userToken.getApiKey()));
  }
}
