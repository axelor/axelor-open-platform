/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2026 Axelor (<http://axelor.com>).
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
package com.axelor.meta;

import com.axelor.i18n.I18n;
import java.text.MessageFormat;

/**
 * Exception thrown when a file fails validation (e.g. disallowed name or content type).
 *
 * <p>Extends {@link IllegalArgumentException} for backward compatibility with existing catch
 * blocks.
 *
 * <p>Use {@link #getLocalizedMessage()} to retrieve the message translated to the current user
 * locale when sending feedback to the frontend.
 */
public class IllegalFileException extends IllegalArgumentException {

  private static final long serialVersionUID = 1L;

  private final String messageKey;
  private final String param;

  public IllegalFileException(String message) {
    this(message, null);
  }

  /**
   * Creates an exception with a message key and a parameter to format into the message.
   *
   * @param message the message key
   * @param param the value to format into the message (e.g. file name or file type)
   */
  public IllegalFileException(String message, String param) {
    super(message);
    this.messageKey = message;
    this.param = param;
  }

  @Override
  public String getMessage() {
    return MessageFormat.format(messageKey, param);
  }

  /**
   * Returns the exception message translated to the current user locale, with any format parameters
   * applied.
   *
   * @return the translated and formatted message
   */
  @Override
  public String getLocalizedMessage() {
    return MessageFormat.format(I18n.get(messageKey), param);
  }
}
