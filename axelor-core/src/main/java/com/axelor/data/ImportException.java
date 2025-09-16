/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.data;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.postgresql.util.PSQLException;

public class ImportException extends Exception {

  private static final long serialVersionUID = 142008624773378396L;

  public ImportException() {
    super();
  }

  public ImportException(String message, Throwable cause) {
    super(message, cause);
  }

  public ImportException(String message) {
    super(message);
  }

  public ImportException(Throwable cause) {
    super(cause);
  }

  public static ImportException from(Throwable ex) {
    if (ex instanceof ConstraintViolationException constraintViolationException)
      return from(constraintViolationException);
    if (ex instanceof PSQLException psqlException) return from(psqlException);
    return new ImportException(ex);
  }

  private static ImportException from(ConstraintViolationException e) {
    final StringBuilder sb = new StringBuilder();

    sb.append("Constraint violation error:");
    sb.append("\n");

    for (ConstraintViolation<?> cv : e.getConstraintViolations()) {
      sb.append("    - [").append(cv.getPropertyPath()).append("] - ");
      sb.append(cv.getMessage()).append("\n");
    }

    return new ImportException(sb.toString(), e);
  }

  private static ImportException from(PSQLException ex) {
    String state = ex.getSQLState();
    if (state == null) {
      state = "";
    }

    // http://www.postgresql.org/docs/9.3/static/errcodes-appendix.html
    switch (state) {
      case "23503": // foreign key violation
        return new ImportException(
            "The record(s) are referenced by other records. Please remove all the references first.",
            ex);
      case "23505": // unique constraint violation
        return new ImportException(
            "The record(s) can't be updated as it violates unique constraint.", ex);
      default:
        return new ImportException("Unexpected database error occurred on the server.", ex);
    }
  }
}
