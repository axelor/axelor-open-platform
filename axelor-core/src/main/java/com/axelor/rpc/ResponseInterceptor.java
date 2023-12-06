/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2023 Axelor (<http://axelor.com>).
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

import com.axelor.auth.AuthSecurityException;
import com.axelor.auth.AuthUtils;
import com.axelor.auth.db.User;
import com.axelor.common.crypto.EncryptorException;
import com.axelor.db.JpaSecurity.AccessType;
import com.axelor.db.JpaSupport;
import com.axelor.i18n.I18n;
import com.google.common.base.Throwables;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.Arrays;
import java.util.Optional;
import javax.crypto.BadPaddingException;
import javax.persistence.EntityTransaction;
import javax.persistence.OptimisticLockException;
import javax.persistence.PersistenceException;
import javax.validation.ConstraintViolationException;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.shiro.authz.AuthorizationException;
import org.apache.shiro.authz.UnauthenticatedException;
import org.postgresql.util.PSQLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResponseInterceptor extends JpaSupport implements MethodInterceptor {

  private final Logger log = LoggerFactory.getLogger(ResponseInterceptor.class);

  private final ThreadLocal<Boolean> running = new ThreadLocal<Boolean>();

  @Override
  public Object invoke(final MethodInvocation invocation) throws Throwable {

    if (Boolean.TRUE.equals(running.get())) {
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
      if (ex instanceof SQLIntegrityConstraintViolationException) {
        return onSQLIntegrityConstraintViolationException(
            (SQLIntegrityConstraintViolationException) ex, response);
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
    final String title = I18n.get("Access error");
    final String message = e.getMessage();
    int status = Response.STATUS_FAILURE;

    if (e instanceof UnauthenticatedException) {
      status = Response.STATUS_LOGIN_REQUIRED;
    }

    if (e.getCause() instanceof AuthSecurityException) {
      logAuthSecurityException((AuthSecurityException) e.getCause());
    } else {
      log.error("Authorization Error: {}", e.getMessage());
    }

    ResponseException error = new ResponseException(message, title, e);
    response.setException(error);
    response.setStatus(status);

    return response;
  }

  private Response onAuthSecurityException(AuthSecurityException e, Response response) {
    logAuthSecurityException(e);
    return e.getType() == AccessType.READ ? response : response.fail(e.getMessage());
  }

  private void logAuthSecurityException(AuthSecurityException e) {
    log.error(
        "Authorization Error: [Message={}, User={}, Detail={}]",
        e.getMessage(),
        Optional.ofNullable(AuthUtils.getUser()).map(User::getCode).orElse(null),
        e.getViolationsDetail());
  }

  private Response onOptimisticLockException(OptimisticLockException e, Response response) {
    String title = I18n.get("Concurrent updates error");
    String message = I18n.get("Record was updated or deleted by another transaction.");
    ResponseException error = new ResponseException(message, title, e);
    response.setException(error);
    log.error("Concurrency Error: {}", e.getMessage());
    return response;
  }

  private Response onConstraintViolationException(
      ConstraintViolationException e, Response response) {
    final String title = I18n.get("Validation error");
    final String message = e.getMessage();
    final ResponseException error = new ResponseException(message, title, e);

    log.error("Constraint Error: {}", e.getMessage());

    response.setException(error);
    return response;
  }

  private Response onEncryptorException(EncryptorException e, Response response) {
    Throwable cause = e.getCause();

    String title = I18n.get("Encryption error");
    String message = e.getMessage();
    if (cause instanceof BadPaddingException) {
      message = I18n.get("Encryption key might be wrong.");
    }

    ResponseException error = new ResponseException(message, title, e);
    response.setException(error);

    log.error("Encryption Error: {}", e.getMessage(), e);

    return response;
  }

  static final String REFERENCE_ERROR_TTILE = /*$$(*/ "Reference error" /*)*/;
  static final String REFERENCE_ERROR_MESSAGE = /*$$(*/
      "The record(s) are referenced by other records. Please remove all the references first." /*)*/;

  static final String UNIQUE_VIOLATION_ERROR_TTILE = /*$$(*/ "Unique constraint violation" /*)*/;
  static final String UNIQUE_VIOLATION_ERROR_MESSAGE = /*$$(*/
      "The record(s) can't be updated as it violates unique constraint." /*)*/;

  static final String DEFAULT_ERROR_TTILE = /*$$(*/ "SQL error" /*)*/;
  static final String DEFAULT_ERROR_MESSAGE = /*$$(*/
      "Unexpected database error occurred on the server." /*)*/;

  private Response onSQLIntegrityConstraintViolationException(
      SQLIntegrityConstraintViolationException e, Response response) {
    int errorNumber = e.getErrorCode();

    String title = null;
    String message = null;

    // https://dev.mysql.com/doc/mysql-errors/8.0/en/server-error-reference.html
    switch (errorNumber) {
      case 1217:
        // fall through
      case 1451: // foreign key violation
        title = I18n.get(REFERENCE_ERROR_TTILE);
        message = I18n.get(REFERENCE_ERROR_MESSAGE);
        break;
      case 1062: // unique constraint violation
        title = I18n.get(UNIQUE_VIOLATION_ERROR_TTILE);
        message = I18n.get(UNIQUE_VIOLATION_ERROR_MESSAGE);
        break;
      default:
        title = I18n.get(DEFAULT_ERROR_TTILE);
        message = I18n.get(DEFAULT_ERROR_MESSAGE);
        break;
    }

    log.error("MySQL Error: {}", e.getMessage());

    ResponseException error = new ResponseException(message, title, e);
    response.setException(error);

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

    String title = null;
    String message = null;

    // https://www.postgresql.org/docs/current/errcodes-appendix.html
    switch (state) {
      case "23503": // foreign key violation
        title = I18n.get(REFERENCE_ERROR_TTILE);
        message = I18n.get(REFERENCE_ERROR_MESSAGE);
        break;
      case "23505": // unique constraint violation
        title = I18n.get(UNIQUE_VIOLATION_ERROR_TTILE);
        message = I18n.get(UNIQUE_VIOLATION_ERROR_MESSAGE);
        break;
      default:
        title = I18n.get(DEFAULT_ERROR_TTILE);
        message = I18n.get(DEFAULT_ERROR_MESSAGE);
        break;
    }

    log.error("PostgreSQL Error: {}", e.getMessage());

    ResponseException error = new ResponseException(message, title, e);
    response.setException(error);

    return response;
  }
}
