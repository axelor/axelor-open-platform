package com.axelor.wkf.db.node.message;

import javax.persistence.Entity;

import com.axelor.db.JPA;
import com.axelor.db.Query;
import com.axelor.wkf.db.node.IntermediateMessageEvent;

@Entity
public class IntermediateInternetSiteEvent extends IntermediateMessageEvent {
	
	/**
	 * Find a <code>IntermediateInternetSiteEvent</code> by <code>id</code>.
	 *
	 */
	public static IntermediateInternetSiteEvent find(Long id) {
		return JPA.find(IntermediateInternetSiteEvent.class, id);
	}
	
	/**
	 * Return a {@link Query} instance for <code>IntermediateInternetSiteEvent</code> to filter
	 * on all the records.
	 *
	 */
	public static Query<IntermediateInternetSiteEvent> allIntermediateInternetSiteEvent() {
		return JPA.all(IntermediateInternetSiteEvent.class);
	}
	
	/**
	 * A shortcut method to <code>IntermediateInternetSiteEvent.all().filter(...)</code>
	 *
	 */
	public static Query<IntermediateInternetSiteEvent> filterIntermediateInternetSiteEvent(String filter, Object... params) {
		return JPA.all(IntermediateInternetSiteEvent.class).filter(filter, params);
	}
	
}
