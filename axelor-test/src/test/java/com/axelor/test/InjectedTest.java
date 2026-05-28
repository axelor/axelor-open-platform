/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Injector;
import org.junit.jupiter.api.Test;

@GuiceModules(InjectedTest.Module.class)
public class InjectedTest extends GuiceJunit5Test {

  static class Module extends AbstractModule {

    @Override
    protected void configure() {}
  }

  @Inject Injector injector;

  @Test
  public void test() {
    assertNotNull(injector);
  }
}
