package com.axelor.wkf.db.node.message;

import javax.persistence.Entity;

import com.axelor.db.JPA;
import com.axelor.db.Query;
import com.axelor.wkf.db.node.IntermediateMessageEvent;

@Entity
public class IntermediateEmailEvent extends IntermediateMessageEvent {
	
	/**
	 * Find a <code>IntermediateEmailEvent</code> by <code>id</code>.
	 *
	 */
	public static IntermediateEmailEvent find(Long id) {
		return JPA.find(IntermediateEmailEvent.class, id);
	}
	
	/**
	 * Return a {@link Query} instance for <code>IntermediateEmailEvent</code> to filter
	 * on all the records.
	 *
	 */
	public static Query<IntermediateEmailEvent> allIntermediateEmailEvent() {
		return JPA.all(IntermediateEmailEvent.class);
	}
	
	/**
	 * A shortcut method to <code>IntermediateEmailEvent.all().filter(...)</code>
	 *
	 */
	public static Query<IntermediateEmailEvent> filterIntermediateEmailEvent(String filter, Object... params) {
		return JPA.all(IntermediateEmailEvent.class).filter(filter, params);
	}
	
}
