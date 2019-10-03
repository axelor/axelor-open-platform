/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2019 Axelor (<http://axelor.com>).
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
package com.axelor.inject.logger;

import com.google.inject.Binding;
import com.google.inject.spi.ProvisionListener;
import java.util.ArrayDeque;

final class LoggerProvisionListener implements ProvisionListener {

  static final ThreadLocal<ArrayDeque<Binding<?>>> bindingStack =
      new ThreadLocal<ArrayDeque<Binding<?>>>() {
        protected ArrayDeque<Binding<?>> initialValue() {
          return new ArrayDeque<>();
        };
      };

  @Override
  public <T> void onProvision(ProvisionInvocation<T> provision) {
    if (provision.getBinding().getSource() instanceof Class<?>) {
      try {
        bindingStack.get().push(provision.getBinding());
        provision.provision();
      } finally {
        bindingStack.get().pop();
      }
    }
  }
}
