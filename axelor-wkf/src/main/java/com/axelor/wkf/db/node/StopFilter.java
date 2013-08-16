package com.axelor.wkf.db.node;

import javax.persistence.Entity;

import com.axelor.db.JPA;
import com.axelor.db.Query;
import com.axelor.wkf.db.Node;

@Entity
public class StopFilter extends Node {

	/**
	 * Find a <code>StopFilter</code> by <code>id</code>.
	 *
	 */
	public static StopFilter find(Long id) {
		return JPA.find(StopFilter.class, id);
	}
	
	/**
	 * Return a {@link Query} instance for <code>StopFilter</code> to StopFilter
	 * on all the records.
	 *
	 */
	public static Query<StopFilter> allStopFilter() {
		return JPA.all(StopFilter.class);
	}
	
	/**
	 * A shortcut method to <code>StopFilter.all().StopFilter(...)</code>
	 *
	 */
	public static Query<StopFilter> StopFilterStopFilter(String StopFilter, Object... params) {
		return JPA.all(StopFilter.class).filter(StopFilter, params);
	}

}
