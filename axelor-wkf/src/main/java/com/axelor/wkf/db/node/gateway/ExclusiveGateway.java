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
	public void execute( ActionHandler actionHandler, User user, Instance instance, Map<Object, Object> context ){
		
		for ( Transition transition : getEndTransitions() ){

			if ( transition.execute( actionHandler, context, user ) ) {

				transition.getNextNode().execute( actionHandler, user, instance, transition, context );
				return ;
				
			}

		}
	}
	
}
