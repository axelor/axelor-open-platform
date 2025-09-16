/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor;

import com.axelor.app.AppModule;
import com.axelor.auth.AuthModule;
import com.axelor.db.JpaModule;
import com.axelor.rpc.ObjectMapperProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.AbstractModule;

public class JpaTestModule extends AbstractModule {

  @Override
  protected void configure() {

    bind(ObjectMapper.class).toProvider(ObjectMapperProvider.class);

    install(
        new JpaModule("testUnit")
            .scan("com.axelor.auth.db")
            .scan("com.axelor.meta.db")
            .scan("com.axelor.mail.db")
            .scan("com.axelor.dms.db")
            .scan("com.axelor.test.db"));
    install(new AuthModule());
    install(new AppModule());
  }
}
