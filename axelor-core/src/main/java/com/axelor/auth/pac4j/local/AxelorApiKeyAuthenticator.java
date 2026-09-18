/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
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
 * Authenticator implementation for API key-based authentication.
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
 * <p>When authentication is successful, the authenticator updates the "last used" date of the
 * token.
 */
public class AxelorApiKeyAuthenticator implements Authenticator {

  public static final String UNSUPPORTED_CREDENTIALS = "Unsupported credentials";
  public static final String MISSING_API_KEY = "No API key provided";
  public static final String INVALID_API_KEY = "Invalid or expired API key";
  public static final String USER_DISABLED = "User is disabled.";

  @Inject UserTokenRepository userTokenRepository;

  @Override
  public Optional<Credentials> validate(CallContext ctx, Credentials credentials) {

    if (!(credentials instanceof TokenCredentials tokenCredentials)) {
      throw new BadCredentialsException(UNSUPPORTED_CREDENTIALS);
    }

    // Extract the key and the token from the API key
    ApiKey apiKey = extractApiKey(tokenCredentials);

    // Validate the credentials and get the matching user token
    UserToken userToken = validateUser(apiKey);

    // Update the token last used date
    setUserTokenLastUsed(userToken);

    // Create the profile
    CommonProfile profile = new CommonProfile();
    profile.setId(userToken.getOwner().getCode());
    profile.addAttribute(Pac4jConstants.USERNAME, userToken.getOwner().getCode());
    credentials.setUserProfile(profile);

    return Optional.of(credentials);
  }

  /**
   * Extracts the key and the token composing the API key of the given credentials.
   *
   * @param tokenCredentials the credentials holding the API key
   * @return the key and the token extracted from the API key
   * @throws BadCredentialsException if the API key is missing or malformed
   */
  protected ApiKey extractApiKey(TokenCredentials tokenCredentials) {
    final String apiKey = tokenCredentials.getToken();

    // Check if the API key is provided
    if (StringUtils.isBlank(apiKey)) {
      throw new BadCredentialsException(MISSING_API_KEY);
    }

    // Check the API key format
    if (apiKey.length() <= TOKEN_KEY_LENGTH) {
      throw new BadCredentialsException(UNSUPPORTED_CREDENTIALS);
    }

    return new ApiKey(apiKey.substring(0, TOKEN_KEY_LENGTH), apiKey.substring(TOKEN_KEY_LENGTH));
  }

  /**
   * Finds the user token matching the given API key.
   *
   * @param apiKey the key and the token extracted from the API key
   * @return the matching user token, owned by an active user
   * @throws AccountNotFoundException if the API key is invalid or expired, or if its owner is
   *     disabled
   */
  protected UserToken validateUser(ApiKey apiKey) {
    // Check if the key matches a stored token
    UserToken userToken = userTokenRepository.findByKey(apiKey.key());
    if (userToken == null) {
      throw new AccountNotFoundException(INVALID_API_KEY);
    }

    // Check if the token matches the stored digest
    if (StringUtils.isBlank(apiKey.token())
        || !AuthService.getInstance().match(apiKey.token(), userToken.getTokenDigest())) {
      throw new AccountNotFoundException(INVALID_API_KEY);
    }

    // Check if the token has expired
    if (userToken.getExpiresAt().isBefore(LocalDateTime.now())) {
      throw new AccountNotFoundException(INVALID_API_KEY);
    }

    // Check if the owner is active
    if (!AuthUtils.isActive(userToken.getOwner())) {
      throw new AccountNotFoundException(USER_DISABLED);
    }

    return userToken;
  }

  @Transactional
  public void setUserTokenLastUsed(UserToken userToken) {
    userToken.setLastUsedAt(LocalDateTime.now());
  }

  /**
   * The two parts composing an API key.
   *
   * @param key the key used to look up the stored {@link UserToken}
   * @param token the token matched against the stored digest
   */
  protected record ApiKey(String key, String token) {}
}
