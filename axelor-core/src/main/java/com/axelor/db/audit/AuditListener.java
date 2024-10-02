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

public class AuditListener
    implements PreDeleteEventListener, PreInsertEventListener, PreUpdateEventListener {

  private static final long serialVersionUID = 1L;

  private static final String UPDATED_BY = "updatedBy";
  private static final String UPDATED_ON = "updatedOn";
  private static final String CREATED_BY = "createdBy";
  private static final String CREATED_ON = "createdOn";

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
        persister.setPropertyValue(entity, i, state[i]);
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
    if (value == null) return;
    for (int i = 0; i < names.length; i++) {
      if (names[i].equals(name)) {
        state[i] = value;
        persister.setPropertyValue(entity, i, value);
        break;
      }
    }
  }

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

      setProperty(persister, entity, names, state, CREATED_ON, now);
      setProperty(persister, entity, names, state, CREATED_BY, user);
    }

    // set sequence field if any
    setSequenceProperty(event);

    // handle tracks
    auditTrail.onPreInsert(event);

    return false;
  }

  @Override
  public boolean onPreUpdate(PreUpdateEvent event) {
    final SessionImplementor session = event.getSession();
    final Object entity = event.getEntity();

    if (entity instanceof AuditableModel && canUpdate(event)) {
      final LocalDateTime now = LocalDateTime.now();
      final User user = AuditUtils.currentUser(session);

      final EntityPersister persister = event.getPersister();
      final String[] names = persister.getPropertyNames();
      final Object[] state = event.getState();

      setProperty(persister, entity, names, state, UPDATED_ON, now);
      setProperty(persister, entity, names, state, UPDATED_BY, user);
    }

    // handle tracks
    auditTrail.onPreUpdate(event);

    return false;
  }

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
