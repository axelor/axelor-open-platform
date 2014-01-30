/**
 * Copyright (c) 2012-2014 Axelor. All Rights Reserved.
 *
 * The contents of this file are subject to the Common Public
 * Attribution License Version 1.0 (the “License”); you may not use
 * this file except in compliance with the License. You may obtain a
 * copy of the License at:
 *
 * http://license.axelor.com/.
 *
 * The License is based on the Mozilla Public License Version 1.1 but
 * Sections 14 and 15 have been added to cover use of software over a
 * computer network and provide for limited attribution for the
 * Original Developer. In addition, Exhibit A has been modified to be
 * consistent with Exhibit B.
 *
 * Software distributed under the License is distributed on an “AS IS”
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 *
 * The Original Code is part of "Axelor Business Suite", developed by
 * Axelor exclusively.
 *
 * The Original Developer is the Initial Developer. The Initial Developer of
 * the Original Code is Axelor.
 *
 * All portions of the code written by Axelor are
 * Copyright (c) 2012-2014 Axelor. All Rights Reserved.
 */
package com.axelor.web;

import javax.persistence.EntityTransaction;
import javax.persistence.PersistenceException;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.shiro.authz.AuthorizationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.axelor.db.JPA;
import com.axelor.rpc.Response;

public class ResponseInterceptor implements MethodInterceptor {
	
	private final Logger log = LoggerFactory.getLogger(ResponseInterceptor.class);
	
	private final ThreadLocal<Boolean> running = new ThreadLocal<Boolean>();
	
	@Override
	public Object invoke(final MethodInvocation invocation) throws Throwable {
		
		if (running.get() == Boolean.TRUE) {
			return invocation.proceed();
		}

		log.trace("Web Service: {}", invocation.getMethod());

		Response response = null;
		
		running.set(true);
		try {
			response = (Response) invocation.proceed();
		} catch (Exception e) {
			EntityTransaction txn = JPA.em().getTransaction();
			if (txn.isActive()) {
				txn.rollback();
			} else if (e instanceof PersistenceException) {
				// recover the transaction
				try {
					txn.begin();
				} catch(Exception ex){}
			}
			response = new Response();
			if (e instanceof AuthorizationException) {
				if (!e.toString().contains("not authorized to read")) {
					response.setException(e);
				}
			} else {
				response.setException(e);
			}
			log.error("Error: {}", e, e);
		} finally {
			running.remove();
		}
		return response;
	}
}
