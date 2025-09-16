/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.inject.logger;

import com.google.inject.Binding;
import com.google.inject.spi.ProvisionListener;
import java.util.ArrayDeque;

final class LoggerProvisionListener implements ProvisionListener {

  static final ThreadLocal<ArrayDeque<Binding<?>>> bindingStack =
      new ThreadLocal<>() {
        protected ArrayDeque<Binding<?>> initialValue() {
          return new ArrayDeque<>();
        }
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
