package com.axelor.auth.db;

import java.io.Serializable;

import org.hibernate.EmptyInterceptor;
import org.hibernate.Transaction;
import org.hibernate.type.Type;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDateTime;

import com.axelor.auth.AuthUtils;

@SuppressWarnings("serial")
public class AuditInterceptor extends EmptyInterceptor {
	
	private ThreadLocal<User> currentUser = new ThreadLocal<User>();
	
	@Override
	public void afterTransactionBegin(Transaction tx) {
		currentUser.set(AuthUtils.getUser());
	}

	@Override
	public void afterTransactionCompletion(Transaction tx) {
		currentUser.remove();
	}

	@Override
	public boolean onFlushDirty(Object entity, Serializable id,
			Object[] currentState, Object[] previousState,
			String[] propertyNames, Type[] types) {
		if (!(entity instanceof AuditableModel)) {
			return false;
		}
		for (int i = 0; i < propertyNames.length; i++) {
			if ("updatedOn".equals(propertyNames[i])) {
				currentState[i] = new LocalDateTime(DateTimeZone.UTC);
			}
			if ("updatedBy".equals(propertyNames[i])) {
				currentState[i] = currentUser.get();
			}
		}
		return true;
	}
	
	@Override
	public boolean onSave(Object entity, Serializable id, Object[] state,
			String[] propertyNames, Type[] types) {
		if (!(entity instanceof AuditableModel)) {
			return false;
		}
		for (int i = 0; i < propertyNames.length; i++) {
			if ("createdOn".equals(propertyNames[i])) {
				state[i] = new LocalDateTime(DateTimeZone.UTC);
			}
			if ("createdBy".equals(propertyNames[i])) {
				state[i] = currentUser.get();
			}
		}
		return true;
	}
}