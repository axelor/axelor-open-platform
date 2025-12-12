/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.db.audit;

import static com.axelor.common.StringUtils.isBlank;

import com.axelor.audit.db.AuditEventType;
import com.axelor.audit.db.AuditLog;
import com.axelor.auth.db.User;
import com.axelor.common.Inflector;
import com.axelor.common.StringUtils;
import com.axelor.db.JPA;
import com.axelor.db.Model;
import com.axelor.db.Query;
import com.axelor.db.annotations.TrackEvent;
import com.axelor.db.annotations.TrackMessage;
import com.axelor.db.mapper.Adapter;
import com.axelor.db.mapper.Mapper;
import com.axelor.db.mapper.Property;
import com.axelor.db.tracking.FieldTracking;
import com.axelor.db.tracking.ModelTracking;
import com.axelor.inject.Beans;
import com.axelor.mail.MailConstants;
import com.axelor.mail.db.MailFollower;
import com.axelor.mail.db.MailMessage;
import com.axelor.mail.db.repo.MailFollowerRepository;
import com.axelor.mail.db.repo.MailMessageRepository;
import com.axelor.rpc.ContextHandlerFactory;
import com.axelor.script.CompositeScriptHelper;
import com.axelor.script.ScriptBindings;
import com.axelor.script.ScriptHelper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.persist.Transactional;
import jakarta.inject.Inject;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Service that processes pending audit logs asynchronously. */
public class AuditProcessor {

  private static final Logger log = LoggerFactory.getLogger(AuditProcessor.class);
  private static final int BATCH_SIZE = 100;
  private static final int MAX_RETRY = 3;

  // Throttling constants
  private static final long MIN_PAUSE_MS = 5;
  private static final long ACTIVITY_PAUSE_MS = 200;
  private static final long ACTIVITY_TIMEOUT_MS = 200; // Consider idle after 200ms of no activity

  // The last time activity was signaled
  private static volatile long lastActivityTime = 0;

  @Inject private MailMessageRepository mailMessageRepository;
  @Inject private MailFollowerRepository mailFollowerRepository;
  @Inject private ObjectMapper objectMapper;

  /**
   * Signal that entity tracking is happening (called from AuditTracker). This tells the processor
   * to back off as real work is in progress.
   */
  public static void signalActivity(Object value) {
    if (value instanceof MailMessage
        || value instanceof MailFollower
        || value instanceof AuditLog) {
      return; // Ignore audit & mail entities
    }
    lastActivityTime = System.currentTimeMillis();
  }

  /** Process all pending audit logs. */
  @Transactional
  public void process() {
    log.info("Starting audit log processing...");
    process(this::findPending);
  }

  /** Process audit logs for a specific transaction ID. */
  @Transactional
  public void process(String txId) {
    log.info("Starting audit log processing for transaction ID: {}", txId);
    process(() -> findPending(txId));
  }

  /** Core processing loop that fetches and processes audit logs in batches. */
  private void process(Supplier<List<AuditLog>> fetcher) {
    Thread.currentThread().setPriority(Thread.MIN_PRIORITY);

    var totalProcessed = 0;
    var totalFailed = 0;

    while (true) {
      // Check if main work is active - back off immediately
      if (isSystemBusy()) {
        try {
          Thread.sleep(ACTIVITY_PAUSE_MS);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          break;
        }
        continue; // Re-check before processing
      }

      var batch = fetcher.get();
      if (batch.isEmpty()) {
        break;
      }

      for (var auditLog : batch) {
        try {
          process(auditLog);
          totalProcessed++;
        } catch (Exception e) {
          totalFailed++;
          handleError(auditLog, e);
        }
      }

      // Flush and clear to avoid memory issues
      JPA.flush();
      JPA.clear();

      // Wait a bit before next batch to reduce DB load
      try {
        Thread.sleep(MIN_PAUSE_MS);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        break;
      }
    }

    log.info(
        "Audit log processing complete. Processed: {}, Failed: {}", totalProcessed, totalFailed);
  }

  /** Check if system is busy with main work. Returns true if entity tracking happened recently. */
  private boolean isSystemBusy() {
    return (System.currentTimeMillis() - lastActivityTime) < ACTIVITY_TIMEOUT_MS;
  }

