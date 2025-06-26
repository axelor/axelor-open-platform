/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2025 Axelor (<http://axelor.com>).
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
