package com.axelor.auth;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.UnavailableSecurityManagerException;
import org.apache.shiro.subject.Subject;

import com.axelor.auth.db.User;

public class AuthUtils {
	
	public static Subject getSubject() {
		try {
			return SecurityUtils.getSubject();
		} catch (UnavailableSecurityManagerException e){
		}
		return null;
	}

	public static User getUser() {
		Subject subject = getSubject();
		if (subject == null || subject.getPrincipal() == null)
			return null;
		return User.all().filter("self.code = ?", subject.getPrincipal()).fetchOne();
	}
}
