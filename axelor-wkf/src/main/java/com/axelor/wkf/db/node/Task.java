package com.axelor.wkf.db.node;

import java.util.Map;

import javax.persistence.Entity;

import com.axelor.db.JPA;
import com.axelor.db.Query;
import com.axelor.meta.ActionHandler;
import com.axelor.wkf.db.Instance;
import com.axelor.wkf.db.Node;
import com.axelor.wkf.db.Transition;

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
	public void execute( ActionHandler actionHandler, Instance instance, Transition transition, Map<Object, Object> context ) { 

		logger.debug("Execute node ::: {}", getName() );
		
		testMaxPassedNode( instance );
		historize( instance, transition );
		
		if ( getAction() != null ) { 

			logger.debug( "Action ::: {}", getAction().getName() );
			actionHandler.getRequest().setAction( getAction().getName() );
			updateContext( context, actionHandler.execute().getData() );
			
		}
		
		execute( actionHandler, instance, context );
		
	}

}