  private void process(AuditLog auditLog) throws Exception {
    // Check if already processed (might be consolidated with previous)
    if (Boolean.TRUE.equals(auditLog.getProcessed())) {
      return;
    }

    var txId = auditLog.getTxId();
    var relatedId = auditLog.getRelatedId();
    var relatedModel = auditLog.getRelatedModel();
    var eventType = auditLog.getEventType();

    // Find all audit logs for same entity in same transaction
    var group = findRelated(txId, eventType, relatedId, relatedModel);
    if (group.isEmpty()) {
      return;
    }

    if (group.size() > 1) {
      log.debug(
          "Consolidating {} audit logs for {}#{} in transaction {}",
          group.size(),
          relatedModel,
          relatedId,
          txId);
    }

    // Consolidate ALL changes from all audit logs in this transaction
    var firstLog = group.get(0);
    var lastLog = group.get(group.size() - 1);
    var oldValues = fromJSON(firstLog.getPreviousState());
    var values = fromJSON(lastLog.getCurrentState());

    // Process with consolidated state
    var entityState = new EntityState();
    var entityClass = Class.forName(relatedModel).asSubclass(Model.class);
    var entity = JPA.em().find(entityClass, relatedId);

    // If entity is deleted, skip processing
    if (entity != null) {
      entityState.entity = entity;
      entityState.received = lastLog.getCreatedOn();
      entityState.values = parseValues(entityClass, values);
      entityState.oldValues = parseValues(entityClass, oldValues);
      entityState.eventType = eventType;
      // Process the audit log
      process(entityState, lastLog.getUser());
    }
    
    // Mark them processed
    for (var logEntry : group) {
      logEntry.setProcessed(true);
      logEntry.setProcessedOn(LocalDateTime.now());
    }

    // Mark ALL audit logs as processed
    for (var item : group) {
      item.setProcessed(true);
      item.setProcessedOn(LocalDateTime.now());
    }
  }

  private void process(EntityState state, User user) {

    final Model entity = state.entity;
    final Mapper mapper = Mapper.of(entity.getClass());
    final MailMessage message = new MailMessage();

    final ModelTracking track = AuditTracker.getTrack(entity);

    final Map<String, Object> values = state.values;
    final Map<String, Object> oldValues = state.oldValues;
    final Map<String, Object> previousState = oldValues.isEmpty() ? null : oldValues;

    final ScriptBindings bindings = new ScriptBindings(state.values);
    final ScriptHelper scriptHelper = new CompositeScriptHelper(bindings);

    final List<Map<String, String>> tags = new ArrayList<>();
    final List<Map<String, String>> tracks = new ArrayList<>();
    final Set<String> tagFields = new HashSet<>();

    final Function<String, Map<String, Object>> jsonValues = new JsonValues(values);
    final Function<String, Map<String, Object>> oldJsonValues = new JsonValues(oldValues);

    // find matched message
    String msg = findMessage(track, track.getMessages(), values, oldValues, scriptHelper);

    // find matched content message
    String content = findMessage(track, track.getContents(), values, oldValues, scriptHelper);

    for (FieldTracking field : track.getFields()) {

      if (!hasEvent(track, field, TrackEvent.ALWAYS)
          && !hasEvent(
              track, field, previousState == null ? TrackEvent.CREATE : TrackEvent.UPDATE)) {
        continue;
      }

      if (!isBlank(field.getCondition()) && !scriptHelper.test(field.getCondition())) {
        continue;
      }

      final String name = field.getFieldName();
      final Property property = mapper.getProperty(entity, field.getName());
      if (property == null) {
        log.debug("{} field not found", field.getName());
        continue;
      }

      String title = property.getTitle();
      if (isBlank(title)) {
        title = Inflector.getInstance().humanize(name);
      }

      final Object value = getValue(values, jsonValues, field, property);
      final Object oldValue = getValue(oldValues, oldJsonValues, field, property);

      if (Objects.equals(value, oldValue)) {
        continue;
      }

      tagFields.add(name);

      final Map<String, String> item = new HashMap<>();
      item.put("name", property.getName());
      item.put("title", title);
      item.put("value", format(property, value));

      if (oldValue != null && state.eventType != AuditEventType.CREATE) {
        item.put("oldValue", format(property, oldValue));
      }

      tracks.add(item);
    }

    // find matched tags
    for (TrackMessage tm : track.getMessages()) {
      boolean canTag =
          tm.fields().length == 0 || (tm.fields().length == 1 && isBlank(tm.fields()[0]));
      for (String name : tm.fields()) {
        if (isBlank(name)) {
          continue;
        }
        canTag = tagFields.contains(name);
        if (canTag) {
          break;
        }
      }
      if (!canTag) {
        continue;
      }
      if (hasEvent(track, tm, previousState == null ? TrackEvent.CREATE : TrackEvent.UPDATE)) {
        if (!isBlank(tm.tag()) && scriptHelper.test(tm.condition())) {
          final Map<String, String> item = new HashMap<>();
          item.put("title", tm.message());
          item.put("style", tm.tag());
          tags.add(item);
        }
      }
    }

    // don't generate empty tracking info
    if (msg == null && content == null && tracks.isEmpty()) {
      return;
    }

    if (msg == null) {
      msg = previousState == null ? /*$$(*/ "Record created" /*)*/ : /*$$(*/ "Record updated" /*)*/;
    }

    final Map<String, Object> json = new HashMap<>();
    json.put("title", msg);
    json.put("tags", tags);
    json.put("tracks", tracks);

    if (!StringUtils.isBlank(content)) {
      json.put("content", content);
    }

    message.setSubject(msg);
    message.setBody(toJSON(json));
    message.setAuthor(user);
    message.setRelatedId(entity.getId());
    message.setRelatedModel(entity.getClass().getName());
    message.setType(MailConstants.MESSAGE_TYPE_NOTIFICATION);
    message.setReceivedOn(state.received);

    mailMessageRepository.save(message);

    try {
      message.setRelatedName(mapper.getNameField().get(entity).toString());
    } catch (Exception e) {
    }

    if (previousState == null && track.isSubscribe()) {
      final MailFollower follower = new MailFollower();
      follower.setRelatedId(entity.getId());
      follower.setRelatedModel(entity.getClass().getName());
      follower.setUser(user);
      follower.setArchived(false);
      mailFollowerRepository.save(follower);
    }
  }

