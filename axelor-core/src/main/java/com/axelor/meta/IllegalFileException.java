/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
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
