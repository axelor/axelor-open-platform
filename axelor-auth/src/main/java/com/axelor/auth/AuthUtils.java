package com.axelor.auth;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;

import com.axelor.auth.db.User;

public class AuthUtils {
	
	public static Subject getSubject() {
		return SecurityUtils.getSubject();
	}

	public static User getUser() {
		Subject subject = getSubject();
		if (subject == null || subject.getPrincipal() == null)
			return null;
		return User.all().filter("self.code = ?", subject.getPrincipal()).fetchOne();
	}
}
