/**
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2017 Axelor (<http://axelor.com>).
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

			try {
				return service.ldapLogin((String) token.getPrincipal(), (String) plain);
			} catch (IllegalStateException e) {
			} catch (AuthenticationException e) {
				return false;
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
		final String passwd = new String(((UsernamePasswordToken) token).getPassword());

		final AuthService service = AuthService.getInstance();
		if (service.ldapEnabled()) {
			try {
				service.ldapLogin(code, passwd);
			} catch (IllegalStateException e) {
			} catch (AuthenticationException e) {
			}
		}

		final User user = AuthUtils.getUser(code);
		if (user == null || !AuthUtils.isActive(user)) {
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
