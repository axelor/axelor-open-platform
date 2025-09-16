/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.db.audit;

import com.axelor.inject.Beans;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.integrator.spi.Integrator;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.service.spi.SessionFactoryServiceRegistry;

public class AuditIntegrator implements Integrator {

  @Override
  public void integrate(
      Metadata metadata,
      BootstrapContext bootstrapContext,
      SessionFactoryImplementor sessionFactory) {
    final ServiceRegistryImplementor serviceRegistry = sessionFactory.getServiceRegistry();
    final EventListenerRegistry registry = serviceRegistry.getService(EventListenerRegistry.class);
    final AuditListener auditListener = new AuditListener();

    registry.appendListeners(EventType.PRE_INSERT, auditListener);
    registry.appendListeners(EventType.PRE_UPDATE, auditListener);
    registry.appendListeners(EventType.PRE_DELETE, auditListener);

    Beans.get(HibernateListenerService.class).registerListeners(registry);
  }

  @Override
  public void disintegrate(
      SessionFactoryImplementor sessionFactory, SessionFactoryServiceRegistry serviceRegistry) {
    // Nothing to do
  }
}
