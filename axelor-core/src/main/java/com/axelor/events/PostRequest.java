/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.events;

import com.axelor.rpc.Request;
import com.axelor.rpc.Response;

public class PostRequest extends RequestEvent {

  private final Response response;

  public PostRequest(Object source, Request request, Response response) {
    super(source, request);
    this.response = response;
  }

  public Response getResponse() {
    return response;
  }
}
