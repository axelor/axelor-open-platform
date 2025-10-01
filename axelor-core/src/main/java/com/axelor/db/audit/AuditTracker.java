/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.db.audit;

import static com.axelor.common.StringUtils.isBlank;

import com.axelor.auth.AuthUtils;
import com.axelor.auth.db.User;
import com.axelor.common.Inflector;
import com.axelor.common.StringUtils;
import com.axelor.db.EntityHelper;
import com.axelor.db.JPA;
import com.axelor.db.Model;
import com.axelor.db.Query;
import com.axelor.db.annotations.Track;
import com.axelor.db.annotations.TrackEvent;
import com.axelor.db.annotations.TrackMessage;
import com.axelor.db.internal.DBHelper;
import com.axelor.db.mapper.Adapter;
import com.axelor.db.mapper.Mapper;
import com.axelor.db.mapper.Property;
import com.axelor.db.tracking.FieldTracking;
import com.axelor.db.tracking.ModelTracking;
import com.axelor.event.Event;
import com.axelor.events.internal.BeforeTransactionComplete;
import com.axelor.inject.Beans;
import com.axelor.mail.MailConstants;
import com.axelor.mail.db.MailFollower;
import com.axelor.mail.db.MailMessage;
import com.axelor.mail.db.repo.MailFollowerRepository;
import com.axelor.mail.db.repo.MailMessageRepository;
import com.axelor.meta.MetaFiles;
import com.axelor.meta.db.MetaJsonField;
import com.axelor.meta.db.MetaJsonRecord;
import com.axelor.rpc.ContextHandler;
import com.axelor.rpc.ContextHandlerFactory;
import com.axelor.script.CompositeScriptHelper;
import com.axelor.script.ScriptBindings;
import com.axelor.script.ScriptHelper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
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
import org.hibernate.FlushMode;
import org.hibernate.action.spi.BeforeTransactionCompletionProcess;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.proxy.HibernateProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** This class provides change tracking for auditing and notifications. */
public class AuditTracker implements BeforeTransactionCompletionProcess {

  private static final Logger log = LoggerFactory.getLogger(AuditTracker.class);

  private final Map<String, Map<Long, EntityState>> store = new HashMap<>();
  private final Set<Model> updated = new HashSet<>();
  private final Set<Model> deleted = new HashSet<>();

  private User currentUser;
  private ObjectMapper objectMapper;

  private MailMessageRepository mailMessageRepository;
  private MailFollowerRepository mailFollowerRepository;

  private String toJSON(Object value) {
    if (objectMapper == null) {
      objectMapper = Beans.get(ObjectMapper.class);
    }
    try {
      return objectMapper.writeValueAsString(value);
    } catch (Exception e) {
    }
    return null;
  }

