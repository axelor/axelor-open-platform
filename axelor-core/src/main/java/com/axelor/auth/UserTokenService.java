/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.auth;

import com.axelor.auth.db.User;
import com.axelor.auth.db.UserToken;
import com.axelor.auth.db.repo.UserRepository;
import com.axelor.auth.db.repo.UserTokenRepository;
import com.google.inject.Inject;
import com.google.inject.persist.Transactional;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import org.hibernate.exception.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service for managing API tokens for user authentication.
 *
 * <p>This service provides functionality to generate, rotate, and revoke API keys that are used for
 * authenticating users via the API key authentication mechanism.
 *
 * <p>Key features of this service:
 *
 * <ul>
 *   <li>Generates cryptographically secure random strings for keys and tokens
 *   <li>Ensures uniqueness of generated keys and tokens
 *   <li>Manages the lifecycle of user tokens (creation, rotation, revocation)
 * </ul>
 *
 * <p>The service uses {@link SecureRandom} for generating random strings and {@link AuthService}
 * for encrypting tokens before storage.
 */
public class UserTokenService {
  private static final Logger log = LoggerFactory.getLogger(UserTokenService.class);
  private static final SecureRandom secureRandom = new SecureRandom();

  public static final int TOKEN_KEY_LENGTH = 16;
  private static final int TOKEN_LENGTH = 32;
  private static final int MAX_TRIES = 3;
  private static final String CHARACTERS =
      "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789_";

  private final UserTokenRepository userTokenRepository;
  private final UserRepository userRepository;

  @Inject
  public UserTokenService(UserTokenRepository userTokenRepository, UserRepository userRepository) {
    this.userTokenRepository = userTokenRepository;
    this.userRepository = userRepository;
  }

  public UserToken createUserToken(String name, LocalDateTime expiresAt, User owner) {
    AuthService authService = AuthService.getInstance();
    String token = generateRandomString(TOKEN_LENGTH);
    Long ownerId = owner.getId();
    int retries = 0;
    String key;
    UserToken ut;

    while (true) {
      try {
        key = generateRandomString(TOKEN_KEY_LENGTH);
        ut = new UserToken();
        owner = userRepository.find(ownerId);
        ut.setName(name);
        ut.setExpiresAt(expiresAt);
        ut.setOwner(owner);
        ut.setTokenDigest(authService.encrypt(token));
        ut.setTokenKey(key);
        ut = saveUserToken(ut);
        break;
      } catch (ConstraintViolationException e) {
        retries++;
        if (retries >= MAX_TRIES) {
          throw e;
        }
        log.debug(
            "API Key creation failed due to unique constraint violation , retrying ({})...",
            retries);
      }
    }

    ut.setApiKey(key.concat(token));
    return ut;
  }

  @Transactional
  protected UserToken saveUserToken(UserToken ut) {
    return userTokenRepository.save(ut);
  }

  public UserToken rotateUserToken(UserToken userToken) {
    AuthService authService = AuthService.getInstance();
    String key;
    String token = generateRandomString(TOKEN_LENGTH);
    int retries = 0;
    while (true) {
      try {
        key = generateRandomString(TOKEN_KEY_LENGTH);
        userToken = userTokenRepository.find(userToken.getId());
        userToken.setTokenKey(key);
        userToken.setTokenDigest(authService.encrypt(token));
        saveUserToken(userToken);
        break;
      } catch (ConstraintViolationException e) {
        retries++;
        if (retries >= MAX_TRIES) {
          throw e;
        }
        log.debug(
            "API Key rotation failed due to unique constraint violation , retrying ({})...",
            retries);
      }
    }
    userToken.setApiKey(key.concat(token));
    return userToken;
  }

  @Transactional
  public void revokeUserToken(UserToken userToken) {
    userTokenRepository.remove(userToken);
  }

  protected String generateRandomString(int length) {
    StringBuilder sb = new StringBuilder(length);
    int charactersLength = CHARACTERS.length();
    for (int i = 0; i < length; i++) {
      sb.append(CHARACTERS.charAt(secureRandom.nextInt(charactersLength)));
    }
    return sb.toString();
  }
}
