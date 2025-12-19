/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.mail.service;

import static com.axelor.common.StringUtils.isBlank;

import com.axelor.audit.db.AuditEventType;
import com.axelor.auth.db.User;
import com.axelor.db.EntityHelper;
import com.axelor.db.Model;
import com.axelor.db.annotations.TrackEvent;
import com.axelor.db.annotations.TrackMessage;
import com.axelor.db.audit.AuditTracker;
import com.axelor.db.audit.state.AuditState;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service responsible for converting consolidated AuditLog (i.e., {@link AuditState}) into {@link
 * MailMessage}
 */
public class MailMessageTrackingService {

  private static final Logger log = LoggerFactory.getLogger(MailMessageTrackingService.class);

  @Inject private MailMessageRepository mailMessageRepository;
  @Inject private MailFollowerRepository mailFollowerRepository;
  @Inject private ObjectMapper objectMapper;

  public void process(AuditState state, User user) {

    final Model entity = state.getEntity();
    final Class<? extends Model> entityClass = EntityHelper.getEntityClass(entity);
    final Mapper mapper = Mapper.of(entityClass);
    final MailMessage message = new MailMessage();

    final ModelTracking track = AuditTracker.getTrack(entity);

    final Map<String, Object> values = state.getValues();
    final Map<String, Object> oldValues = state.getOldValues();
    final Map<String, Object> previousState = oldValues.isEmpty() ? null : oldValues;

    final ScriptBindings bindings = new ScriptBindings(values);
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
        title = com.axelor.common.Inflector.getInstance().humanize(name);
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

      if (oldValue != null && state.getEventType() != AuditEventType.CREATE) {
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

    if (!com.axelor.common.StringUtils.isBlank(content)) {
      json.put("content", content);
    }

    message.setSubject(msg);
    message.setBody(toJSON(json));
    message.setAuthor(user);
    message.setRelatedId(entity.getId());
    message.setRelatedModel(entityClass.getName());
    message.setType(MailConstants.MESSAGE_TYPE_NOTIFICATION);
    message.setReceivedOn(state.getReceived());

    log.trace(
        "Creating the tracking mail message for the following record : {}#{}",
        entityClass.getName(),
        entity.getId());

    mailMessageRepository.save(message);

    try {
      if (mapper.getNameField() != null) {
        message.setRelatedName(mapper.getNameField().get(entity).toString());
      }
    } catch (Exception e) {
      log.error(
          "Unable to retrieve the name field of the record {}#{}",
          entityClass.getName(),
          entity.getId(),
          e);
    }

    if (previousState == null && track.isSubscribe()) {
      final MailFollower follower = new MailFollower();
      follower.setRelatedId(entity.getId());
      follower.setRelatedModel(entityClass.getName());
      follower.setUser(user);
      follower.setArchived(false);
      mailFollowerRepository.save(follower);
    }
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

  private String toJSON(Object value) {
    try {
      return objectMapper.writeValueAsString(value);
    } catch (Exception e) {
      log.error("Failed to serialize JSON", e);
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
}
