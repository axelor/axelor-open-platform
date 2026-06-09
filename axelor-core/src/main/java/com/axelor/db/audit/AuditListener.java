/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.db.audit;

import com.axelor.auth.db.AuditableModel;
import com.axelor.auth.db.Group;
import com.axelor.auth.db.User;
import com.axelor.db.EntityHelper;
import com.axelor.db.JpaSequence;
import com.axelor.db.Model;
import com.axelor.db.mapper.Mapper;
import com.axelor.db.mapper.Property;
import com.axelor.meta.db.MetaSequence;
import jakarta.persistence.PersistenceException;
import java.time.LocalDateTime;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.event.spi.PreDeleteEvent;
import org.hibernate.event.spi.PreDeleteEventListener;
import org.hibernate.event.spi.PreInsertEvent;
import org.hibernate.event.spi.PreInsertEventListener;
import org.hibernate.event.spi.PreUpdateEvent;
import org.hibernate.event.spi.PreUpdateEventListener;
import org.hibernate.persister.entity.EntityPersister;

/**
 * Hibernate listener handling audit concerns: creation field stamping, sequence generation, change
 * tracking and admin access control on insert, update and delete.
 */
public class AuditListener
    implements PreDeleteEventListener, PreInsertEventListener, PreUpdateEventListener {

  private static final String ADMIN_USER = "admin";
  private static final String ADMIN_GROUP = "admins";
  private static final String ADMIN_CHECK_FIELD = "code";

  final AuditTrail auditTrail = new AuditTrail();

  private boolean canUpdate(PreUpdateEvent event) {
    final Object id = event.getId();
    final Object entity = event.getEntity();
    final String[] names = event.getPersister().getPropertyNames();
    final Object[] state = event.getState();
    final Object[] oldState = event.getOldState();
    for (int i = 0; i < names.length; i++) {
      if (!canUpdate(entity, names[i], oldState[i], state[i])) {
        throw new PersistenceException(
            String.format(
                "You can't update: %s#%s, values (%s=%s)",
                entity.getClass().getName(), id, names[i], state[i]));
      }
    }
    return true;
  }

  private boolean canUpdate(Object entity, String field, Object prevValue, Object newValue) {
    if (!(entity instanceof Model) || ((Model) entity).getId() == null) {
      return true;
    }
    if (entity instanceof User || entity instanceof Group) {
      if (!ADMIN_CHECK_FIELD.equals(field)) {
        return true;
      }
      if (entity instanceof User && ADMIN_USER.equals(prevValue) && !ADMIN_USER.equals(newValue)) {
        return false;
      }
      if (entity instanceof Group
          && ADMIN_GROUP.equals(prevValue)
          && !ADMIN_GROUP.equals(newValue)) {
        return false;
      }
    }
    return true;
  }

  private boolean canDelete(Object entity) {
    if (entity instanceof User && ADMIN_USER.equals(((User) entity).getCode())) {
      return false;
    }
    if (entity instanceof Group && ADMIN_GROUP.equals(((Group) entity).getCode())) {
      return false;
    }
    return true;
  }

  private void setSequenceProperty(PreInsertEvent event) {
    final Object entity = event.getEntity();
    if ((entity instanceof MetaSequence) || !(entity instanceof Model)) {
      return;
    }

    final SessionImplementor session = event.getSession();
    final EntityPersister persister = event.getPersister();
    final String[] names = persister.getPropertyNames();
    final Object[] state = event.getState();

    final Mapper mapper = Mapper.of(EntityHelper.getEntityClass(entity));

    for (int i = 0; i < names.length; i++) {
      if (state[i] != null) {
        continue;
      }
      final Property property = mapper.getProperty(names[i]);
      if (property != null && property.isSequence()) {
        state[i] = JpaSequence.nextValue(session, property.getSequenceName());
        persister.setValue(entity, i, state[i]);
      }
    }
  }

  private void setProperty(
      EntityPersister persister,
      Object entity,
      String[] names,
      Object[] state,
      String name,
      Object value) {
    setProperty(persister, entity, names, state, name, value, false);
  }

  private void setProperty(
      EntityPersister persister,
      Object entity,
      String[] names,
      Object[] state,
      String name,
      Object value,
      boolean onlyIfAbsent) {
    if (value == null) return;
    for (int i = 0; i < names.length; i++) {
      if (names[i].equals(name)) {
        if (onlyIfAbsent && state[i] != null) {
          break;
        }
        state[i] = value;
        persister.setValue(entity, i, value);
        break;
      }
    }
  }

  /**
   * Stamps the creation audit fields, assigns sequence values and records change tracking.
   *
   * <p>The {@code createdOn}/{@code createdBy} fields are stamped here (only when not already set),
   * unlike their update counterparts: {@code PRE_INSERT} runs before Hibernate builds the {@code
   * INSERT} statement, so the values are picked up even for {@code @DynamicInsert} entities.
   */
  @Override
  public boolean onPreInsert(PreInsertEvent event) {
    final SessionImplementor session = event.getSession();
    final Object entity = event.getEntity();

    if (entity instanceof AuditableModel) {
      final LocalDateTime now = LocalDateTime.now();
      final User user = AuditUtils.currentUser(session);

      final EntityPersister persister = event.getPersister();
      final String[] names = persister.getPropertyNames();
      final Object[] state = event.getState();

      setProperty(persister, entity, names, state, AuditUtils.CREATED_ON, now, true);
      setProperty(persister, entity, names, state, AuditUtils.CREATED_BY, user, true);
    }

    // set sequence field if any
    setSequenceProperty(event);

    // handle tracks
    auditTrail.onPreInsert(event);

    return false;
  }

  /**
   * Handles update access control and change tracking.
   *
   * <p>The {@code updatedOn}/{@code updatedBy} audit fields are intentionally <strong>not</strong>
   * stamped here. {@code PRE_UPDATE} fires after Hibernate has frozen the {@code UPDATE} column
   * set, so writing them at this point would be silently dropped for {@code @DynamicUpdate}
   * entities. They are stamped earlier, during dirty checking, by {@link AuditUpdateListener}.
   */
  @Override
  public boolean onPreUpdate(PreUpdateEvent event) {
    // Check if entity can be updated
    canUpdate(event);

    // handle tracks
    auditTrail.onPreUpdate(event);

    return false;
  }

  /**
   * Enforces delete access control and records change tracking.
   *
   * @throws PersistenceException if the entity is not allowed to be deleted (e.g. the {@code admin}
   *     user or {@code admins} group)
   */
  @Override
  public boolean onPreDelete(PreDeleteEvent event) {
    final Object id = event.getId();
    final Object entity = event.getEntity();

    if (!canDelete(entity)) {
      throw new PersistenceException(
          String.format(
              "You can't delete: %s#%s", EntityHelper.getEntityClass(entity).getName(), id));
    }

    // handle delete
    auditTrail.onPreDelete(event);

    return false;
  }
}
