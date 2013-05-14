package com.axelor.auth;

import java.util.Map;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.web.filter.authc.FormAuthenticationFilter;

import com.fasterxml.jackson.databind.ObjectMapper;

public class AuthFilter extends FormAuthenticationFilter {
	
	@Override
	protected boolean onAccessDenied(ServletRequest request, ServletResponse response) throws Exception {

		if ("XMLHttpRequest".equals(((HttpServletRequest) request).getHeader("X-Requested-With"))) {

			if (isLoginRequest(request, response) && isLoginSubmission(request, response)) {

				ObjectMapper mapper = new ObjectMapper();
				@SuppressWarnings("unchecked")
				Map<String, String> data = mapper.readValue(request.getInputStream(), Map.class);
				
				String username = data.get("username");
				String password = data.get("password");
				
				AuthenticationToken token = createToken(username, password, request, response);
				 try {
		            Subject subject = getSubject(request, response);
		            subject.login(token);
		            return onLoginSuccess(token, subject, request, response);
		        } catch (AuthenticationException e) {
		        }
			}
			((HttpServletResponse) response).setStatus(401);
			return false;
		}
		return super.onAccessDenied(request, response);
	}
	
	@Override
	protected boolean onLoginSuccess(AuthenticationToken token,
			Subject subject, ServletRequest request, ServletResponse response) throws Exception {
		HttpSession session = ((HttpServletRequest) request).getSession();
		Object timeout = session.getServletContext().getAttribute("session.timeout");
		if (timeout instanceof Integer) {
			session.setMaxInactiveInterval(((Integer) timeout) * 60);
		}
		return super.onLoginSuccess(token, subject, request, response);
	}
}
