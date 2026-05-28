/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.inject.logger;

import com.google.inject.Binding;
import jakarta.inject.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class LoggerProvider implements Provider<Logger> {

  @Override
  public Logger get() {
    final Binding<?> binding = LoggerProvisionListener.bindingStack.get().peek();
    return binding == null
        ? LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME)
        : LoggerFactory.getLogger(binding.getKey().getTypeLiteral().getRawType().getName());
  }
}
