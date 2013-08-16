package com.axelor.wkf.db.node.gateway;

import java.util.Map;

import javax.persistence.Entity;

import com.axelor.db.JPA;
import com.axelor.db.Query;
import com.axelor.meta.ActionHandler;
import com.axelor.wkf.db.Instance;
import com.axelor.wkf.db.Transition;
import com.axelor.wkf.db.node.Gateway;

@Entity
public class ExclusiveGateway extends Gateway {
	
	/**
	 * Find a <code>ExclusiveGateway</code> by <code>id</code>.
	 *
	 */
	public static ExclusiveGateway find(Long id) {
		return JPA.find(ExclusiveGateway.class, id);
	}
	
	/**
	 * Return a {@link Query} instance for <code>ExclusiveGateway</code> to filter
	 * on all the records.
	 *
	 */
	public static Query<ExclusiveGateway> allExclusiveGateway() {
		return JPA.all(ExclusiveGateway.class);
	}
	
	/**
	 * A shortcut method to <code>ExclusiveGateway.all().filter(...)</code>
	 *
	 */
	public static Query<ExclusiveGateway> filterExclusiveGateway(String filter, Object... params) {
		return JPA.all(ExclusiveGateway.class).filter(filter, params);
	}
	
	@Override
	public void execute( ActionHandler actionHandler, Instance instance, Map<Object, Object> context ){
		
		for ( Transition transition : getEndTransitions() ){

			if ( transition.execute( actionHandler ) ) {

				transition.getNextNode().execute( actionHandler, instance, transition, context );
				return ;
				
			}

		}
	}
	
}
