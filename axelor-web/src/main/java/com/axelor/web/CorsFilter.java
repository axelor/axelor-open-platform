package com.axelor.web;

import static com.axelor.common.StringUtils.isBlank;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Singleton;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.axelor.app.AppSettings;

/**
 * Simple CORS filter that checks <code>Origin</code> header of the requests
 * with the allowed origin pattern.
 * 
 * <p>
 * If the <code>Origin</code> header matches the configured allowed pattern, it
 * will set other configured CORS headers.
 * </p>
 * 
 */
@Singleton
public class CorsFilter implements Filter {

	private static final String DEFAULT_CORS_ALLOW_ORIGIN = "*";
	private static final String DEFAULT_CORS_ALLOW_CREDENTIALS = "true";
	private static final String DEFAULT_CORS_ALLOW_METHODS = "GET,PUT,POST,DELETE,HEAD,OPTIONS";
	private static final String DEFAULT_CORS_ALLOW_HEADERS = "Origin,Accept,X-Requested-With,Content-Type,Access-Control-Request-Method,Access-Control-Request-Headers";

	private static Pattern corsOriginPattern;

	private static String corsAllowOrigin;
	private static String corsAllowCredentials;
	private static String corsAllowMethods;
	private static String corsAllowHeaders;

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {

		final AppSettings settings = AppSettings.get();

		corsAllowOrigin = settings.get("cors.allow.origin");
		corsAllowCredentials = settings.get("cors.allow.credentials", DEFAULT_CORS_ALLOW_CREDENTIALS);
		corsAllowMethods = settings.get("cors.allow.methods", DEFAULT_CORS_ALLOW_METHODS);
		corsAllowHeaders = settings.get("cors.allow.headers", DEFAULT_CORS_ALLOW_HEADERS);

		if (!isBlank(corsAllowOrigin)) {
			corsOriginPattern = Pattern.compile(corsAllowOrigin);
		}
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {

		final HttpServletRequest req = (HttpServletRequest) request;
		final HttpServletResponse res = (HttpServletResponse) response;
		final String origin = req.getHeader("Origin");

		if (corsOriginPattern == null || isBlank(origin)) {
			chain.doFilter(request, response);
			return;
		}

		final Matcher matcher = corsOriginPattern.matcher(origin);

		if (matcher.matches() || DEFAULT_CORS_ALLOW_ORIGIN.equals(corsAllowOrigin)) {
			res.addHeader("Access-Control-Allow-Origin", origin);
			res.addHeader("Access-Control-Allow-Credentials", corsAllowCredentials);
			res.addHeader("Access-Control-Allow-Methods", corsAllowMethods);
			res.addHeader("Access-Control-Allow-Headers", corsAllowHeaders);
		}

		chain.doFilter(request, response);
	}

	@Override
	public void destroy() {
	}
}
