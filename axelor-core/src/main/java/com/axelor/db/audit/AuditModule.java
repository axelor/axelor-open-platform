/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.db.audit;

import com.google.inject.AbstractModule;

public class AuditModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(AuditQueue.class).to(AsyncAuditQueue.class);
  }
}
