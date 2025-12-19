/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.db.audit;

import com.axelor.app.AppSettings;
import com.axelor.app.AvailableAppSettings;
import com.axelor.audit.db.AuditEventType;
import com.axelor.db.EntityHelper;
import com.axelor.db.Model;
import com.axelor.db.Query;
import com.axelor.db.annotations.Track;
import com.axelor.db.audit.state.EntityState;
import com.axelor.db.internal.DBHelper;
import com.axelor.db.mapper.Mapper;
import com.axelor.db.mapper.PropertyType;
import com.axelor.db.tracking.FieldTracking;
import com.axelor.db.tracking.ModelTracking;
import com.axelor.event.Event;
import com.axelor.events.internal.BeforeTransactionComplete;
import com.axelor.inject.Beans;
import com.axelor.meta.MetaFiles;
import com.axelor.meta.db.MetaJsonField;
import com.axelor.meta.db.MetaJsonRecord;
import com.axelor.rpc.Resource;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.hibernate.FlushMode;
import org.hibernate.action.spi.AfterTransactionCompletionProcess;
import org.hibernate.action.spi.BeforeTransactionCompletionProcess;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** This class provides change tracking for auditing and notifications. */
public class AuditTracker
    implements BeforeTransactionCompletionProcess, AfterTransactionCompletionProcess {

  private static final Logger log = LoggerFactory.getLogger(AuditTracker.class);

  private final String txId;

  private final Set<Model> updated = new HashSet<>();
  private final Set<Model> deleted = new HashSet<>();

  private static final SecureRandom random = new SecureRandom();
  private static final int FLUSH_THRESHOLD =
      AppSettings.get()
          .getInt(AvailableAppSettings.AUDIT_LOGS_FLUSH_THRESHOLD, DBHelper.getJdbcBatchSize());

  private final Map<StoreKey, EntityState> store = new HashMap<>();
  private ObjectMapper mapper;
  private boolean logCreated = false;

  public AuditTracker() {
    this.txId = generateTxId();
    this.mapper = Beans.get(ObjectMapper.class);
  }

  /**
   * Generate transaction id
   *
   * @return UUIDv7
   */
  private static String generateTxId() {
    var unixMillis = Instant.now().toEpochMilli();

    // --- 48 bits timestamp ---
    var ms = unixMillis & 0xFFFFFFFFFFFFL;

    // --- random bits ---
    var randA = random.nextInt(1 << 12); // 12 bits
    var randB = random.nextLong() & 0x3FFFFFFFFFFFFFFFL; // 62 bits

    // Construct MSB (timestamp + version 7)
    var msb =
        (ms << 16)
            | 0x7000 // version = 7 (bits 12–15)
            | randA;

    // Construct LSB (variant + 62 random bits)
    var lsb = (0x8000000000000000L) | randB; // variant 2 (RFC-4122)

    return new UUID(msb, lsb).toString();
  }

  private String toJSON(Object value) {
    try {
      return mapper.writeValueAsString(value);
    } catch (Exception e) {
      log.error("Failed to serialize entity values to JSON", e);
    }
    return null;
  }

  public static List<FieldTracking> getTrackedCustomFields(Model model) {
    Query<MetaJsonField> query = Query.of(MetaJsonField.class).cacheable().autoFlush(false);

    if (model instanceof MetaJsonRecord metaJsonRecord) {
      query
          .filter("self.jsonModel.name = :name AND self.tracked IS TRUE")
          .bind("name", metaJsonRecord.getJsonModel());
    } else {
      query
          .filter("self.model = :model AND self.tracked IS TRUE")
          .bind("model", model.getClass().getName());
    }

    return query.fetch().stream().map(FieldTracking::new).toList();
  }

  public static ModelTracking getTrack(Model entity) {
    if (entity == null) {
      return null;
    }
    var entityClass = EntityHelper.getEntityClass(entity);
    var track = entityClass.getAnnotation(Track.class);
    var trackedCustomFields = getTrackedCustomFields(entity);
    if (track == null && trackedCustomFields.isEmpty()) {
      return null;
    }
    return ModelTracking.create(track, trackedCustomFields);
  }

  /**
   * Track entity changes and create AuditLog IMMEDIATELY. Always creates new AuditLog - no queries,
   * no consolidation here. Consolidation happens in AuditProcessor during background processing.
   *
   * @param entity the object being tracked
   * @param names the field names
   * @param state current values
   * @param previousState old values
   */
  public void track(
      SessionImplementor session,
      Model entity,
      String[] names,
      Object[] state,
      Object[] previousState) {

    // Signal activity to audit processor (skip audit's own entities)
    AuditProcessor.signalActivity(entity);

    var track = getTrack(entity);
    if (track == null) {
      return;
    }

    // Store FULL entity state (required for condition evaluation in tracking messages)
    var currentValues = new HashMap<String, Object>();
    var previousValues = previousState != null ? new HashMap<String, Object>() : null;
    var entityClass = EntityHelper.getEntityClass(entity);
    var mapper = Mapper.of(entityClass);

    for (int i = 0; i < names.length; i++) {
      var fieldName = names[i];
      var newValue = state[i];
      var oldValue = previousState != null ? previousState[i] : null;

      // Skip non-trackable fields
      var property = mapper.getProperty(fieldName);
      if (property == null
          || property.isTransient()
          || property.isPassword()
          || property.isEncrypted()
          || property.isCollection()
          || property.getType() == PropertyType.BINARY) {
        continue;
      }

      // For reference fields, store both id and name field value
      if (newValue instanceof Model newModel) {
        newValue = Resource.toMapCompact(newModel);
      }
      if (oldValue instanceof Model oldModel) {
        oldValue = Resource.toMapCompact(oldModel);
      }

      currentValues.put(fieldName, newValue);
      if (previousValues != null) {
        previousValues.put(fieldName, oldValue);
      }
    }

    var entityState =
        store.computeIfAbsent(
            new StoreKey(entityClass, entity.getId()),
            key -> new EntityState(entity, currentValues, previousValues));

    if (entityState.getValues() != currentValues) {
      entityState.getValues().putAll(currentValues);
    }

    if (store.size() >= FLUSH_THRESHOLD) {
      processStore(session);
    }
  }

  private void processStore(SessionImplementor session) {
    if (store.isEmpty()) {
      return;
    }

    log.trace("Flushing {} AuditLog records", store.size());

    try {
      // We can't use entity manager here because we are in the middle of flush
      // and Hibernate doesn't allow new entity inserts at this point.
      // Directly create mutation query to insert AuditLogs, bypassing normal entity manager flow.

      var builder = new StringBuilder();
      builder.append("INSERT INTO AuditLog (");
      builder.append(
          "txId, eventType, relatedModel, relatedId, currentState, previousState, user, processed,"
              + " createdBy, createdOn");
      builder.append(") VALUES ");

      for (int i = 0; i < store.size(); ++i) {
        if (i > 0) {
          builder.append(", ");
        }
        builder
            .append("(:txId")
            .append(i)
            .append(", :eventType")
            .append(i)
            .append(", :relatedModel")
            .append(i)
            .append(", :relatedId")
            .append(i)
            .append(", :currentState")
            .append(i)
            .append(", :previousState")
            .append(i)
            .append(", :user")
            .append(i)
            .append(", :processed")
            .append(i)
            .append(", :createdBy")
            .append(i)
            .append(", :createdOn")
            .append(i)
            .append(")");
      }

      var query = session.createMutationQuery(builder.toString());
      var user = AuditUtils.currentUser(session);
      var now = LocalDateTime.now();

      int i = 0;
      for (var state : store.values()) {
        var entity = state.getEntity();
        var isCreate = state.getOldValues() == null;

        query.setParameter("txId" + i, txId);
        query.setParameter(
            "eventType" + i, isCreate ? AuditEventType.CREATE : AuditEventType.UPDATE);
        query.setParameter("relatedModel" + i, EntityHelper.getEntityClass(entity).getName());
        query.setParameter("relatedId" + i, entity.getId());
        query.setParameter("currentState" + i, toJSON(state.getValues()));
        query.setParameter("previousState" + i, isCreate ? null : toJSON(state.getOldValues()));
        query.setParameter("user" + i, user);
        query.setParameter("processed" + i, false);
        query.setParameter("createdBy" + i, user);
        query.setParameter("createdOn" + i, now);
        ++i;
      }

      query.setHibernateFlushMode(FlushMode.MANUAL); // Prevent flush during audit log insertion
      query.executeUpdate();
      logCreated = true;
    } finally {
      store.clear();
    }
  }

  public void deleted(Model entity) {
    deleted.add(entity);
  }

  public void updated(Model entity) {
    updated.add(entity);
  }

  private void processDelete() {
    final MetaFiles files = Beans.get(MetaFiles.class);
    for (Model entity : deleted) {
      log.trace(
          "Delete attachments for the entity {}#{}",
          entity.getClass().getSimpleName(),
          entity.getId());
      files.deleteAttachments(entity);
    }
  }

  private void fireBeforeCompleteEvent() {
    if (!updated.isEmpty() || !deleted.isEmpty()) {
      Beans.get(BeforeTransactionCompleteService.class).fire(updated, deleted);
    }
  }

  @Override
  public void doBeforeTransactionCompletion(SessionImplementor session) {
    fireBeforeCompleteEvent();
    processDelete();
    processStore(session);

    if (session.getHibernateFlushMode() == FlushMode.MANUAL || session.isClosed()) {
      return;
    }

    session.flush();
  }

  @Override
  public void doAfterTransactionCompletion(
      boolean success, SharedSessionContractImplementor session) {
    if (success && logCreated) {
      Beans.get(AuditQueue.class).process(txId);
    }
  }

  private static record StoreKey(Class<? extends Model> entityClass, Long id) {}

  @Singleton
  static class BeforeTransactionCompleteService {

    @Inject private Event<BeforeTransactionComplete> event;

    public void fire(Set<? extends Model> updated, Set<? extends Model> deleted) {
      event.fire(new BeforeTransactionComplete(updated, deleted));
    }
  }
}
