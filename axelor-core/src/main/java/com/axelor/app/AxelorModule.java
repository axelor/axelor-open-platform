/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.app;

import com.axelor.db.audit.HibernateListenerConfigurator;
import com.axelor.ui.QuickMenuCreator;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.multibindings.Multibinder;

/**
 * The entry-point of application module.
 *
 * <p>Application module can provide an implementation of {@link AxelorModule} to configure {@link
 * Guice} bindings and do some initialization when application starts.
 */
public abstract class AxelorModule extends AbstractModule {

  protected Multibinder<QuickMenuCreator> quickMenuBinder() {
    return Multibinder.newSetBinder(binder(), QuickMenuCreator.class);
  }

  protected void addQuickMenu(Class<? extends QuickMenuCreator> quickMenuClass) {
    quickMenuBinder().addBinding().to(quickMenuClass);
  }

  protected Multibinder<HibernateListenerConfigurator> hibernateListenerConfiguratorBinder() {
    return Multibinder.newSetBinder(binder(), HibernateListenerConfigurator.class);
  }

  protected void addHibernateListenerConfigurator(
      Class<? extends HibernateListenerConfigurator> listenerClass) {
    hibernateListenerConfiguratorBinder().addBinding().to(listenerClass);
  }
}
