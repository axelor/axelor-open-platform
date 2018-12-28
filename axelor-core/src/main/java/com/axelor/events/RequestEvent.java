/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2018 Axelor (<http://axelor.com>).
 *
 * This program is free software: you can redistribute it and/or  modify
 * it under the terms of the GNU Affero General Public License, version 3,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
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
