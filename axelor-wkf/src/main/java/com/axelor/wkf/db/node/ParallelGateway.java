package com.axelor.wkf.db.node;

import java.util.Map;

import javax.persistence.Entity;

import com.axelor.db.JPA;
import com.axelor.db.Query;
import com.axelor.meta.ActionHandler;
import com.axelor.wkf.db.Gateway;
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
	public void execute( ActionHandler actionHandler, Instance instance, Transition transition, Map<Object, Object> context ) {

		Preconditions.checkNotNull(instance); Preconditions.checkNotNull(transition);

		logger.debug("Execute node ::: {}", getName() );
		
		instance.addExecutedTransition(transition);
		
		if ( instance.getExecutedTransitions().containsAll( getStartTransitions() ) ) {
			
			logger.debug("Parallel OK !" );
			counterAdd(instance);
			historize(instance, transition);
			instance.removeAllExecutedTransition( getStartTransitions() );
			execute(actionHandler, instance, context);
			
			
		}
		else { instance.removeNode( transition.getStartNode() ); }
		
	}
	
}
