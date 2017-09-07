/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2017 Axelor (<http://axelor.com>).
 *
 * This program is free software: you can redistribute it and/or  modify
 * it under the terms of the GNU Affero General Public License, version 3,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.axelor.auth;

import static com.axelor.common.StringUtils.isBlank;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.Transaction;

import com.axelor.auth.db.AuditableModel;
import com.axelor.auth.db.User;
import com.axelor.common.Inflector;
import com.axelor.common.StringUtils;
import com.axelor.db.JPA;
import com.axelor.db.annotations.Track;
import com.axelor.db.annotations.TrackEvent;
import com.axelor.db.annotations.TrackField;
import com.axelor.db.annotations.TrackMessage;
import com.axelor.db.mapper.Mapper;
import com.axelor.db.mapper.Property;
import com.axelor.inject.Beans;
import com.axelor.mail.MailConstants;
import com.axelor.mail.db.MailFollower;
import com.axelor.mail.db.MailMessage;
import com.axelor.mail.db.repo.MailFollowerRepository;
import com.axelor.mail.db.repo.MailMessageRepository;
import com.axelor.script.CompositeScriptHelper;
import com.axelor.script.ScriptBindings;
import com.axelor.script.ScriptHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Objects;

/**
 * This class provides change tracking for auditing and notifications.
 *
 */
final class AuditTracker {

	private static final ThreadLocal<Map<String, EntityState>> STORE = new ThreadLocal<>();

	private static class EntityState {

		private AuditableModel entity;

		private Map<String, Object> values;
		private Map<String, Object> oldValues;

		public static void create(AuditableModel entity, Map<String, Object> values, Map<String, Object> oldValues) {
			if (STORE.get() == null) {
				STORE.set(new HashMap<String, EntityState>());
			}
			String key = entity.getClass().getName() + ":" + entity.getId();
			EntityState state = STORE.get().get(key);
			if (state == null) {
				state = new EntityState();
				state.entity = entity;
				state.values = values;
				state.oldValues = oldValues;
				STORE.get().put(key, state);
			} else {
				state.values.putAll(values);
			}
		}
	}

	private ObjectMapper objectMapper;

	private String toJSON(Object value) {
		if (objectMapper == null) {
			objectMapper = Beans.get(ObjectMapper.class);
		}
		try {
			return objectMapper.writeValueAsString(value);
		} catch (Exception e) {}
		return null;
	}

	private Track getTrack(AuditableModel entity) {
		if (entity == null) {
			return null;
		}
		return entity.getClass().getAnnotation(Track.class);
	}

	private boolean hasEvent(TrackEvent[] events, TrackEvent event) {
		for (TrackEvent e : events) {
			if (e == event || e == TrackEvent.ALWAYS) {
				return true;
			}
		}
		return false;
	}

	private boolean hasEvent(Track track, TrackField field, TrackEvent event) {
		return hasEvent(field.on(), event) || (field.on().length == 0 && hasEvent(track.on(), event));
	}

	private boolean hasEvent(Track track, TrackMessage message, TrackEvent event) {
		return hasEvent(message.on(), event) || (message.on().length == 0 && hasEvent(track.on(), event));
	}

	private String format(Property property, Object value) {
		if (value == null) {
			return "";
		}
		if (value == Boolean.TRUE) {
			return "True";
		}
		if (value == Boolean.FALSE) {
			return "False";
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
		return value.toString();
	}

	/**
	 * Record the changes as a notification message.
	 *
	 * @param entity
	 *            the object being tracked
	 * @param names
	 *            the field names
	 * @param state
	 *            current values
	 * @param previousState
	 *            old values
	 */
	public void track(AuditableModel entity, String[] names, Object[] state, Object[] previousState) {

		final Track track = getTrack(entity);
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

		EntityState.create(entity, values, oldValues);
	}
	
	private String findMessage(Track track, TrackMessage[] messages, Map<String, Object> values, Map<String, Object> oldValues, ScriptHelper scriptHelper) {
		for (TrackMessage tm : messages) {
			if (hasEvent(track, tm, oldValues.isEmpty() ? TrackEvent.CREATE : TrackEvent.UPDATE)) {
				boolean matched = tm.fields().length == 0;
				for (String field : tm.fields()) {
					if (isBlank(field)) {
						matched = true;
						break;
					}
					matched = oldValues.isEmpty() ? values.containsKey(field) : !Objects.equal(values.get(field), oldValues.get(field));
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

	private void process(EntityState state, User user) {

		final AuditableModel entity = state.entity;
		final Mapper mapper = Mapper.of(entity.getClass());
		final MailMessage message = new MailMessage();

		final Track track = getTrack(entity);

		final Map<String, Object> values = state.values;
		final Map<String, Object> oldValues = state.oldValues;
		final Map<String, Object> previousState = oldValues.isEmpty() ? null : oldValues;

		final ScriptBindings bindings = new ScriptBindings(state.values);
		final ScriptHelper scriptHelper = new CompositeScriptHelper(bindings);

		final List<Map<String, String>> tags = new ArrayList<>();
		final List<Map<String, String>> tracks = new ArrayList<>();
		final Set<String> tagFields = new HashSet<>();

		// find matched message
		String msg = findMessage(track, track.messages(), values, oldValues, scriptHelper);
		
		// find matched content message
		String content = findMessage(track, track.contents(), values, oldValues, scriptHelper);

		for (TrackField field : track.fields()) {

			if (!hasEvent(track, field, TrackEvent.ALWAYS) &&
				!hasEvent(track, field, previousState == null ? TrackEvent.CREATE: TrackEvent.UPDATE)) {
				continue;
			}

			if (!isBlank(field.condition()) && !scriptHelper.test(field.condition())) {
				continue;
			}

			final String name = field.name();
			final Property property = mapper.getProperty(name);

			String title = property.getTitle();
			if (isBlank(title)) {
				title = Inflector.getInstance().humanize(name);
			}

			final Object value = values.get(name);
			final Object oldValue = oldValues.get(name);

			if (previousState != null && Objects.equal(value, oldValue)) {
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
		for (TrackMessage tm : track.messages()) {
			boolean canTag = tm.fields().length == 0 || (tm.fields().length == 1 && isBlank(tm.fields()[0]));
			for (String name : tm.fields()) {
				if (isBlank(name)) { continue; }
				canTag = tagFields.contains(name);
				if (canTag) { break; }
			}
			if (!canTag) { continue; }
			if (hasEvent(track, tm, previousState == null ? TrackEvent.CREATE: TrackEvent.UPDATE)) {
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

		Beans.get(MailMessageRepository.class).save(message);

		try {
			message.setRelatedName(mapper.getNameField().get(entity).toString());
		} catch (Exception e) {
		}

		if (previousState == null && track.subscribe()) {
			final MailFollower follower = new MailFollower();
			follower.setRelatedId(entity.getId());
			follower.setRelatedModel(entity.getClass().getName());
			follower.setUser(user);
			follower.setArchived(false);
			Beans.get(MailFollowerRepository.class).save(follower);
		}
	}

	/**
	 * This method should be called from
	 * {@link AuditInterceptor#beforeTransactionCompletion(Transaction)} method
	 * to finish change recording.
	 *
	 * @param tx
	 *            the transaction in which the change tracking is being done
	 * @param user
	 *            the session user
	 */
	public void onComplete(Transaction tx, User user) {

		final Map<String, EntityState> store = STORE.get();
		if (store == null) {
			return;
		}
		// prevent concurrent update
		STORE.remove();
		try {
			for (EntityState state : store.values()) {
				process(state, user);
			}
		} finally {
			JPA.em().flush();
		}
	}
}
