package com.axelor.wkf.db.node.message;

import javax.persistence.Entity;

import com.axelor.db.JPA;
import com.axelor.db.Query;
import com.axelor.wkf.db.node.IntermediateMessageEvent;

@Entity
public class IntermediateSmsEvent extends IntermediateMessageEvent {
	
	/**
	 * Find a <code>IntermediateSmsEvent</code> by <code>id</code>.
	 *
	 */
	public static IntermediateSmsEvent find(Long id) {
		return JPA.find(IntermediateSmsEvent.class, id);
	}
	
	/**
	 * Return a {@link Query} instance for <code>IntermediateSmsEvent</code> to filter
	 * on all the records.
	 *
	 */
	public static Query<IntermediateSmsEvent> allIntermediateSmsEvent() {
		return JPA.all(IntermediateSmsEvent.class);
	}
	
	/**
	 * A shortcut method to <code>IntermediateSmsEvent.all().filter(...)</code>
	 *
	 */
	public static Query<IntermediateSmsEvent> filterIntermediateSmsEvent(String filter, Object... params) {
		return JPA.all(IntermediateSmsEvent.class).filter(filter, params);
	}
	
}
