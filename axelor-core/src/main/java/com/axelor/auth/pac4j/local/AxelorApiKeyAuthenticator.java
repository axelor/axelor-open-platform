/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2025 Axelor (<http://axelor.com>).
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
package com.axelor.auth.pac4j.local;

import static com.axelor.auth.UserTokenService.TOKEN_KEY_LENGTH;

import com.axelor.auth.AuthService;
import com.axelor.auth.AuthUtils;
import com.axelor.auth.db.UserToken;
import com.axelor.auth.db.repo.UserTokenRepository;
import com.axelor.common.StringUtils;
import com.google.inject.Inject;
import com.google.inject.persist.Transactional;
import java.time.LocalDateTime;
import java.util.Optional;
import org.pac4j.core.context.CallContext;
import org.pac4j.core.credentials.Credentials;
import org.pac4j.core.credentials.TokenCredentials;
import org.pac4j.core.credentials.authenticator.Authenticator;
import org.pac4j.core.exception.AccountNotFoundException;
import org.pac4j.core.exception.BadCredentialsException;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.core.util.Pac4jConstants;

/**
 * Authenticator implementation for API key-based authentication in Axelor.
 *
 * <p>This authenticator validates API key credentials
 *
 * <p>The authenticator performs the following validations:
 *
 * <ul>
 *   <li>Verifies the API key is present and has the correct format
 *   <li>Validates the token against the stored digest
 *   <li>Checks if the token has not expired
 *   <li>Confirms the associated user account is active
 * </ul>
 *
 * <p>When authentication is successful, the authenticator updates the "last used" of the token and
 * creates a user profile for the authenticated user.
 */
public class AxelorApiKeyAuthenticator implements Authenticator {
  public static final String UNSUPPORTED_CREDENTIALS = "Unsupported credentials";
  public static final String MISSING_API_KEY = "No API key provided";
  public static final String INVALID_API_KEY = "Invalid or expired API key";
  public static final String USER_DISABLED = "User is disabled.";
  @Inject UserTokenRepository userTokenRepository;

  @Override
  public Optional<Credentials> validate(CallContext ctx, Credentials credentials) {
    if (credentials instanceof TokenCredentials tokenCredentials) {
      String apiKey = tokenCredentials.getToken();
      if (StringUtils.isBlank(apiKey)) {
        throw new BadCredentialsException(MISSING_API_KEY);
      }
      if (apiKey.length() <= TOKEN_KEY_LENGTH) {
        throw new BadCredentialsException(UNSUPPORTED_CREDENTIALS);
      }

      String key = apiKey.substring(0, TOKEN_KEY_LENGTH);
      String token = apiKey.substring(TOKEN_KEY_LENGTH);

      UserToken userToken = userTokenRepository.findByKey(key);
      if (userToken == null) {
        throw new AccountNotFoundException(INVALID_API_KEY);
      }
      AuthService authService = AuthService.getInstance();

      if (!authService.match(token, userToken.getTokenDigest())) {
        throw new AccountNotFoundException(INVALID_API_KEY);
      }
      if (userToken.getExpiresAt().isBefore(LocalDateTime.now())) {
        throw new AccountNotFoundException(INVALID_API_KEY);
      }
      if (!AuthUtils.isActive(userToken.getOwner())) {
        throw new AccountNotFoundException(USER_DISABLED);
      }
      setUserTokenLastUsed(userToken);
      CommonProfile profile = new CommonProfile();
      profile.setId(userToken.getOwner().getCode());
      profile.addAttribute(Pac4jConstants.USERNAME, userToken.getOwner().getCode());

      credentials.setUserProfile(profile);

      return Optional.of(credentials);

    } else {
      throw new BadCredentialsException(UNSUPPORTED_CREDENTIALS);
    }
  }

  @Transactional
  public void setUserTokenLastUsed(UserToken userToken) {
    userToken.setLastUsedAt(LocalDateTime.now());
  }
}
