package com.axelor.wkf.db.node;

import javax.persistence.Entity;

import com.axelor.db.JPA;
import com.axelor.db.Query;
import com.axelor.wkf.db.Gateway;

@Entity
public class InclusiveGateway extends Gateway {
	
	/**
	 * Find a <code>InclusiveGateway</code> by <code>id</code>.
	 *
	 */
	public static InclusiveGateway find(Long id) {
		return JPA.find(InclusiveGateway.class, id);
	}
	
	/**
	 * Return a {@link Query} instance for <code>InclusiveGateway</code> to filter
	 * on all the records.
	 *
	 */
	public static Query<InclusiveGateway> allInclusiveGateway() {
		return JPA.all(InclusiveGateway.class);
	}
	
	/**
	 * A shortcut method to <code>InclusiveGateway.all().filter(...)</code>
	 *
	 */
	public static Query<InclusiveGateway> filterInclusiveGateway(String filter, Object... params) {
		return JPA.all(InclusiveGateway.class).filter(filter, params);
	}
	
}
