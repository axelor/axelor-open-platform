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

import java.io.IOException;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.web.filter.authc.FormAuthenticationFilter;
import org.apache.shiro.web.util.WebUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

public class AuthFilter extends FormAuthenticationFilter {

	@Inject
	@Named("app.loginUrl")
	private String loginUrl;

	@Override
	public String getLoginUrl() {
		if (loginUrl != null) {
			return loginUrl;
		}
		return super.getLoginUrl();
	}

	private boolean isRootWithoutSlash(ServletRequest request) {
		final HttpServletRequest req = (HttpServletRequest) request;
		final String ctx = WebUtils.getContextPath(req);
		final String uri = WebUtils.getRequestUri(req);
		return ctx != null && uri != null && !uri.endsWith("/") && ctx.length() == uri.length();
	}

	@Override
	public void doFilterInternal(ServletRequest request, ServletResponse response, FilterChain chain)
			throws ServletException, IOException {

		// tomcat 7.0.67 doesn't redirect with / if root request is sent without slash
		// see RM-4500 for more details
		if (!SecurityUtils.getSubject().isAuthenticated() && isRootWithoutSlash(request)) {
			WebUtils.issueRedirect(request, response, "/");
			return;
		}

		if (isLoginRequest(request, response) && SecurityUtils.getSubject().isAuthenticated()) {
			// in case of login submission with ajax
			if (isXHR(request) && isLoginSubmission(request, response)) {
				WebUtils.toHttp(response).setStatus(200);
				return;
			}
			WebUtils.issueRedirect(request, response, "/");
		}
		super.doFilterInternal(request, response, chain);
	}

	@Override
	protected boolean onAccessDenied(ServletRequest request, ServletResponse response) throws Exception {
		
		// set encoding to UTF-8 (see RM-4304)
		request.setCharacterEncoding("UTF-8");

		if (isXHR(request)) {
			
			int status = 401;
			if (isLoginRequest(request, response) && isLoginSubmission(request, response)) {
				if (doLogin(request, response)) {
					status = 200;
				}
			}

			// set HTTP status for ajax requests
			((HttpServletResponse) response).setStatus(status);

			// don't process further, otherwise login.jsp will be rendered as response data
			return false;
		}

		return super.onAccessDenied(request, response);
	}
	
	@Override
	protected boolean onLoginSuccess(AuthenticationToken token, Subject subject, ServletRequest request,
			ServletResponse response) throws Exception {
		// change session id to prevent session fixation
		((HttpServletRequest) request).changeSessionId();
		return super.onLoginSuccess(token, subject, request, response);
	}

	@SuppressWarnings("unchecked")
	private boolean doLogin(ServletRequest request, ServletResponse response) throws Exception {

		final ObjectMapper mapper = new ObjectMapper();
		final Map<String, String> data = mapper.readValue(request.getInputStream(), Map.class);

		final String username = data.get("username");
		final String password = data.get("password");

		final AuthenticationToken token = createToken(username, password, request, response);
		final Subject subject = getSubject(request, response);

		try {
			subject.login(token);
		} catch (AuthenticationException e) {
			return false;
		}

		return true;
	}

	private boolean isXHR(ServletRequest request) {
		final HttpServletRequest req = (HttpServletRequest) request;
		return "XMLHttpRequest".equals(req.getHeader("X-Requested-With"))
				|| "application/json".equals(req.getHeader("Accept"))
				|| "application/json".equals(req.getHeader("Content-Type"));
	}
}
