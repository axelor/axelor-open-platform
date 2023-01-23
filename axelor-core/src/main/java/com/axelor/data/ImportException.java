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
package com.axelor.data;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
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
    if (ex instanceof ConstraintViolationException) return from((ConstraintViolationException) ex);
    if (ex instanceof PSQLException) return from((PSQLException) ex);
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
            "The record(s) are referenced by other records, please remove all the references first.",
            ex);
      case "23505": // unique constraint violation
        return new ImportException(
            "The record(s) can't be updated as it violates unique constraint.", ex);
      default:
        return new ImportException("Unexpected database error occurred on the server.", ex);
    }
  }
}
