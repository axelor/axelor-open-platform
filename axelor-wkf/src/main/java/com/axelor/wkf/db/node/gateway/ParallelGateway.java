/**
 * Axelor Business Solutions
 *
 * Copyright (C) 2012-2014 Axelor (<http://axelor.com>).
 *
 * This program is free software: you can redistribute it and/or  modify
 * it under the terms of the GNU Affero General Public License, version 3,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.axelor.wkf.db.node.gateway;

import java.util.Map;

import javax.persistence.Entity;

import com.axelor.auth.db.User;
import com.axelor.db.JPA;
import com.axelor.db.Query;
import com.axelor.meta.ActionHandler;
import com.axelor.wkf.db.Instance;
import com.axelor.wkf.db.Transition;
import com.axelor.wkf.db.node.Gateway;
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
	public static Query<? extends ParallelGateway> all() {
		return JPA.all(ParallelGateway.class);
	}
	
	/**
	 * A shortcut method to <code>ParallelGateway.all().filter(...)</code>
	 *
	 */
	public static Query<? extends ParallelGateway> filter(String filter, Object... params) {
		return all().filter(filter, params);
	}

	@Override
	public void execute( ActionHandler actionHandler, User user, Instance instance, Transition transition, Map<Object, Object> context ) {

		Preconditions.checkNotNull(instance); Preconditions.checkNotNull(transition);

		logger.debug("Execute node ::: {}", getName() );
		
		instance.addExecutedTransition(transition);
		
		if ( instance.getExecutedTransitions().containsAll( getStartTransitions() ) ) {
			
			logger.debug("Parallel OK !" );
			counterAdd(instance);
			historize(instance, transition);
			instance.removeAllExecutedTransition( getStartTransitions() );
			execute(actionHandler, user, instance, context);
			
			
		}
		else { instance.removeNode( transition.getStartNode() ); }
		
	}
	
}
