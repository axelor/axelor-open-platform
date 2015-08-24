/**
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2015 Axelor (<http://axelor.com>).
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

import com.axelor.meta.ActionHandler;
import com.axelor.meta.db.MetaAction;
import com.axelor.wkf.db.Workflow;

public class WorkflowRepository extends AbstractWorkflowRepository {

	@SuppressWarnings("rawtypes")
	public boolean isRunnable(Workflow workflow, ActionHandler handler) {

		final MetaAction condition = workflow.getCondition();
		if (condition == null) {
			return true;
		}

		handler.getRequest().setAction(condition.getName());
		for (Object data : (List<?>) handler.execute().getData()) {
			if (((Map) data).containsKey("errors")
					&& ((Map) data).get("errors") != null
					&& !((Map) ((Map) data).get("errors")).isEmpty()) {
				return false;
			}
		}
		return true;
	}

	@Override
	public Map<String, Object> populate(Map<String, Object> json, Map<String, Object> context) {
		Long id = (Long) json.get("id");
		if (id == null) {
			return json;
		}
		Workflow entity = find(id);
		if (entity == null || entity.getMetaModel() == null) {
			return json;
		}
		json.put("metaModelName", entity.getMetaModel().getFullName());
		return json;
	}
}
