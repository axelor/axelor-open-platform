package com.axelor.web;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.axelor.db.JPA;
import com.axelor.rpc.Response;
import com.google.inject.persist.Transactional;

public class ResponseInterceptor implements MethodInterceptor {
	
	private final Logger log = LoggerFactory.getLogger(ResponseInterceptor.class);
	
	private final ThreadLocal<Boolean> txnStarted = new ThreadLocal<Boolean>();
	
	@Override
	public Object invoke(final MethodInvocation invocation) throws Throwable {
		
		Response response = null;
		Transactional transactional = invocation.getMethod().getAnnotation(Transactional.class);
		if (transactional != null || txnStarted.get() == Boolean.TRUE) {
			log.debug("Transactional: {}", invocation.getMethod());
			try {
				response = (Response) invocation.proceed();
				log.debug("Success");
			} catch (Exception e) {
				response = new Response();
				response.setException(e);
				log.error("Error: {}", e, e);
				if (JPA.em().getTransaction().isActive()) {
					JPA.em().getTransaction().rollback();
				}
			}
		}

		if (response != null) {
			return response;
		}
		
		final EntityManager em = JPA.em();
		final EntityTransaction txn = em.getTransaction();
		
		if (txn.isActive()) {
			log.debug("In transaction: {}", invocation.getMethod());
			try {
				response = (Response) invocation.proceed();
				log.debug("Success");
			} catch (Exception e) {
				response = new Response();
				response.setException(e);
				log.error("Error: {}", e, e);
				if (JPA.em().getTransaction().isActive()) {
					JPA.em().getTransaction().rollback();
				}
			}
		}
		
		if (response != null) {
			return response;
		}
		
		log.debug("Transaction begin: {}", invocation.getMethod());

		txn.begin();
		txnStarted.set(true);

		try {
			response = (Response) invocation.proceed();
			if (txn.isActive() && !txn.getRollbackOnly()) {
				txn.commit();
			}
		} catch (Throwable e) {
			response = new Response();
			response.setException(e);
			log.error("Error: {}", e, e);
		} finally {
			if (txn.isActive()) {
				txn.rollback();
			}
			log.debug("Transaction complete: {}", invocation.getMethod());
			txnStarted.remove();
		}
		return response;
	}
}
