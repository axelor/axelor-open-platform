/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.db.audit;

import com.axelor.audit.db.AuditEventType;
import com.axelor.audit.db.AuditLog;
import com.axelor.db.EntityHelper;
import com.axelor.db.Model;
import com.axelor.db.Query;
import com.axelor.db.annotations.Track;
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
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.hibernate.FlushMode;
import org.hibernate.action.spi.AfterTransactionCompletionProcess;
import org.hibernate.action.spi.BeforeTransactionCompletionProcess;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;

/** This class provides change tracking for auditing and notifications. */
public class AuditTracker
    implements BeforeTransactionCompletionProcess, AfterTransactionCompletionProcess {

  private final String txId;

  private final Set<Model> updated = new HashSet<>();
  private final Set<Model> deleted = new HashSet<>();

  private static final SecureRandom random = new SecureRandom();
  private static final int BATCH_SIZE = DBHelper.getJdbcBatchSize();

  private final Queue<AuditLog> queue = new ConcurrentLinkedQueue<>();
  private boolean logCreated = false;

  public AuditTracker() {
    this.txId = generateTxId();
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

  private static String toJSON(Object value) {
    try {
      return Beans.get(ObjectMapper.class).writeValueAsString(value);
    } catch (Exception e) {
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

    var values = new HashMap<String, Object>();
    var oldValues = new HashMap<String, Object>();

    for (int i = 0; i < names.length; i++) {
      values.put(names[i], state[i]);
    }

    if (previousState != null) {
      for (int i = 0; i < names.length; i++) {
        oldValues.put(names[i], previousState[i]);
      }
    }

    // OPTIMIZATION: Extract only changed fields
    var changedCurrent = new HashMap<String, Object>();
    var changedOld = new HashMap<String, Object>();

    var mapper = Mapper.of(entity.getClass());

    for (var entry : values.entrySet()) {
      var fieldName = entry.getKey();
      var newValue = entry.getValue();
      var oldValue = oldValues.get(fieldName);

      // Skip audit fields
      if (mapper.getSetter(fieldName) == null) {
        continue;
      }

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

      // Skip unchanged fields
      if (Objects.equals(newValue, oldValue)) {
        continue;
      }

      if (newValue instanceof Model newModel) {
        newValue = newModel.getId();
      }

      if (oldValue instanceof Model oldModel) {
        oldValue = oldModel.getId();
      }

      changedCurrent.put(fieldName, newValue);

      if (!oldValues.isEmpty()) {
        changedOld.put(fieldName, oldValue);
      }
    }

    // Get current user
    var user = AuditUtils.currentUser(session);

    // Create new AuditLog - always INSERT
    var auditLog = new AuditLog();
    auditLog.setRelatedModel(EntityHelper.getEntityClass(entity).getName());
    auditLog.setRelatedId(entity.getId());
    auditLog.setTxId(txId);
    auditLog.setEventType(oldValues.isEmpty() ? AuditEventType.CREATE : AuditEventType.UPDATE);
    auditLog.setCurrentState(toJSON(changedCurrent));
    auditLog.setPreviousState(oldValues.isEmpty() ? null : toJSON(changedOld));
    auditLog.setUser(user);
    auditLog.setProcessed(false);

    // We can't use entity manager here because we are in the middle of flush
    // and Hibernate doesn't allow new entity inserts at this point.

    // Directly create mutation query to insert AuditLog, bypassing normal entity manager flow.
    enqueueAuditLog(session, auditLog);
  }

  private void enqueueAuditLog(SessionImplementor session, AuditLog auditLog) {
    queue.add(auditLog);
    logCreated = true;
    if (queue.size() >= BATCH_SIZE) {
      flushQueue(session, false);
    }
  }

  private void flushQueue(SessionImplementor session, boolean full) {
    if (queue.isEmpty()) {
      return;
    }

    var builder = new StringBuilder();
    var params = new ArrayList<AuditLog>();

    var n = full ? queue.size() : BATCH_SIZE;
    while (n-- > 0) {
      var auditLog = queue.poll();
      if (auditLog == null) {
        break;
      }
      params.add(auditLog);
    }

    builder.append("INSERT INTO AuditLog (");
    builder.append(
        "txId, eventType, relatedModel, relatedId, currentState, previousState, user, processed, createdBy, createdOn");
    builder.append(") VALUES ");

    for (int i = 0; i < params.size(); i++) {
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

    for (int i = 0; i < params.size(); i++) {
      var log = params.get(i);
      query.setParameter("txId" + i, log.getTxId());
      query.setParameter("eventType" + i, log.getEventType());
      query.setParameter("relatedModel" + i, log.getRelatedModel());
      query.setParameter("relatedId" + i, log.getRelatedId());
      query.setParameter("currentState" + i, log.getCurrentState());
      query.setParameter("previousState" + i, log.getPreviousState());
      query.setParameter("user" + i, log.getUser());
      query.setParameter("processed" + i, log.getProcessed());
      query.setParameter("createdBy" + i, log.getUser());
      query.setParameter("createdOn" + i, LocalDateTime.now());
    }

    query.setHibernateFlushMode(FlushMode.MANUAL); // Prevent flush during audit log insertion
    query.executeUpdate();
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
    flushQueue(session, true);

    if (session.getHibernateFlushMode() == FlushMode.MANUAL || session.isClosed()) {
      return;
    }

    session.flush();
  }

  @Override
  public void doAfterTransactionCompletion(
      boolean success, SharedSessionContractImplementor session) {
    if (logCreated) {
      Beans.get(AuditQueue.class).process(txId);
    }
  }

  @Singleton
  static class BeforeTransactionCompleteService {

    @Inject private Event<BeforeTransactionComplete> event;

    public void fire(Set<? extends Model> updated, Set<? extends Model> deleted) {
      event.fire(new BeforeTransactionComplete(updated, deleted));
    }
  }
}