  private void handleError(AuditLog auditLog, Exception e) {
    log.error("Failed to process audit log: {}", auditLog.getId(), e);

    var message = e.getMessage();
    if (message != null && message.length() > 1000) {
      message = message.substring(0, 1000);
    }

    auditLog.setRetryCount(auditLog.getRetryCount() + 1);
    auditLog.setErrorMessage(message);

    if (auditLog.getRetryCount() >= MAX_RETRY) {
      log.error("Max retries exceeded for audit log: {}", auditLog.getId());
      auditLog.setProcessed(true); // Stop retrying
      auditLog.setProcessedOn(LocalDateTime.now());
    }
  }

  private List<AuditLog> findPending() {
    return Query.of(AuditLog.class)
        .filter("self.processed = false AND COALESCE(self.retryCount, 0) < :maxRetry")
        .bind("maxRetry", MAX_RETRY)
        .order("txId")
        .order("relatedModel")
        .order("relatedId")
        .order("eventType")
        .order("createdOn")
        .fetch(BATCH_SIZE);
  }

  private List<AuditLog> findPending(String txId) {
    return Query.of(AuditLog.class)
        .filter(
            "self.processed = false AND COALESCE(self.retryCount, 0) < :maxRetry AND self.txId = :txId")
        .bind("maxRetry", MAX_RETRY)
        .bind("txId", txId)
        .order("relatedModel")
        .order("relatedId")
        .order("eventType")
        .order("createdOn")
        .fetch(BATCH_SIZE);
  }

  private List<AuditLog> findRelated(
      String txId, AuditEventType eventType, Long relatedId, String relatedModel) {
    return Query.of(AuditLog.class)
        .filter(
            """
            self.txId = :txId
            AND self.processed = false
            AND self.eventType = :eventType
            AND self.relatedModel = :relatedModel
            AND self.relatedId = :relatedId
            """)
        .bind("txId", txId)
        .bind("relatedId", relatedId)
        .bind("relatedModel", relatedModel)
        .bind("eventType", eventType)
        .order("createdOn")
        .fetch();
  }

  private boolean hasEvent(TrackEvent event, TrackEvent targetEvent) {
    return event == targetEvent || event == TrackEvent.ALWAYS;
  }

  private boolean hasEvent(ModelTracking track, FieldTracking field, TrackEvent event) {
    return hasEvent(field.getOn(), event)
        || ((field.getOn() == TrackEvent.DEFAULT) && hasEvent(track.getOn(), event));
  }

  private boolean hasEvent(ModelTracking track, TrackMessage message, TrackEvent event) {
    return hasEvent(message.on(), event)
        || (message.on() == TrackEvent.DEFAULT && hasEvent(track.getOn(), event));
  }

  private String format(Property property, Object value) {
    if (value == null) {
      return "";
    }
    switch (property.getType()) {
      case MANY_TO_ONE:
      case ONE_TO_ONE:
        var mapper = Mapper.of(property.getTarget());
        var nameField = mapper.getNameField();
        var nameKey = nameField == null ? "id" : nameField.getName();
        var nameValue = (Object) "N/A";
        if (value instanceof Model model) nameValue = mapper.get(model, nameKey);
        if (value instanceof Map map) nameValue = map.get(nameKey);
        if (nameValue != null) return nameValue.toString();
        break;
      case ONE_TO_MANY:
      case MANY_TO_MANY:
        return "N/A";
      default:
        break;
    }
    if (value instanceof Boolean) {
      return Boolean.TRUE.equals(value) ? "True" : "False";
    }
    if (value instanceof BigDecimal decimal) {
      return decimal.toPlainString();
    }
    if (value instanceof ZonedDateTime zonedDateTime) {
      return zonedDateTime.withZoneSameInstant(ZoneOffset.UTC).toString();
    }
    if (value instanceof LocalDateTime localDateTime) {
      return localDateTime
          .atZone(ZoneId.systemDefault())
          .withZoneSameInstant(ZoneOffset.UTC)
          .toString();
    }
    return value.toString();
  }

