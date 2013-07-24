package com.axelor.wkf.db.node;

import javax.persistence.Entity;

import com.axelor.db.JPA;
import com.axelor.db.Query;
import com.axelor.wkf.db.Node;

@Entity
public class EndEvent extends Node {
	
	/**
	 * Find a <code>EndEvent</code> by <code>id</code>.
	 *
	 */
	public static EndEvent find(Long id) {
		return JPA.find(EndEvent.class, id);
	}
	
	/**
	 * Return a {@link Query} instance for <code>EndEvent</code> to filter
	 * on all the records.
	 *
	 */
	public static Query<EndEvent> allEndEvent() {
		return JPA.all(EndEvent.class);
	}
	
	/**
	 * A shortcut method to <code>EndEvent.all().filter(...)</code>
	 *
	 */
	public static Query<EndEvent> filterEndEvent(String filter, Object... params) {
		return JPA.all(EndEvent.class).filter(filter, params);
	}
	
}