  public List<FieldTracking> getTrackedCustomFields(Model model) {
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

  private ModelTracking getTrack(Model entity) {
    if (entity == null) {
      return null;
    }
    Track track = entity.getClass().getAnnotation(Track.class);
    List<FieldTracking> trackedCustomFields = getTrackedCustomFields(entity);
    if (track == null && trackedCustomFields.isEmpty()) {
      return null;
    }
    return ModelTracking.create(track, trackedCustomFields);
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
        try {
          return Mapper.of(property.getTarget()).get(value, property.getTargetName()).toString();
        } catch (Exception e) {
        }
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

  /**
   * Record the changes as a notification message.
   *
   * @param entity the object being tracked
   * @param names the field names
   * @param state current values
   * @param previousState old values
   */
  public void track(Model entity, String[] names, Object[] state, Object[] previousState) {

    final ModelTracking track = getTrack(entity);
    if (track == null) {
      return;
    }

    final Map<String, Object> values = new HashMap<>();
    final Map<String, Object> oldValues = new HashMap<>();

    for (int i = 0; i < names.length; i++) {
      values.put(names[i], state[i]);
    }

    if (previousState != null) {
      for (int i = 0; i < names.length; i++) {
        oldValues.put(names[i], previousState[i]);
      }
    }

    final Long id = entity.getId();
    final String key = EntityHelper.getEntityClass(entity).getName();
    final Map<Long, EntityState> entityStates = store.computeIfAbsent(key, key_ -> new HashMap<>());

    final EntityState entityState =
        entityStates.computeIfAbsent(
            id,
            id_ -> {
              EntityState newEntityState = new EntityState();
              newEntityState.entity = entity;
              newEntityState.values = values;
              newEntityState.oldValues = oldValues;
              return newEntityState;
            });

    if (entityState.values != values) {
      entityState.values.putAll(values);
    }
  }

  public void deleted(Model entity) {
    deleted.add(entity);
  }

  public void updated(Model entity) {
    updated.add(entity);
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

  private void process(EntityState state, User user) {

    final Model entity = state.entity;
    final Mapper mapper = Mapper.of(entity.getClass());
    final MailMessage message = new MailMessage();

    final ModelTracking track = getTrack(entity);

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

      if (oldValue != null) {
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

    getMailMessageRepository().save(message);

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
      getMailFollowerRepository().save(follower);
    }
  }

  private MailMessageRepository getMailMessageRepository() {
    if (mailMessageRepository == null) {
      mailMessageRepository = Beans.get(MailMessageRepository.class);
    }
    return mailMessageRepository;
  }

  private MailFollowerRepository getMailFollowerRepository() {
    if (mailFollowerRepository == null) {
      mailFollowerRepository = Beans.get(MailFollowerRepository.class);
    }
    return mailFollowerRepository;
  }

  private Object getValue(
      Map<String, Object> values,
      Function<String, Map<String, Object>> jsonValues,
      FieldTracking field,
      Property property) {
    Object value = getValueInternal(values, jsonValues, field, property);

    // If value is not in the L1 cache (may be it's cleared), it may cause lazy initialization error
    if (value instanceof Model model
        && value instanceof HibernateProxy
        && !JPA.em().contains(value)) {
      Long id = model.getId();
      if (id != null) {
        return JPA.em().getReference(EntityHelper.getEntityClass(value), id);
      }
    }

    return value;
  }

  private Object getValueInternal(
      Map<String, Object> values,
      Function<String, Map<String, Object>> jsonValues,
      FieldTracking field,
      Property property) {

    if (!field.isCustomField()) {
      return values.get(field.getFieldName());
    }

    Object value = jsonValues.apply(field.getJsonFieldName()).get(field.getFieldName());

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
        if (value instanceof Map) {
          @SuppressWarnings("unchecked")
          Map<String, Object> map = (Map<String, Object>) value;
          ContextHandler<?> handler = ContextHandlerFactory.newHandler(property.getTarget(), map);
          return handler.getProxy();
        }
        return value;
      default:
        return value;
    }
  }

  private void processTracks() {
    var count = 0;

    for (var states : store.values()) {
      for (var state : states.values()) {
        process(state, currentUser);

        if (++count % DBHelper.getJdbcBatchSize() == 0) {
          JPA.flush();
          JPA.clear();

          if (currentUser != null) {
            currentUser = AuthUtils.getUser(currentUser.getCode());
          }
        }
      }
    }
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

    currentUser = AuditUtils.currentUser(session);
    processTracks();
    processDelete();

    if (session.getHibernateFlushMode() == FlushMode.MANUAL || session.isClosed()) {
      return;
    }

    session.flush();
  }

  private static class EntityState {
    private Model entity;
    private Map<String, Object> values;
    private Map<String, Object> oldValues;
  }

  @Singleton
  static class BeforeTransactionCompleteService {

    @Inject private Event<BeforeTransactionComplete> event;

    public void fire(Set<? extends Model> updated, Set<? extends Model> deleted) {
      event.fire(new BeforeTransactionComplete(updated, deleted));
    }
  }
}
