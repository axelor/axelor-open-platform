/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.db.audit;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.Set;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Service to manage and provide access to the set of HibernateListenerConfigurator instances. */
@Singleton
public class HibernateListenerService {

  private final Set<HibernateListenerConfigurator> configurators;

  private static Logger log = LoggerFactory.getLogger(HibernateListenerService.class);

  @Inject
  public HibernateListenerService(Set<HibernateListenerConfigurator> configurators) {
    this.configurators = configurators;
  }

  /**
   * Registers Hibernate event listeners using the provided registry.
   *
   * @param registry event listener registry
   */
  public void registerListeners(EventListenerRegistry registry) {
    for (var configurator : configurators) {
      log.debug("Applying configurator: {}", configurator.getClass().getName());
      configurator.registerListeners(registry);
    }
  }
}
