/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2018 Axelor (<http://axelor.com>).
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
package com.axelor.rpc;

import com.axelor.auth.AuthSecurityException;
import com.axelor.auth.AuthUtils;
import com.axelor.common.crypto.EncryptorException;
import com.axelor.db.JpaSecurity.AccessType;
import com.axelor.db.JpaSupport;
import com.axelor.db.Model;
import com.axelor.i18n.I18n;
import com.google.common.base.Throwables;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import javax.crypto.BadPaddingException;
import javax.persistence.EntityTransaction;
import javax.persistence.OptimisticLockException;
import javax.persistence.PersistenceException;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.shiro.authz.AuthorizationException;
import org.postgresql.util.PSQLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResponseInterceptor extends JpaSupport implements MethodInterceptor {

  private final Logger log = LoggerFactory.getLogger(ResponseInterceptor.class);

  private final ThreadLocal<Boolean> running = new ThreadLocal<Boolean>();

  @Override
  public Object invoke(final MethodInvocation invocation) throws Throwable {

    if (running.get() == Boolean.TRUE) {
      return invocation.proceed();
    }

    log.trace("Web Service: {}", invocation.getMethod());

    Response response = null;

    running.set(true);
    try {
      response = (Response) invocation.proceed();
    } catch (Exception e) {
      final EntityTransaction txn = getEntityManager().getTransaction();
      if (txn.isActive()) {
        txn.rollback();
      } else if (e instanceof PersistenceException) {
        // recover the transaction
        try {
          txn.begin();
        } catch (Exception ex) {
        }
      }
      try {
        response = new Response();
        response = onException(e, response);
        if (log.isTraceEnabled()) {
          log.trace("Exception: {}", e.getMessage(), e);
        }
      } finally {
        if (txn.isActive()) {
          txn.rollback();
        }
      }
    } finally {
      running.remove();
    }
    return response;
  }

  private Response onException(Throwable throwable, Response response) {
    final Throwable cause = throwable.getCause();
    final Throwable root = Throwables.getRootCause(throwable);
    for (Throwable ex : Arrays.asList(throwable, cause, root)) {
      if (ex instanceof AuthorizationException) {
        return onAuthorizationException((AuthorizationException) ex, response);
      }
      if (ex instanceof AuthSecurityException) {
        return onAuthSecurityException((AuthSecurityException) ex, response);
      }
      if (ex instanceof OptimisticLockException) {
        return onOptimisticLockException((OptimisticLockException) ex, response);
      }
      if (ex instanceof ConstraintViolationException) {
        return onConstraintViolationException((ConstraintViolationException) ex, response);
      }
      if (ex instanceof SQLException) {
        return onSQLException((SQLException) ex, response);
      }
      if (ex instanceof EncryptorException) {
        return onEncryptorException((EncryptorException) ex, response);
      }
    }
    response.setException(throwable);
    log.error("Error: {}", throwable.getMessage());
    return response;
  }

  private Response onAuthorizationException(AuthorizationException e, Response response) {
    final Map<String, Object> report = new HashMap<>();
    final String title = I18n.get("Access error");
    final String message = e.getMessage();

    report.put("title", title);
    report.put("message", message);

    response.setData(report);
    response.setStatus(Response.STATUS_FAILURE);

    log.error("Authorization Error: {}", e.getMessage());
    return response;
  }

  private Response onAuthSecurityException(AuthSecurityException e, Response response) {
    log.error("Access Error: {}", e.getMessage());
    return e.getType() == AccessType.READ ? response : response.fail(e.getMessage());
  }

  private Response onOptimisticLockException(OptimisticLockException e, Response response) {
    final Map<String, Object> report = new HashMap<>();

    String title = I18n.get("Concurrent updates error");
    String message = I18n.get("Record was updated or deleted by another transaction");

    Object entity = e.getEntity();
    if (entity instanceof Model) {
      message =
          message
              + " : ["
              + entity.getClass().getSimpleName()
              + "{id:"
              + ((Model) entity).getId()
              + "}]";
    }

    report.put("title", title);
    report.put("message", message);

    response.setData(report);
    response.setStatus(Response.STATUS_FAILURE);

    log.error("Concurrency Error: {}", e.getMessage());
    return response;
  }

  private Response onConstraintViolationException(
      ConstraintViolationException e, Response response) {
    final StringBuilder sb = new StringBuilder();
    final Map<String, Object> report = new HashMap<>();
    for (ConstraintViolation<?> cv : e.getConstraintViolations()) {
      sb.append("    &#8226; [").append(cv.getPropertyPath()).append("] - ");
      sb.append(cv.getMessage()).append("\n");
    }

    report.put("title", I18n.get("Validation error"));
    report.put("message", sb.toString());

    response.setData(report);
    response.setStatus(Response.STATUS_FAILURE);

    log.error("Constraint Error: {}", e.getMessage());
    return response;
  }

  private Response onEncryptorException(EncryptorException e, Response response) {
    Throwable cause = e.getCause();
    final Map<String, Object> report = new HashMap<>();
    String message = e.getMessage();
    if (cause instanceof BadPaddingException) {
      message = I18n.get("Encryption key might be wrong.");
    }
    report.put("title", I18n.get("Encryption error"));
    report.put("message", message);
    log.error("Encryption Error: {}", e.getMessage(), e);
    response.setData(report);
    response.setStatus(Response.STATUS_FAILURE);
    return response;
  }

  private Response onSQLException(SQLException e, Response response) {

    if (!(e instanceof PSQLException)) {
      response.setException(e);
      return response;
    }

    String state = e.getSQLState();
    if (state == null) {
      state = "";
    }

    PSQLException pe = (PSQLException) e;

    String title = null;
    String message = pe.getServerErrorMessage().getMessage();

    // http://www.postgresql.org/docs/9.3/static/errcodes-appendix.html
    switch (state) {
      case "23503": // foreign key violation
        title = I18n.get("Reference error");
        message =
            I18n.get(
                "The record(s) are referenced by other records, please remove all the references first.");
        break;
      case "23505": // unique constraint violation
        title = I18n.get("Unique constraint violation");
        message = I18n.get("The record(s) can't be updated as it violates unique constraint.");
        break;
      default:
        title = I18n.get("SQL error");
        message = I18n.get("Unexpected database error occurred on the server.");
        break;
    }

    if (AuthUtils.isAdmin(AuthUtils.getUser()) || AuthUtils.isTechnicalStaff(AuthUtils.getUser())) {
      message += "<p>" + pe.getMessage() + "</p>";
    }

    Map<String, Object> report = new HashMap<>();
    report.put("title", title);
    report.put("message", message);

    response.setData(report);
    response.setStatus(Response.STATUS_FAILURE);

    log.error("SQL Error: {}", e.getMessage());
    return response;
  }
}
