package com.axelor.wkf.db.node.message;

import javax.persistence.Entity;

import com.axelor.db.JPA;
import com.axelor.db.Query;
import com.axelor.wkf.db.node.IntermediateMessageEvent;

@Entity
public class IntermediateMailEvent extends IntermediateMessageEvent {
	
	/**
	 * Find a <code>IntermediateMailEvent</code> by <code>id</code>.
	 *
	 */
	public static IntermediateMailEvent find(Long id) {
		return JPA.find(IntermediateMailEvent.class, id);
	}
	
	/**
	 * Return a {@link Query} instance for <code>IntermediateMailEvent</code> to filter
	 * on all the records.
	 *
	 */
	public static Query<IntermediateMailEvent> allIntermediateMailEvent() {
		return JPA.all(IntermediateMailEvent.class);
	}
	
	/**
	 * A shortcut method to <code>IntermediateMailEvent.all().filter(...)</code>
	 *
	 */
	public static Query<IntermediateMailEvent> filterIntermediateMailEvent(String filter, Object... params) {
		return JPA.all(IntermediateMailEvent.class).filter(filter, params);
	}
	
}
