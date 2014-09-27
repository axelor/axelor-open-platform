/**
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2014 Axelor (<http://axelor.com>).
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
package com.axelor.wkf.db.repo;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.axelor.auth.AuthUtils;
import com.axelor.auth.db.Role;
import com.axelor.auth.db.User;
import com.axelor.i18n.I18n;
import com.axelor.meta.ActionHandler;
import com.axelor.meta.db.MetaAction;
import com.axelor.wkf.db.Transition;

public class TransitionRepository extends AbstractTransitionRepository {

	protected Logger logger = LoggerFactory.getLogger(getClass());

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public boolean execute(Transition transition, User user, ActionHandler handler,  Map<Object, Object> context){

		logger.debug("Execute transition ::: {}", transition.getName());

		final String signal = transition.getSignal();
		if ( signal != null && (
				!handler.getContext().containsKey("_signal") ||
				!handler.getContext().get("_signal").equals(signal))) {
			logger.debug("Signal ::: {}", signal);
			return false;
		}

		final Role role = transition.getRole();
		if (role != null) {
			if (user == null) {
				return false;
			}
			if (!AuthUtils.hasRole(user, role.getName())) {
				logger.debug( "Role ::: {}", role.getName() );
				context.put("flash", I18n.get("You have no sufficient rights."));
				return false;
			}
		}

		final MetaAction condition = transition.getCondition();
		if ( condition != null ) {
			logger.debug( "Condition ::: {}", condition.getName() );
			handler.getRequest().setAction(condition.getName() );
			for ( Object data : (List) handler.execute().getData()) {
				if ( data instanceof Boolean ) {
					return (Boolean) data;
				}
				if (data instanceof Map &&
						((Map) data).containsKey("errors") &&
						((Map) data).get("errors") != null &&
						!( (Map) ((Map) data).get("errors") ).isEmpty() ) {
					logger.debug( "Context with Errors ::: {}", data );
					context.putAll( (Map) data );
					return false;
				}
			}
		}
		return true;
	}
}
