package com.axelor.wkf.db.node;

import javax.persistence.Entity;

import com.axelor.db.JPA;
import com.axelor.db.Query;
import com.axelor.wkf.db.Node;

@Entity
public class Filter extends Node {
	
	private String filter;
	
	public String getFilter() {
		return filter;
	}

	public void setFilter(String filter) {
		this.filter = filter;
	}

	/**
	 * Find a <code>Filter</code> by <code>id</code>.
	 *
	 */
	public static Filter find(Long id) {
		return JPA.find(Filter.class, id);
	}
	
	/**
	 * Return a {@link Query} instance for <code>Filter</code> to filter
	 * on all the records.
	 *
	 */
	public static Query<Filter> allFilter() {
		return JPA.all(Filter.class);
	}
	
	/**
	 * A shortcut method to <code>Filter.all().filter(...)</code>
	 *
	 */
	public static Query<Filter> filterFilter(String filter, Object... params) {
		return JPA.all(Filter.class).filter(filter, params);
	}

}
