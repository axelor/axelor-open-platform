/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.db.audit;

import org.hibernate.event.service.spi.EventListenerRegistry;

/*
 * Service for configuring additional Hibernate event listeners.
 */
public interface HibernateListenerConfigurator {

  /*
   * Registers Hibernate event listeners using the provided registry.
   *
   * @param registry event listener registry
   *
   */
  void registerListeners(EventListenerRegistry registry);
}
