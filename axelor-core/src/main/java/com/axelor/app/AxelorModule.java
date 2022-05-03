/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2022 Axelor (<http://axelor.com>).
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.axelor.app;

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
}
