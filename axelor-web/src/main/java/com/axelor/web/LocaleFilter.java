package com.axelor.web;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import com.axelor.meta.service.MetaTranslations;
import com.google.inject.Singleton;

@Singleton
public class LocaleFilter implements Filter {

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response,
			FilterChain chain) throws IOException, ServletException {
		
		MetaTranslations.language.set(request.getLocale());
		try {
			chain.doFilter(request, response);
		} finally {
			MetaTranslations.language.remove();
		}
	}

	@Override
	public void destroy() {
	}
}
