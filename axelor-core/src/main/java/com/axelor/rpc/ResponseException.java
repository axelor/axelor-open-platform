/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.rpc;

import com.axelor.auth.AuthUtils;
import com.axelor.auth.db.User;
import com.axelor.db.EntityHelper;
import com.axelor.db.Model;
import com.google.common.base.Throwables;
import jakarta.persistence.OptimisticLockException;
import jakarta.validation.ConstraintViolationException;
import java.util.HashMap;
import java.util.Map;
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

    if (message == null && cause != null) {
      message = cause.getMessage();
    }
    if (message != null) {
      report.put("message", this.getMessage());
    }
    if (title != null) {
      report.put("title", title);
    }

    User user = AuthUtils.getUser();

    if (cause != null
        && user != null
        && (AuthUtils.isAdmin(user) || AuthUtils.isTechnicalStaff(user))) {
      report.put("causeClass", cause.getClass().getName());
      report.put("causeStack", Throwables.getStackTraceAsString(cause));
      report.put("causeString", cause.toString());
    }

    if (cause instanceof OptimisticLockException ex) {
      Object entity = ex.getEntity();
      if (entity instanceof Model model) {
        report.put("entityId", model.getId());
        report.put("entityName", EntityHelper.getEntityClass(entity).getName());
      } else if (ex.getCause() instanceof StaleObjectStateException sx) {
        report.put("entityId", sx.getIdentifier());
        report.put("entityName", sx.getEntityName());
      }
    }

    if (cause instanceof ConstraintViolationException ex) {
      Map<String, Object> errors = new HashMap<>();
      ex.getConstraintViolations()
          .forEach(error -> errors.put(error.getPropertyPath().toString(), error.getMessage()));
      report.put("constraints", errors);
    }

    return report;
  }
}
