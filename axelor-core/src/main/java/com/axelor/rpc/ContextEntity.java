/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.rpc;

import java.util.Map;

/** This interface is used by {@link ContextHandler} to return real context entity. */
public interface ContextEntity {

  Long getContextId();

  Object getContextEntity();

  Map<String, Object> getContextMap();
}
