/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.inject.logger;

import jakarta.inject.Inject;
import org.slf4j.Logger;

public class TestLoggerService {

  @Inject private Logger log;

  public Logger getLog() {
    return log;
  }
}
