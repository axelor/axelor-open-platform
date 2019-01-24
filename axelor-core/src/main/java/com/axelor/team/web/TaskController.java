/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2019 Axelor (<http://axelor.com>).
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
package com.axelor.team.web;

import com.axelor.auth.AuthUtils;
import com.axelor.db.JpaSupport;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.axelor.rpc.Response;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import javax.persistence.TypedQuery;

public class TaskController extends JpaSupport {

  private static final String SQL_TASKS_DUE =
      ""
          + "SELECT tt FROM TeamTask tt "
          + "LEFT JOIN tt.assignedTo u "
          + "WHERE "
          + "	(tt.status NOT IN :closed_status AND u.id = :uid) AND "
          + " (tt.taskDeadline <= current_date)";

  private static final String SQL_TASKS_TODO =
      ""
          + "SELECT tt FROM TeamTask tt "
          + "LEFT JOIN tt.assignedTo u "
          + "WHERE "
          + "	(tt.status NOT IN :closed_status AND u.id = :uid) AND "
          + "	(tt.taskDeadline <= current_date OR tt.taskDate <= current_date)";

  public void countTasks(ActionRequest request, ActionResponse response) {
    final Map<String, Object> value = new HashMap<>();
    value.put("pending", countTasks(SQL_TASKS_DUE));
    value.put("current", countTasks(SQL_TASKS_TODO));
    response.setValue("tasks", value);
    response.setStatus(Response.STATUS_SUCCESS);
  }

  private Long countTasks(String queryString) {

    final String countString =
        queryString.replace("SELECT tt FROM TeamTask tt", "SELECT COUNT(tt.id) FROM TeamTask tt");

    final TypedQuery<Long> query = getEntityManager().createQuery(countString, Long.class);

    query.setParameter("uid", AuthUtils.getUser().getId());
    query.setParameter("closed_status", Arrays.asList("closed", "canceled"));
    try {
      return query.getSingleResult();
    } catch (Exception e) {
    }
    return 0L;
  }
}
