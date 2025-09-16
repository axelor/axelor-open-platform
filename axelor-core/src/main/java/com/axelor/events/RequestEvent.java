/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.events;

import com.axelor.rpc.Request;

public class RequestEvent {
  public static final String SEARCH = "search";
  public static final String EXPORT = "export";
  public static final String READ = "read";
  public static final String FETCH = "fetch";
  public static final String SAVE = "save";
  public static final String MASS_UPDATE = "mass-update";
  public static final String REMOVE = "remove";
  public static final String COPY = "copy";
  public static final String FETCH_NAME = "fetch:name";

  private final Object source;
  private final Request request;

  public RequestEvent(Object source, Request request) {
    this.source = source;
    this.request = request;
  }

  public Object getSource() {
    return source;
  }

  public Request getRequest() {
    return request;
  }
}
