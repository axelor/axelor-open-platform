package com.axelor.auth.db;

import java.io.Serializable;

import javax.persistence.FlushModeType;
import javax.persistence.TypedQuery;

import org.hibernate.EmptyInterceptor;
import org.hibernate.Transaction;
import org.hibernate.type.Type;
import org.joda.time.LocalDateTime;

import com.axelor.auth.AuthUtils;
import com.axelor.db.JPA;

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
	
	private User getUser() {
		User user = currentUser.get();
		if (user == null || JPA.em().contains(user)) {
			return user;
		}
		
		TypedQuery<User> q = JPA.em().createQuery(
				"SELECT self FROM User self WHERE self.code = :code", User.class);

		q.setFlushMode(FlushModeType.COMMIT);
		q.setParameter("code", user.getCode());
		
		user = q.getSingleResult();
		
		try {
			user = q.getSingleResult();
		} catch (Exception e){
			return null;
		}
		
		currentUser.remove();
		currentUser.set(user);

		return user;
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
				currentState[i] = new LocalDateTime();
			}
			if ("updatedBy".equals(propertyNames[i])) {
				currentState[i] = this.getUser();
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
				state[i] = new LocalDateTime();
			}
			if ("createdBy".equals(propertyNames[i])) {
				state[i] = this.getUser();
			}
		}
		return true;
	}
}