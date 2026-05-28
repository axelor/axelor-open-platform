/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.event;

public class ObserverException extends RuntimeException {

  private static final long serialVersionUID = 0L;

  public ObserverException(Throwable cause) {
    super(cause);
  }
}
