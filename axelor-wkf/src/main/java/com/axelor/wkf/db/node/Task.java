package com.axelor.wkf.db.node;

import javax.persistence.Entity;

import com.axelor.db.JPA;
import com.axelor.db.Query;
import com.axelor.meta.ActionHandler;
import com.axelor.wkf.db.Node;
import com.google.common.collect.Lists;

@Entity
public class Task extends Node {
	
	/**
	 * Find a <code>Task</code> by <code>id</code>.
	 *
	 */
	public static Task find(Long id) {
		return JPA.find(Task.class, id);
	}
	
	/**
	 * Return a {@link Query} instance for <code>Task</code> to filter
	 * on all the records.
	 *
	 */
	public static Query<Task> allTask() {
		return JPA.all(Task.class);
	}
	
	/**
	 * A shortcut method to <code>Task.all().filter(...)</code>
	 *
	 */
	public static Query<Task> filterTask(String filter, Object... params) {
		return JPA.all(Task.class).filter(filter, params);
	}
	
	@Override
	public Object execute( ActionHandler actionHandler, Object...params ) {
		
		if ( getAction() != null ) { 
			
			actionHandler.getRequest().setAction( getAction().getName() );
			return actionHandler.execute().getData() ;
		
		}
		
		return Lists.newArrayList();
		
	}

}
