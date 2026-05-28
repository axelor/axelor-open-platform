/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.rpc;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.ext.Provider;
import jakarta.ws.rs.ext.WriterInterceptor;
import jakarta.ws.rs.ext.WriterInterceptorContext;
import java.io.IOException;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

/**
 * Keep track of current {@link Request} object.
 *
 * <p>The current {@link Request} is stored as thread local as soon as it's available and cleared
 * when the web service finishes writing response.
 */
@Provider
public class RequestFilter implements MethodInterceptor, WriterInterceptor {

  private Request getRequest(MethodInvocation invocation) {
    final Object[] args = invocation.getArguments();
    for (Object arg : args) {
      if (arg instanceof Request request) {
        return request;
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
