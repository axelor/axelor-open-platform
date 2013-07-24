package com.axelor.wkf.db.node;

import javax.persistence.Entity;

import com.axelor.db.JPA;
import com.axelor.db.Query;
import com.axelor.meta.ActionHandler;
import com.axelor.wkf.db.Instance;
import com.axelor.wkf.db.Transition;
import com.google.common.base.Preconditions;

@Entity
public class ParallelGateway extends Gateway {
	
	/**
	 * Find a <code>ParallelGateway</code> by <code>id</code>.
	 *
	 */
	public static ParallelGateway find(Long id) {
		return JPA.find(ParallelGateway.class, id);
	}
	
	/**
	 * Return a {@link Query} instance for <code>ParallelGateway</code> to filter
	 * on all the records.
	 *
	 */
	public static Query<ParallelGateway> allParallelGateway() {
		return JPA.all(ParallelGateway.class);
	}
	
	/**
	 * A shortcut method to <code>ParallelGateway.all().filter(...)</code>
	 *
	 */
	public static Query<ParallelGateway> filterParallelGateway(String filter, Object... params) {
		return JPA.all( ParallelGateway.class ).filter(filter, params);
	}

	@Override
	public Object execute( ActionHandler actionHandler, Object... params ) {
		
		Instance instance = null; Transition transition = null;
		
		for (Object param : params) {
			
			if ( param instanceof Instance ){ instance = (Instance) param; }
			if ( param instanceof Transition ){ transition = (Transition) param; }
			
		}
		
		Preconditions.checkNotNull(instance); Preconditions.checkNotNull(transition);
		
		if ( instance.getExecutedTransitions().containsAll( getStartTransitions() ) ) {
			instance.removeAllExecutedTransition( getStartTransitions() );
			return true ;
		}
		
		return false;
		
	}
	
}
