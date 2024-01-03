/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2024 Axelor (<http://axelor.com>).
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.axelor.rpc;

import com.axelor.auth.AuthUtils;
import com.axelor.db.EntityHelper;
import com.axelor.db.Model;
import com.google.common.base.Throwables;
import java.util.HashMap;
import java.util.Map;
import javax.persistence.OptimisticLockException;
import javax.validation.ConstraintViolationException;
import org.hibernate.StaleObjectStateException;

public class ResponseException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  private String title;

  public ResponseException(String message) {
    super(message);
  }

  public ResponseException(String message, Throwable cause) {
    super(message, cause);
  }

  public ResponseException(String message, String title, Throwable cause) {
    super(message, cause);
    this.title = title;
  }

  public ResponseException(Throwable cause) {
    super(cause.getMessage(), cause);
  }

  public Map<String, Object> toReport() {
    Map<String, Object> report = new HashMap<>();
    Throwable cause = getCause();
    String message = getMessage();

    if (message == null) message = cause.getMessage();
    if (message != null) report.put("message", this.getMessage());
    if (title != null) report.put("title", title);

    if (cause != null
        && (AuthUtils.isAdmin(AuthUtils.getUser())
            || AuthUtils.isTechnicalStaff(AuthUtils.getUser()))) {
      report.put("causeClass", cause.getClass().getName());
      report.put("causeStack", Throwables.getStackTraceAsString(cause));
      report.put("causeString", cause.toString());
    }

    if (cause instanceof OptimisticLockException) {
      OptimisticLockException ex = (OptimisticLockException) cause;
      Object entity = ex.getEntity();
      if (entity instanceof Model) {
        report.put("entityId", ((Model) entity).getId());
        report.put("entityName", EntityHelper.getEntityClass(entity).getName());
      } else if (ex.getCause() instanceof StaleObjectStateException) {
        StaleObjectStateException sx = (StaleObjectStateException) ex.getCause();
        report.put("entityId", sx.getIdentifier());
        report.put("entityName", sx.getEntityName());
      }
    }

    if (cause instanceof ConstraintViolationException) {
      ConstraintViolationException ex = (ConstraintViolationException) cause;
      Map<String, Object> errors = new HashMap<>();
      ex.getConstraintViolations()
          .forEach(error -> errors.put(error.getPropertyPath().toString(), error.getMessage()));
      report.put("constraints", errors);
    }

    return report;
  }
}
