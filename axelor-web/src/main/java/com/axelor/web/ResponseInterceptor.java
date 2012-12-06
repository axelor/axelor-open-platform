package com.axelor.web;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import com.axelor.db.JPA;
import com.axelor.rpc.Response;
import com.google.inject.persist.Transactional;

public class ResponseInterceptor implements MethodInterceptor {
	
	private final ThreadLocal<Boolean> txnStarted = new ThreadLocal<Boolean>();
	
	@Override
	public Object invoke(final MethodInvocation invocation) throws Throwable {
		
		final Transactional transactional = invocation.getMethod().getAnnotation(Transactional.class);
		if (transactional != null || txnStarted.get() == Boolean.TRUE) {
			return invocation.proceed();
		}
		
		final EntityManager em = JPA.em();
		final EntityTransaction txn = em.getTransaction();
		
		if (txn.isActive()) {
			return invocation.proceed();
		}
		
		txn.begin();
		txnStarted.set(true);
		
		Response response;
		try {
			response = (Response) invocation.proceed();
			if (txn.isActive() && !txn.getRollbackOnly()) {
				txn.commit();
			}
		} catch (Throwable e) {
			response = new Response();
			response.setException(e);
		} finally {
			if (txn.isActive()) {
				txn.rollback();
			}
			txnStarted.remove();
		}
		return response;
	}
}
