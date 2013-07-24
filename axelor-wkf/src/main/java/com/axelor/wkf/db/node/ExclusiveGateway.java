package com.axelor.wkf.db.node;

import javax.persistence.Entity;

import com.axelor.db.JPA;
import com.axelor.db.Query;

@Entity
public class ExclusiveGateway extends Gateway {
	
	/**
	 * Find a <code>ExclusiveGateway</code> by <code>id</code>.
	 *
	 */
	public static ExclusiveGateway find(Long id) {
		return JPA.find(ExclusiveGateway.class, id);
	}
	
	/**
	 * Return a {@link Query} instance for <code>ExclusiveGateway</code> to filter
	 * on all the records.
	 *
	 */
	public static Query<ExclusiveGateway> allExclusiveGateway() {
		return JPA.all(ExclusiveGateway.class);
	}
	
	/**
	 * A shortcut method to <code>ExclusiveGateway.all().filter(...)</code>
	 *
	 */
	public static Query<ExclusiveGateway> filterExclusiveGateway(String filter, Object... params) {
		return JPA.all(ExclusiveGateway.class).filter(filter, params);
	}
	
}
