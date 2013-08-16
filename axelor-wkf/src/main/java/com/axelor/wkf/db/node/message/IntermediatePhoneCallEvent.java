package com.axelor.wkf.db.node.message;

import javax.persistence.Entity;

import com.axelor.db.JPA;
import com.axelor.db.Query;
import com.axelor.wkf.db.node.IntermediateMessageEvent;

@Entity
public class IntermediatePhoneCallEvent extends IntermediateMessageEvent {
	
	/**
	 * Find a <code>IntermediatePhoneCallEvent</code> by <code>id</code>.
	 *
	 */
	public static IntermediatePhoneCallEvent find(Long id) {
		return JPA.find(IntermediatePhoneCallEvent.class, id);
	}
	
	/**
	 * Return a {@link Query} instance for <code>IntermediatePhoneCallEvent</code> to filter
	 * on all the records.
	 *
	 */
	public static Query<IntermediatePhoneCallEvent> allIntermediatePhoneCallEvent() {
		return JPA.all(IntermediatePhoneCallEvent.class);
	}
	
	/**
	 * A shortcut method to <code>IntermediatePhoneCallEvent.all().filter(...)</code>
	 *
	 */
	public static Query<IntermediatePhoneCallEvent> filterIntermediatePhoneCallEvent(String filter, Object... params) {
		return JPA.all(IntermediatePhoneCallEvent.class).filter(filter, params);
	}
	
}
