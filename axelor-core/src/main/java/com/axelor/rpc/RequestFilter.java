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
package com.axelor.rpc;

import java.io.IOException;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.ext.Provider;
import javax.ws.rs.ext.WriterInterceptor;
import javax.ws.rs.ext.WriterInterceptorContext;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

/**
 * Keep track of current {@link Request} object.
 *
 * <p>
 * The current {@link Request} is stored as thread local as soon as it's
 * available and cleared when the web service finishes writing response.
 * </p>
 */
@Provider
public class RequestFilter implements MethodInterceptor, WriterInterceptor {

	private Request getRequest(MethodInvocation invocation) {
		final Object[] args = invocation.getArguments();
		for (Object arg : args) {
			if (arg instanceof Request) {
				return (Request) arg;
			}
		}
		return null;
	}

	@Override
	public Object invoke(MethodInvocation invocation) throws Throwable {

		Request request = getRequest(invocation);
		if (request != null) {
			Request.CURRENT.set(request);
		}

		return invocation.proceed();
	}

	@Override
	public void aroundWriteTo(WriterInterceptorContext context)
			throws IOException, WebApplicationException {
		try {
			context.proceed();
		} finally {
			Request.CURRENT.remove();
		}
	}
}
