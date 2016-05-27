/**
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2014 Axelor (<http://axelor.com>).
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

import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.credential.DefaultPasswordService;
import org.apache.shiro.crypto.hash.DefaultHashService;
import org.apache.shiro.crypto.hash.format.ParsableHashFormat;
import org.apache.shiro.crypto.hash.format.Shiro1CryptFormat;

import com.axelor.auth.db.User;
import com.axelor.i18n.I18n;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.axelor.rpc.Context;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.inject.persist.Transactional;

/**
 * The {@link AuthService} class provides various utility services including
 * password encryption, password match and saving user password in encrypted
 * form.
 * <p>
 * The {@link AuthService} should not be manually instantiated but either
 * injected or user {@link #getInstance()} method to get the instance of the
 * service.
 *
 */
@Singleton
public class AuthService {

	private final DefaultPasswordService passwordService = new DefaultPasswordService();

	private final DefaultHashService hashService = new DefaultHashService();

	private final ParsableHashFormat hashFormat = new Shiro1CryptFormat();

	public static AuthService instance;
	
	@Inject
	public AuthService(
			@Named("auth.hash.algorithm") String hashAlgorihm,
			@Named("auth.hash.iterations") int hashIterations) {
		super();

		this.hashService.setHashAlgorithmName(hashAlgorihm);
		this.hashService.setHashIterations(hashIterations);
		this.hashService.setGeneratePublicSalt(true);

		this.passwordService.setHashService(hashService);
		this.passwordService.setHashFormat(hashFormat);

		if (instance != null) {
			throw new RuntimeException("AuthService initialized twice.");
		}
		instance = this;
	}

	/**
	 * Get the instance of the {@link AuthService}.
	 *
	 * @throws IllegalStateException
	 *             if AuthService is not initialized
	 * @return the {@link AuthService} instance
	 */
	public static AuthService getInstance() {
		if (instance == null) {
			throw new IllegalStateException("AuthService is not initialized, did you forget to bind the AuthService?");
		}
		return instance;
	}

	@Inject
	private AuthLdap authLdap;

	/**
	 * Perform LDAP authentication.
	 * <p>
	 * The user/group objects are created in the database when user logins first
	 * time via ldap server. The user object created has a random password
	 * generated so the user can not logged in against the local database object
	 * as password is unknown.
	 *
	 * @param subject
	 *            the user login name
	 * @param password
	 *            the user submitted password
	 * @throws IllegalStateException
	 *             if ldap is not enabled
	 * @throws AuthenticationException
	 *             if ldap authentication failed
	 *
	 * @return true if login success else false
	 */
	boolean ldapLogin(String subject, String password) throws AuthenticationException {
		return authLdap.login(subject, password);
	}

	boolean ldapEnabled() {
		return authLdap.isEnabled();
	}

	/**
	 * Encrypt the given password text if it's not encrypted yet.
	 * <p>
	 * The method tests the password for a special format to check if it is
	 * already encrypted, and In that case the password is returned as it is to
	 * avoid multiple encryption.
	 *
	 * @param password
	 *            the password to encrypt
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
	 * @param user
	 *            the user whose password needs to be encrypted
	 * @return the same user instance
	 */
	public User encrypt(User user) {
		user.setPassword(encrypt(user.getPassword()));
		return user;
	}

	/**
	 * This is an adapter method to be used with data import.
	 * <p>
	 * This method can be used as
	 * <code>call="com.axelor.auth.AuthService:encrypt"</code> while importing
	 * user data to ensure user passwords are encrypted.
	 *
	 * @param user
	 *            the object instance passed by data import engine
	 * @param context
	 *            the data import context
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
	 * @param plain
	 *            the plain password text
	 * @param saved
	 *            the saved password text (hashed)
	 * @return true if they match
	 */
	public boolean match(String plain, String saved) {
		// TODO: remove plain text match in final version
		if (Objects.equal(plain, saved)) { // plain text match
			return true;
		}
		return passwordService.passwordsMatch(plain, saved);
	}

	/**
	 * A helper method used to encrypt user password when the user record is
	 * saved with user interface.
	 *
	 * @param request
	 *            the request with user object as context
	 * @param response
	 *            the response, which is updated according to the validation
	 */
	@Transactional
	public void validate(ActionRequest request, ActionResponse response) {
		Context context = request.getContext();
		if (context.get("confirm") == null) {
			return;
		}

		String password = (String) context.get("newPassword");
		String confirm = (String) context.get("confirm");

		if (Objects.equal(password, confirm)) {
			response.setValue("password", encrypt(password));
			response.setValue("newPassword", null);
			response.setValue("confirm", null);
			response.setValue("change", false);
		} else {
			response.setData(ImmutableList.of(ImmutableMap.of("error", I18n.get("Password doesn't match"))));
		}
	}
}
