package com.axelor.wkf.db.node.message.socialnetwork;

import javax.persistence.Entity;

import com.axelor.db.JPA;
import com.axelor.db.Query;
import com.axelor.wkf.db.node.message.IntermediateSocialNetworkEvent;

@Entity
public class IntermediateLinkedinEvent extends IntermediateSocialNetworkEvent {

	/**
	 * Find a <code>IntermediateLinkedinEvent</code> by <code>id</code>.
	 *
	 */
	public static IntermediateLinkedinEvent find(Long id) {
		return JPA.find(IntermediateLinkedinEvent.class, id);
	}

	/**
	 * Return a {@link Query} instance for <code>IntermediateLinkedinEvent</code> to filter
	 * on all the records.
	 *
	 */
	public static Query<IntermediateLinkedinEvent> allIntermediateLinkedinEvent() {
		return JPA.all(IntermediateLinkedinEvent.class);
	}
	
	/**
	 * A shortcut method to <code>IntermediateLinkedinEvent.all().filter(...)</code>
	 *
	 */
	public static Query<IntermediateLinkedinEvent> filterIntermediateLinkedinEvent(String filter, Object... params) {
		return JPA.all(IntermediateLinkedinEvent.class).filter(filter, params);
	}
	
}
