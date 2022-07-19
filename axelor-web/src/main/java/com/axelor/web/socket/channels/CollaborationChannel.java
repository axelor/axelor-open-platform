/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2022 Axelor (<http://axelor.com>).
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
package com.axelor.web.socket.channels;

import com.axelor.app.AppSettings;
import com.axelor.app.AvailableAppSettings;
import com.axelor.auth.db.Group;
import com.axelor.auth.db.User;
import com.axelor.db.mapper.Mapper;
import com.axelor.db.mapper.Property;
import com.axelor.inject.Beans;
import com.axelor.web.socket.Channel;
import com.axelor.web.socket.Message;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import javax.inject.Singleton;
import javax.websocket.EncodeException;
import javax.websocket.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class CollaborationChannel extends Channel {

  private static final String NAME = "collaboration";

  // <key, set of sessions>
  private static final Multimap<String, Session> ROOMS =
      Multimaps.newSetMultimap(new ConcurrentHashMap<>(), ConcurrentHashMap::newKeySet);

  // <key, <user code, state>>
  private static final Map<String, Map<String, CollaborationState>> STATES =
      new ConcurrentHashMap<>();

  private static final String CAN_VIEW_COLLABORATION = "canViewCollaboration";

  private static final Logger logger = LoggerFactory.getLogger(CollaborationChannel.class);

  @Override
  public String getName() {
    return NAME;
  }

  @Override
  public boolean isEnabled() {
    return AppSettings.get().getBoolean(AvailableAppSettings.VIEW_COLLABORATION, true);
  }

  @Override
  public void onSubscribe(Session session) {
    final boolean canViewCollaboration =
        Optional.of(getUser(session))
            .map(User::getGroup)
            .map(Group::getCanViewCollaboration)
            .orElse(false);
    session.getUserProperties().put(CAN_VIEW_COLLABORATION, canViewCollaboration);
  }

  @Override
  public void onUnsubscribe(Session session) {
    Set<String> keys = ROOMS.keySet();
    remove(session, keys);
  }

  @Override
  public void onMessage(Session session, Message message) {
    CollaborationData data = getData(message);
    User user = getUser(session);
    data.setUser(user);

    switch (data.getCommand()) {
      case JOIN:
        welcome(session, data);
        break;
      case LEFT:
        remove(session, data.getKey());
        return;
      case STATE:
        updateState(data);
        break;
      default:
        break;
    }

    broadcast(session, data);
  }

  @Override
  public void send(Session session, Object data) throws IOException, EncodeException {
    if (canViewCollaboration(session)) {
      super.send(session, data);
    }
  }

  private boolean canViewCollaboration(Session session) {
    return Boolean.TRUE.equals(session.getUserProperties().get(CAN_VIEW_COLLABORATION));
  }

  private CollaborationData getData(Message message) {
    ObjectMapper mapper = Beans.get(ObjectMapper.class);
    return mapper.convertValue(message.getData(), CollaborationData.class);
  }

  private void welcome(Session session, CollaborationData data) {
    ROOMS.put(data.getKey(), session);
    getState(data);

    final CollaborationData resp = new CollaborationData();
    resp.setModel(data.getModel());
    resp.setRecordId(data.getRecordId());
    resp.setUser(data.getUser());
    final String key = data.getKey();
    resp.setUsers(getUsers(key));
    resp.setStates(getStates(key));

    updateState(data);

    try {
      this.send(session, resp);
    } catch (IOException | EncodeException e) {
      logger.error(e.getMessage(), e);
    }
  }

  private void remove(Session client, String key) {
    remove(client, Collections.singleton(key));
  }

  private void remove(Session client, Collection<String> keys) {
    for (final String key : keys) {
      final boolean removed = ROOMS.remove(key, client);
      final Map<String, CollaborationState> states = STATES.get(key);

      if (states != null) {
        states.remove(client.getUserPrincipal().getName());
      }

      if (ROOMS.get(key).isEmpty()) {
        ROOMS.removeAll(key);
        STATES.remove(key);
      }

      if (removed) {
        CollaborationData data = new CollaborationData();
        String[] parts = key.split(":");

        data.setCommand(CollaborationCommand.LEFT);
        data.setUser(getUser(client));
        data.setModel(parts[0]);
        data.setRecordId(Long.valueOf(parts[1]));

        broadcast(client, data);
      }
    }
  }

  private void broadcast(Session session, CollaborationData data) {
    String key = data.getKey();
    for (Session client : ROOMS.get(key)) {
      if (client == session) {
        continue;
      }
      try {
        send(client, data);
      } catch (EncodeException e) {
        // ignore
      } catch (IOException | IllegalStateException e) {
        remove(client, key);
        try {
          client.close();
        } catch (IOException e1) {
          // ignore
        }
      }
    }
  }

  private List<User> getUsers(String key) {
    return ROOMS.get(key).stream().map(this::getUser).collect(Collectors.toList());
  }

  public Map<String, CollaborationState> getStates(String key) {
    return STATES.computeIfAbsent(key, k -> new ConcurrentHashMap<>());
  }

  private CollaborationState getState(CollaborationData data) {
    final Map<String, CollaborationState> map = getStates(data.getKey());
    return map.computeIfAbsent(data.getUser().getCode(), key -> new CollaborationState());
  }

  private void updateState(CollaborationData data) {
    @SuppressWarnings("unchecked")
    final Map<String, Object> message = (Map<String, Object>) data.getMessage();
    if (message == null) {
      return;
    }

    final CollaborationState state = getState(data);

    final Boolean editable = (Boolean) message.get("editable");
    if (editable != null) {
      state.setEditable(editable);
    }

    final Boolean dirty = (Boolean) message.get("dirty");
    if (dirty != null) {
      state.setDirty(dirty);
    }

    final Number version = (Number) message.get("version");
    if (version != null) {
      state.setVersion(version.longValue());
    }
  }

  public enum CollaborationCommand {
    LEFT,
    JOIN,
    STATE
  }

  @JsonInclude(Include.NON_NULL)
  public static class CollaborationState {

    private final LocalDateTime joinDate = LocalDateTime.now();

    private Boolean editable;

    private LocalDateTime editableDate;

    private Boolean dirty;

    private LocalDateTime dirtyDate;

    private Long version;

    private LocalDateTime versionDate;

    public LocalDateTime getJoinDate() {
      return joinDate;
    }

    public Boolean isEditable() {
      return editable;
    }

    public void setEditable(Boolean editable) {
      this.editable = editable;
      editableDate = LocalDateTime.now();
    }

    public LocalDateTime getEditableDate() {
      return editableDate;
    }

    public Boolean isDirty() {
      return dirty;
    }

    public void setDirty(Boolean dirty) {
      this.dirty = dirty;
      dirtyDate = LocalDateTime.now();
    }

    public LocalDateTime getDirtyDate() {
      return dirtyDate;
    }

    public Long getVersion() {
      return version;
    }

    public void setVersion(Long version) {
      this.version = version;
      versionDate = LocalDateTime.now();
      setDirty(false);
    }

    public LocalDateTime getVersionDate() {
      return versionDate;
    }
  }

  @JsonInclude(Include.NON_NULL)
  public static class CollaborationData {

    private String model;

    private Long recordId;

    private CollaborationCommand command;

    private Object message;

    @JsonSerialize(using = UserSerializer.class)
    private User user;

    @JsonSerialize(using = UserListSerializer.class)
    private List<User> users;

    private Map<String, CollaborationState> states;

    public String getModel() {
      return model;
    }

    public void setModel(String model) {
      this.model = model;
    }

    public Long getRecordId() {
      return recordId;
    }

    public void setRecordId(Long recordId) {
      this.recordId = recordId;
    }

    public CollaborationCommand getCommand() {
      return command;
    }

    public void setCommand(CollaborationCommand command) {
      this.command = command;
    }

    @JsonIgnore
    public String getKey() {
      return String.format("%s:%d", model, recordId);
    }

    public Object getMessage() {
      return message;
    }

    public void setMessage(Object message) {
      this.message = message;
    }

    public User getUser() {
      return user;
    }

    public void setUser(User user) {
      this.user = user;
    }

    public List<User> getUsers() {
      return users;
    }

    public void setUsers(List<User> users) {
      this.users = users;
    }

    public Map<String, CollaborationState> getStates() {
      return states;
    }

    public void setStates(Map<String, CollaborationState> states) {
      this.states = states;
    }
  }

  public static class UserSerializer extends JsonSerializer<User> {

    @Override
    public void serialize(User value, JsonGenerator gen, SerializerProvider serializers)
        throws IOException {
      gen.writeObject(toMap(value));
    }

    public static Map<String, Object> toMap(User user) {
      Mapper mapper = Mapper.of(User.class);
      Map<String, Object> values = new HashMap<>();

      values.put("id", user.getId());
      values.put("code", user.getCode());

      Property nameField = mapper.getNameField();
      if (nameField != null) {
        Object nameValue = nameField.get(user);
        if (nameValue != null) {
          values.put(nameField.getName(), nameValue);
        }
      } else {
        values.put("name", user.getName());
      }

      if (user.getImage() != null) {
        values.put(
            "$avatar",
            String.format(
                "ws/rest/%s/%d/image/download?image=true&v=%d",
                User.class.getName(), user.getId(), user.getVersion()));
      }

      final boolean canViewCollaboration =
          Optional.of(user).map(User::getGroup).map(Group::getCanViewCollaboration).orElse(false);

      if (!canViewCollaboration) {
        values.put('$' + CAN_VIEW_COLLABORATION, canViewCollaboration);
      }

      return values;
    }
  }

  public static class UserListSerializer extends JsonSerializer<List<User>> {

    @Override
    public void serialize(List<User> value, JsonGenerator gen, SerializerProvider serializers)
        throws IOException {
      gen.writeObject(value.stream().map(UserSerializer::toMap));
    }
  }
}