  private String findMessage(
      ModelTracking track,
      List<TrackMessage> messages,
      Map<String, Object> values,
      Map<String, Object> oldValues,
      ScriptHelper scriptHelper) {
    for (TrackMessage tm : messages) {
      if (hasEvent(track, tm, oldValues.isEmpty() ? TrackEvent.CREATE : TrackEvent.UPDATE)) {
        boolean matched = tm.fields().length == 0;
        for (String field : tm.fields()) {
          if (isBlank(field)) {
            matched = true;
            break;
          }
          matched =
              oldValues.isEmpty()
                  ? values.containsKey(field)
                  : !Objects.equals(values.get(field), oldValues.get(field));
          if (matched) {
            break;
          }
        }
        if (matched && isBlank(tm.tag()) && scriptHelper.test(tm.condition())) {
          String msg = tm.message();
          // evaluate message expression
          if (msg != null && msg.indexOf("#{") == 0) {
            msg = (String) scriptHelper.eval(msg);
          }
          return msg;
        }
      }
    }
    return null;
  }

  private Map<String, Object> parseValues(Class<?> entityClass, Map<String, Object> values) {
    var mapper = Mapper.of(entityClass);
    var entity = Mapper.toBean(entityClass, null);
    var parsedValues = new HashMap<String, Object>();
    for (var entry : values.entrySet()) {
      var name = entry.getKey();
      var value = entry.getValue();
      var prop = mapper.getProperty(name);
      if (prop == null || prop.isReference()) {
        parsedValues.put(name, value);
        continue;
      }
      prop.set(entity, value);
      parsedValues.put(name, prop.get(entity));
    }
    return parsedValues;
  }

  private Object getValue(
      Map<String, Object> values,
      Function<String, Map<String, Object>> jsonValues,
      FieldTracking field,
      Property property) {

    var value =
        field.isCustomField()
            ? jsonValues.apply(field.getJsonFieldName()).get(field.getFieldName())
            : values.get(field.getFieldName());

    switch (property.getType()) {
      case BOOLEAN:
        return Adapter.adapt(value, Boolean.class, null, null);
      case INTEGER:
        return Adapter.adapt(value, Integer.class, null, null);
      case DECIMAL:
        return Adapter.adapt(value, BigDecimal.class, null, null);
      case DATE:
        return Adapter.adapt(value, LocalDate.class, null, null);
      case DATETIME:
        return Adapter.adapt(value, LocalDateTime.class, null, null);
      case MANY_TO_ONE:
      case ONE_TO_ONE:
        if (value instanceof Map map) {
          @SuppressWarnings("unchecked")
          var handler = ContextHandlerFactory.newHandler(property.getTarget(), map);
          return handler.getProxy();
        }
        return value;
      default:
        return value;
    }
  }

  private Map<String, Object> fromJSON(String json) {
    if (StringUtils.isBlank(json)) {
      return Collections.emptyMap();
    }
    try {
      return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
    } catch (Exception e) {
      log.error("Failed to deserialize JSON", e);
      return Collections.emptyMap();
    }
  }

  private String toJSON(Object value) {
    try {
      return objectMapper.writeValueAsString(value);
    } catch (Exception e) {
    }
    return null;
  }

  private static class EntityState {
    private Model entity;
    private Map<String, Object> values;
    private Map<String, Object> oldValues;
    private LocalDateTime received;
    private AuditEventType eventType;
  }

  private class JsonValues implements Function<String, Map<String, Object>> {
    private final Map<String, Object> values;
    private final Map<String, Map<String, Object>> maps = new HashMap<>();

    public JsonValues(Map<String, Object> values) {
      this.values = values;
    }

    @Override
    public Map<String, Object> apply(String jsonFieldName) {
      return maps.computeIfAbsent(jsonFieldName, k -> fromJSON(values.get(k)));
    }

    private Map<String, Object> fromJSON(Object value) {
      if (value != null) {
        if (objectMapper == null) {
          objectMapper = Beans.get(ObjectMapper.class);
        }
        try {
          return objectMapper.readValue(
              value.toString(), new TypeReference<Map<String, Object>>() {});
        } catch (JsonProcessingException e) {
          log.error(e.getMessage(), e);
        }
      }
      return Collections.emptyMap();
    }
  }
}
