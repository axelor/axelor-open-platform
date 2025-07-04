/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.auth;

import com.axelor.i18n.I18n;
import java.text.MessageFormat;
import java.time.Duration;
import java.time.LocalDateTime;

public class MFATooManyRequestsException extends RuntimeException {

  private final LocalDateTime retryAfter;

  public MFATooManyRequestsException(LocalDateTime retryAfter) {
    super(
        MessageFormat.format(
            I18n.get("Too many requests. You can retry in {0} seconds."),
            Duration.between(LocalDateTime.now(), retryAfter).toSeconds()));
    this.retryAfter = retryAfter;
  }

  public LocalDateTime getRetryAfter() {
    return retryAfter;
  }
}
