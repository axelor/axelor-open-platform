/**
 * Copyright (c) 2012-2013 Axelor. All Rights Reserved.
 *
 * The contents of this file are subject to the Common Public
 * Attribution License Version 1.0 (the “License”); you may not use
 * this file except in compliance with the License. You may obtain a
 * copy of the License at:
 *
 * http://license.axelor.com/.
 *
 * The License is based on the Mozilla Public License Version 1.1 but
 * Sections 14 and 15 have been added to cover use of software over a
 * computer network and provide for limited attribution for the
 * Original Developer. In addition, Exhibit A has been modified to be
 * consistent with Exhibit B.
 *
 * Software distributed under the License is distributed on an “AS IS”
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 *
 * The Original Code is part of "Axelor Business Suite", developed by
 * Axelor exclusively.
 *
 * The Original Developer is the Initial Developer. The Initial Developer of
 * the Original Code is Axelor.
 *
 * All portions of the code written by Axelor are
 * Copyright (c) 2012-2013 Axelor. All Rights Reserved.
 */
package com.axelor.auth;

import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.SimpleAuthenticationInfo;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.authc.credential.CredentialsMatcher;
import org.apache.shiro.authc.credential.PasswordMatcher;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;
import org.joda.time.LocalDate;

import com.axelor.auth.db.Group;
import com.axelor.auth.db.User;

public class AuthRealm extends AuthorizingRealm {

	public static class AuthMatcher extends PasswordMatcher {

		@Override
		public boolean doCredentialsMatch(AuthenticationToken token, AuthenticationInfo info) {

			//TODO: remove plain text match in final version
			Object plain = getSubmittedPassword(token);
			Object saved = getStoredPassword(info);
			AuthService service = AuthService.getInstance();
			if (plain instanceof char[]) {
				plain = new String((char[]) plain);
			}
			if (service.match((String) plain, (String) saved)) {
				return true;
			}

			return super.doCredentialsMatch(token, info);
		}
	}

	private CredentialsMatcher credentialsMatcher = new AuthMatcher();

	@Override
	public CredentialsMatcher getCredentialsMatcher() {
		return credentialsMatcher;
	}

	@Override
	protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {

		final String code = ((UsernamePasswordToken) token).getUsername();
		final User user = AuthUtils.getUser(code);

		if (user == null || user.getBlocked() == true) {
			return null;
		} else if ((user.getActiveFrom() != null && user.getActiveFrom().isAfter(new LocalDate())) ||
				(user.getActiveTo() != null && user.getActiveTo().isBefore(new LocalDate()))) {
			return null;
		}

		return new SimpleAuthenticationInfo(code, user.getPassword(), getName());
	}

	@Override
	protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals) {

		final String code = (String) principals.fromRealm(getName()).iterator().next();
		final User user = AuthUtils.getUser(code);

		if (user == null) {
			return null;
		}

		final SimpleAuthorizationInfo info = new SimpleAuthorizationInfo();
		final Group group = user.getGroup();
		if (group != null) {
			info.addRole(group.getCode());
		}

		return info;
	}
}
