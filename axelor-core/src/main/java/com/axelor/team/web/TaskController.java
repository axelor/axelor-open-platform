/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.team.web;

import com.axelor.auth.AuthUtils;
import com.axelor.auth.db.User;
import com.axelor.db.JpaSupport;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.axelor.rpc.Response;
import jakarta.persistence.TypedQuery;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class TaskController extends JpaSupport {

  private static final String SQL_TASKS_DUE =
      """
      SELECT tt FROM TeamTask tt \
      LEFT JOIN tt.assignedTo u \
      WHERE \
      	(tt.status NOT IN :closed_status AND u.id = :uid) AND \
       (tt.taskDeadline <= current_date)""";

  private static final String SQL_TASKS_TODO =
      """
      SELECT tt FROM TeamTask tt \
      LEFT JOIN tt.assignedTo u \
      WHERE \
      	(tt.status NOT IN :closed_status AND u.id = :uid) AND \
      	(tt.taskDeadline <= current_date OR tt.taskDate <= current_date)""";

  public void countTasks(ActionRequest request, ActionResponse response) {
    final Map<String, Object> value = new HashMap<>();
    value.put("pending", countTasks(SQL_TASKS_DUE));
    value.put("current", countTasks(SQL_TASKS_TODO));
    response.setValue("tasks", value);
    response.setStatus(Response.STATUS_SUCCESS);
  }

  private Long countTasks(String queryString) {
    final User user = AuthUtils.getUser();

    if (user == null) {
      return 0L;
    }

    final String countString =
        queryString.replace("SELECT tt FROM TeamTask tt", "SELECT COUNT(tt.id) FROM TeamTask tt");

    final TypedQuery<Long> query = getEntityManager().createQuery(countString, Long.class);

    query.setParameter("uid", user.getId());
    query.setParameter("closed_status", Arrays.asList("closed", "canceled"));
    try {
      return query.getSingleResult();
    } catch (Exception e) {
    }
    return 0L;
  }
}
