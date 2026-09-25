/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.concurrent;

import com.axelor.db.JPA;
import com.axelor.db.tenants.TenantModule;
import com.axelor.db.tenants.TenantResolver;
import com.axelor.inject.Beans;
import com.google.inject.persist.UnitOfWork;
import java.util.Objects;
import org.hibernate.Session;

/**
 * Scopes the unit of work of a context-aware task.
 *
 * <p>Hibernate binds a session to the tenant resolved when the session is opened, and guice-persist
 * keeps the {@code EntityManager} on the thread until the unit of work is ended. Tasks running on
 * reused threads (executors) must therefore get their own unit of work, started after the context
 * is applied, so that the session is opened on the task's tenant.
 *
 * <p>If the thread already has a unit of work (task run inline), it is reused and left open, but it
 * must belong to the task's tenant.
 */
final class ContextUnitOfWork implements AutoCloseable {

  private final UnitOfWork unitOfWork;
  private final boolean owner;

  private ContextUnitOfWork(UnitOfWork unitOfWork, boolean owner) {
    this.unitOfWork = unitOfWork;
    this.owner = owner;
  }

  /**
   * Begins a unit of work on the current thread if none is active. Must be called after the context
   * state has been applied.
   *
   * @return the unit of work scope, to be closed once the task is completed
   * @throws IllegalStateException if the active unit of work belongs to another tenant
   */
  static ContextUnitOfWork begin() {
    final UnitOfWork unitOfWork = Beans.get(UnitOfWork.class);
    try {
      unitOfWork.begin();
      return new ContextUnitOfWork(unitOfWork, true);
    } catch (IllegalStateException e) {
      // already in a unit of work, owned by the caller
      if (TenantModule.isEnabled()) {
        checkTenant();
      }
      return new ContextUnitOfWork(unitOfWork, false);
    }
  }

  private static void checkTenant() {
    final Object sessionTenant = JPA.em().unwrap(Session.class).getTenantIdentifierValue();
    final String currentTenant = TenantResolver.currentTenantIdentifier();
    if (!Objects.equals(sessionTenant, currentTenant)) {
      throw new IllegalStateException(
          String.format(
              "Cannot run a task for tenant '%s' inside a unit of work of tenant '%s'.",
              currentTenant, sessionTenant));
    }
  }

  /** Ends the unit of work if it was started by this scope. */
  @Override
  public void close() {
    if (owner) {
      unitOfWork.end();
    }
  }
}
