/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2018 Axelor (<http://axelor.com>).
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

import com.axelor.auth.db.User;
import com.axelor.db.mapper.Mapper;
import com.axelor.db.mapper.Property;
import com.axelor.inject.Beans;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.axelor.rpc.Context;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.apache.shiro.authc.credential.DefaultPasswordService;
import org.apache.shiro.crypto.hash.DefaultHashService;
import org.apache.shiro.crypto.hash.format.ParsableHashFormat;
import org.apache.shiro.crypto.hash.format.Shiro1CryptFormat;

/**
 * The {@link AuthService} class provides various utility services including password encryption,
 * password match and saving user password in encrypted form.
 *
 * <p>The {@link AuthService} should not be manually instantiated but either injected or user {@link
 * #getInstance()} method to get the instance of the service.
 */
@Singleton
public class AuthService {

  private final String HASH_ALGORITHM = "SHA-512";
  private final int HASH_ITERATIONS = 500000;

  private final DefaultPasswordService passwordService = new DefaultPasswordService();

  private final DefaultHashService hashService = new DefaultHashService();

  private final ParsableHashFormat hashFormat = new Shiro1CryptFormat();

  @Inject
  public AuthService() {
    this.hashService.setHashAlgorithmName(HASH_ALGORITHM);
    this.hashService.setHashIterations(HASH_ITERATIONS);
    this.hashService.setGeneratePublicSalt(true);
    this.passwordService.setHashService(hashService);
    this.passwordService.setHashFormat(hashFormat);
  }

  /**
   * Get the instance of the {@link AuthService}.
   *
   * @throws IllegalStateException if AuthService is not initialized
   * @return the {@link AuthService} instance
   */
  public static AuthService getInstance() {
    try {
      return Beans.get(AuthService.class);
    } catch (Exception e) {
      throw new IllegalStateException(
          "AuthService is not initialized, did you forget to bind the AuthService?");
    }
  }

  /**
   * Encrypt the given password text if it's not encrypted yet.
   *
   * <p>The method tests the password for a special format to check if it is already encrypted, and
   * In that case the password is returned as it is to avoid multiple encryption.
   *
   * @param password the password to encrypt
   * @return encrypted password
   */
  public String encrypt(String password) {
    try {
      hashFormat.parse(password);
      return password;
    } catch (IllegalArgumentException e) {
    }
    return passwordService.encryptPassword(password);
  }

  /**
   * Encrypt the password of the given user.
   *
   * @param user the user whose password needs to be encrypted
   * @return the same user instance
   */
  public User encrypt(User user) {
    user.setPassword(encrypt(user.getPassword()));
    return user;
  }

  /**
   * This is an adapter method to be used with data import.
   *
   * <p>This method can be used as <code>call="com.axelor.auth.AuthService:encrypt"</code> while
   * importing user data to ensure user passwords are encrypted.
   *
   * @param user the object instance passed by data import engine
   * @param context the data import context
   * @return the same instance passed
   */
  public Object encrypt(Object user, @SuppressWarnings("rawtypes") Map context) {
    if (user instanceof User) {
      return encrypt((User) user);
    }
    return user;
  }

  /**
   * Match the given plain and saved passwords.
   *
   * @param plain the plain password text
   * @param saved the saved password text (hashed)
   * @return true if they match
   */
  public boolean match(String plain, String saved) {
    return passwordService.passwordsMatch(plain, saved);
  }

  /**
   * Helper action to check user password.
   *
   * @param request the request with username & password as context or data
   * @param response the response, with user details if password matched
   */
  public void checkPassword(ActionRequest request, ActionResponse response) {
    final Context context = request.getContext();
    final Map<String, Object> data = context == null ? request.getData() : context;

    final String username = (String) data.getOrDefault("username", data.get("code"));
    final String password = (String) data.getOrDefault("password", data.get("newPassword"));

    final User user = AuthUtils.getUser(username);
    if (user == null || !match(password, user.getPassword())) {
      response.setStatus(ActionResponse.STATUS_FAILURE);
      response.setError("No such user or password doesn't match.");
      return;
    }

    final Mapper mapper = Mapper.of(User.class);
    final Property name = mapper.getNameField();
    response.setValue("id", user.getId());
    response.setValue("name", name.get(user));
    response.setValue("nameField", name.getName());
    response.setValue("login", user.getCode());
    response.setValue("lang", user.getLanguage());
  }
}
