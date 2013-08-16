package com.axelor.wkf.db.node.message.socialnetwork;

import javax.persistence.Entity;

import com.axelor.db.JPA;
import com.axelor.db.Query;
import com.axelor.wkf.db.node.message.IntermediateSocialNetworkEvent;

@Entity
public class IntermediateFacebookEvent extends IntermediateSocialNetworkEvent {

	private String targetPage;
	
	public String getTargetPage() {
		return targetPage;
	}

	public void setTargetPage(String targetPage) {
		this.targetPage = targetPage;
	}

	/**
	 * Find a <code>IntermediateFacebookEvent</code> by <code>id</code>.
	 *
	 */
	public static IntermediateFacebookEvent find(Long id) {
		return JPA.find(IntermediateFacebookEvent.class, id);
	}

	/**
	 * Return a {@link Query} instance for <code>IntermediateFacebookEvent</code> to filter
	 * on all the records.
	 *
	 */
	public static Query<IntermediateFacebookEvent> allIntermediateFacebookEvent() {
		return JPA.all(IntermediateFacebookEvent.class);
	}
	
	/**
	 * A shortcut method to <code>IntermediateFacebookEvent.all().filter(...)</code>
	 *
	 */
	public static Query<IntermediateFacebookEvent> filterIntermediateFacebookEvent(String filter, Object... params) {
		return JPA.all(IntermediateFacebookEvent.class).filter(filter, params);
	}
	
}
