/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.events;

import com.axelor.rpc.Request;

public class PreRequest extends RequestEvent {

  public PreRequest(Object source, Request request) {
    super(source, request);
  }
}
