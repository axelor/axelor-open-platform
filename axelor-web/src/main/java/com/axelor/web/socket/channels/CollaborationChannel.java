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
import com.axelor.auth.db.User;
import com.axelor.db.mapper.Mapper;
import com.axelor.db.mapper.Property;
import com.axelor.inject.Beans;
import com.axelor.web.socket.Channel;
import com.axelor.web.socket.Message;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import javax.inject.Singleton;
import javax.websocket.EncodeException;
import javax.websocket.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class CollaborationChannel extends Channel {

  private static final String NAME = "collaboration";

  private static final Multimap<String, Session> ROOMS =
      Multimaps.newMultimap(new ConcurrentHashMap<>(), CopyOnWriteArrayList::new);

  private static final Map<String, Map<String, CollaborationState>> STATES =
      new ConcurrentHashMap<>();

  private static final Logger log = LoggerFactory.getLogger(CollaborationChannel.class);

  @Override
  public String getName() {
    return NAME;
  }

  @Override
  public boolean isEnabled() {
    return AppSettings.get().getBoolean(AvailableAppSettings.VIEW_COLLABORATION, true);
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
      case EDITABLE:
      case DIRTY:
      case SAVE:
        updateState(data);
        break;
      default:
        break;
    }

    broadcast(session, data);
  }

  private CollaborationData getData(Message message) {
    ObjectMapper mapper = Beans.get(ObjectMapper.class);
    return mapper.convertValue(message.getData(), CollaborationData.class);
  }

  private void welcome(Session session, CollaborationData data) {
    ROOMS.put(data.getKey(), session);

    CollaborationData resp = new CollaborationData();
    resp.setModel(data.getModel());
    resp.setRecord(data.getRecord());
    resp.setUser(data.getUser());
    resp.setUsers(getUsers(data.getKey()));

    try {
      this.send(session, resp);
    } catch (IOException | EncodeException e) {
      log.error(e.getMessage(), e);
    }
  }

  private void remove(Session client, String key) {
    remove(client, Collections.singleton(key));
  }

  private void remove(Session client, Collection<String> keys) {
    for (String key : keys) {
      boolean removed = ROOMS.remove(key, client);
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
        data.setRecord(parts[1]);

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

  private void updateState(CollaborationData data) {
    final Map<String, CollaborationState> map = data.getStates();
    final CollaborationState state =
        map.computeIfAbsent(data.getUser().getCode(), key -> new CollaborationState());

    switch (data.getCommand()) {
      case EDITABLE:
        state.setEditable((Boolean) data.getMessage());
        break;
      case DIRTY:
        state.setDirty((Boolean) data.getMessage());
        break;
      case SAVE:
        state.setVersion((Long) data.getMessage());
        break;
      default:
        break;
    }
  }

  public enum CollaborationCommand {
    LEFT,
    JOIN,
    EDIT,
    IDLE,
    EDITABLE,
    DIRTY,
    SAVE,
  }

  @JsonInclude(Include.NON_NULL)
  public static class CollaborationState {

    private Boolean editable;

    private Boolean dirty;

    private Long version;

    private Map<String, Object> changes;

    public Boolean isEditable() {
      return editable;
    }

    public void setEditable(Boolean editable) {
      this.editable = editable;
    }

    public Boolean isDirty() {
      return dirty;
    }

    public void setDirty(Boolean dirty) {
      this.dirty = dirty;
    }

    public Long getVersion() {
      return version;
    }

    public void setVersion(Long version) {
      this.version = version;
    }

    public Map<String, Object> getPendingChanges() {
      return changes;
    }

    public void setPendingChanges(Map<String, Object> pendingChanges) {
      this.changes = pendingChanges;
    }
  }

  @JsonInclude(Include.NON_NULL)
  public static class CollaborationData {

    private String model;

    @JsonProperty("record")
    private String rec;

    private CollaborationCommand command;

    private Object message;

    @JsonSerialize(using = UserSerializer.class)
    private User user;

    @JsonSerialize(using = UserListSerializer.class)
    private List<User> users;

    public String getModel() {
      return model;
    }

    public void setModel(String model) {
      this.model = model;
    }

    public String getRecord() {
      return rec;
    }

    public void setRecord(String rec) {
      this.rec = rec;
    }

    public CollaborationCommand getCommand() {
      return command;
    }

    public void setCommand(CollaborationCommand command) {
      this.command = command;
    }

    @JsonIgnore
    public String getKey() {
      return String.format("%s:%s", model, rec);
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
      return STATES.computeIfAbsent(getKey(), key -> new ConcurrentHashMap<>());
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
      values.put("name", user.getName());
      values.put("code", user.getCode());

      if (user.getEmail() != null) {
        values.put("email", user.getEmail());
      }

      Property nameField = mapper.getNameField();
      if (nameField != null) {
        Object nameValue = nameField.get(user);
        if (nameValue != null) {
          values.put(nameField.getName(), nameValue);
        }
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
