package com.axelor.auth;

import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.SimpleAuthenticationInfo;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;
import org.joda.time.LocalDate;

import com.axelor.auth.db.Group;
import com.axelor.auth.db.User;

public class AuthRealm extends AuthorizingRealm {

	@Override
	protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {

		String code = ((UsernamePasswordToken) token).getUsername();
		User user = User.all().filter("self.code = ?1", code).fetchOne();

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

		String code = (String) principals.fromRealm(getName()).iterator().next();
		User user = User.all().filter("self.code = ?1", code).fetchOne();

		if (user == null) {
			return null;
		}

		SimpleAuthorizationInfo info = new SimpleAuthorizationInfo();
		Group group = user.getGroup();
		if (group != null)
			info.addRole(group.getCode());
		return info;
	}
}
