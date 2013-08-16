package com.axelor.wkf.db.node.message.socialnetwork;

import javax.persistence.Entity;

import com.axelor.db.JPA;
import com.axelor.db.Query;
import com.axelor.wkf.db.node.message.IntermediateSocialNetworkEvent;

@Entity
public class IntermediateTwitterEvent extends IntermediateSocialNetworkEvent {

	/**
	 * Find a <code>IntermediateTwitterEvent</code> by <code>id</code>.
	 *
	 */
	public static IntermediateTwitterEvent find(Long id) {
		return JPA.find(IntermediateTwitterEvent.class, id);
	}

	/**
	 * Return a {@link Query} instance for <code>IntermediateTwitterEvent</code> to filter
	 * on all the records.
	 *
	 */
	public static Query<IntermediateTwitterEvent> allIntermediateTwitterEvent() {
		return JPA.all(IntermediateTwitterEvent.class);
	}
	
	/**
	 * A shortcut method to <code>IntermediateTwitterEvent.all().filter(...)</code>
	 *
	 */
	public static Query<IntermediateTwitterEvent> filterIntermediateTwitterEvent(String filter, Object... params) {
		return JPA.all(IntermediateTwitterEvent.class).filter(filter, params);
	}
	
}
