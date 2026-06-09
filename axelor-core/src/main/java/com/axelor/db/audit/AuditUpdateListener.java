/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.db.audit;

import com.axelor.auth.db.AuditableModel;
import com.axelor.auth.db.User;
import java.time.LocalDateTime;
import org.hibernate.event.internal.DefaultFlushEntityEventListener;
import org.hibernate.event.spi.FlushEntityEvent;
import org.hibernate.persister.entity.EntityPersister;

/**
 * Stamps the {@code updatedOn}/{@code updatedBy} audit fields of {@link AuditableModel} entities on
 * update.
 *
 * <h2>Why not a {@code PreUpdateEventListener}?</h2>
 *
 * The natural place to set these fields would be {@code PreUpdateEventListener#onPreUpdate}. It
 * works for most entities, but <strong>not</strong> for entities mapped with
 * {@code @DynamicUpdate}: by the time {@code PRE_UPDATE} fires (from {@code EntityUpdateAction}),
 * Hibernate has already computed which columns are dirty and frozen the column list of the {@code
 * UPDATE} statement. Writing the audit values into the event state at that point is too late — the
 * {@code updated_on} / {@code updated_by} columns are simply not part of the generated SQL, and the
 * change is silently dropped.
 *
 * <p>Setting the fields must therefore happen <em>during dirty checking</em>, i.e. in the {@code
 * FLUSH_ENTITY} phase, so the audit columns become part of the dirty set that drives the dynamic
 * {@code UPDATE}. This is the same phase the legacy {@code AuditInterceptor#onFlushDirty} used,
 * which is why audit stamping worked before the interceptor-to-listener migration.
 *
 * <h2>Why override {@code invokeInterceptor} and not {@code onFlushEntity}?</h2>
 *
 * {@code onFlushEntity} is invoked for <em>every</em> managed entity during flush, and the "is this
 * entity actually dirty?" decision happens <em>inside</em> it (in {@code isUpdateNecessary}).
 * Stamping from an {@code onFlushEntity} override would run before that gate, marking every loaded
 * {@code AuditableModel} dirty and triggering spurious {@code UPDATE}s on records that were never
 * touched.
 *
 * <p>{@code invokeInterceptor} is only reached (via {@code scheduleUpdate}) once an entity has
 * already been found dirty, so stamping here is naturally gated to genuine updates. Returning
 * {@code true} then makes {@code handleInterception} re-run the dirty check, folding the audit
 * columns into the dirty set. It is also the exact hook Hibernate uses for JPA {@code @PreUpdate}
 * callbacks and the legacy {@code Interceptor#onFlushDirty}.
 */
public class AuditUpdateListener extends DefaultFlushEntityEventListener {

  /**
   * {@inheritDoc}
   *
   * <p>After delegating to the default implementation, stamps the audit fields for live updates of
   * {@link AuditableModel} entities.
   */
  @Override
  protected boolean invokeInterceptor(FlushEntityEvent event) {
    boolean intercepted = super.invokeInterceptor(event);

    // Skip entities scheduled removed: there is nothing to stamp.
    final var entry = event.getEntityEntry();
    if (entry.getStatus().isDeletedOrGone()) {
      return intercepted;
    }

    if (event.getEntity() instanceof AuditableModel) {
      stampUpdateFields(event);
      // Signal that state was modified so handleInterception re-runs the dirty check and includes
      // the audit columns in the (possibly dynamic) UPDATE.
      intercepted = true;
    }

    return intercepted;
  }

  /**
   * Stamps the {@code updatedOn}/{@code updatedBy} fields into the flush state of the given event.
   *
   * <p>The values are written into the event property values (not the entity): the dirty check
   * compares them against the loaded snapshot, and signalling interception makes Hibernate copy the
   * state back onto the entity via {@code persister.setPropertyValues}, keeping the in-memory
   * object consistent for free.
   */
  private void stampUpdateFields(FlushEntityEvent event) {
    final LocalDateTime now = LocalDateTime.now();
    final User user = AuditUtils.currentUser(event.getSession());

    final EntityPersister persister = event.getEntityEntry().getPersister();
    final Object[] values = event.getPropertyValues();
    setValue(persister, values, AuditUtils.UPDATED_ON, now);
    setValue(persister, values, AuditUtils.UPDATED_BY, user);
  }

  /** Sets {@code value} into the flush {@code values} array at the index of the named property. */
  private void setValue(EntityPersister persister, Object[] values, String name, Object value) {
    if (value == null) return;
    final String[] names = persister.getPropertyNames();
    for (int i = 0; i < names.length; i++) {
      if (names[i].equals(name)) {
        values[i] = value;
        break;
      }
    }
  }
}
