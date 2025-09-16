/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.web.service;

import com.google.inject.Injector;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.UriInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractService {

  protected Logger LOG = LoggerFactory.getLogger(getClass());

  @Context private UriInfo uriInfo;

  @Inject private Injector injector;

  protected final UriInfo getUriInfo() {
    return uriInfo;
  }

  protected final Injector getInjector() {
    return injector;
  }
}
