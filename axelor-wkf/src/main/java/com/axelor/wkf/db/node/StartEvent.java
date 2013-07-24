package com.axelor.wkf.db.node;

import javax.persistence.Entity;

import com.axelor.db.JPA;
import com.axelor.db.Query;
import com.axelor.wkf.db.Node;

@Entity
public class StartEvent extends Node {
	
	/**
	 * Find a <code>StartEvent</code> by <code>id</code>.
	 *
	 */
	public static StartEvent find(Long id) {
		return JPA.find(StartEvent.class, id);
	}
	
	/**
	 * Return a {@link Query} instance for <code>StartEvent</code> to filter
	 * on all the records.
	 *
	 */
	public static Query<StartEvent> allStartEvent() {
		return JPA.all(StartEvent.class);
	}
	
	/**
	 * A shortcut method to <code>StartEvent.all().filter(...)</code>
	 *
	 */
	public static Query<StartEvent> filterStartEvent(String filter, Object... params) {
		return JPA.all(StartEvent.class).filter(filter, params);
	}
	
}
