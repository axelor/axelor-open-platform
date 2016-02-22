/**
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2016 Axelor (<http://axelor.com>).
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
import com.axelor.db.JPA;
import com.axelor.db.Model;
import com.axelor.db.annotations.Track;
import com.axelor.db.annotations.TrackEvent;
import com.axelor.db.annotations.TrackField;
import com.axelor.db.annotations.TrackMessage;
import com.axelor.db.mapper.Mapper;
import com.axelor.db.mapper.Property;
import com.axelor.i18n.I18n;
import com.axelor.inject.Beans;
import com.axelor.mail.MailConstants;
import com.axelor.mail.db.MailFollower;
import com.axelor.mail.db.MailMessage;
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

	private static final ThreadLocal<List<Model>> PENDING = new ThreadLocal<>();

	private ObjectMapper objectMapper;

	private void add(Model entity) {
		if (PENDING.get() == null) {
			PENDING.set(new ArrayList<Model>());
		}
		PENDING.get().add(entity);
	}

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

	private boolean hasEvent(TrackField field, TrackEvent event) {
		for (TrackEvent e : field.on()) {
			if (e == event) {
				return true;
			}
		}
		return false;
	}

	private boolean hasEvent(TrackMessage message, TrackEvent event) {
		for (TrackEvent e : message.on()) {
			if (e == event) {
				return true;
			}
		}
		return false;
	}

	private String format(Property property, Object value) {
		if (value == null) {
			return "";
		}
		if (value == Boolean.TRUE) {
			return I18n.get("True");
		}
		if (value == Boolean.FALSE) {
			return I18n.get("False");
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
	 * @param user
	 *            the user who initiated the change
	 * @param entity
	 *            the object being tracked
	 * @param names
	 *            the field names
	 * @param state
	 *            current values
	 * @param previousState
	 *            old values
	 */
	public void track(User user, AuditableModel entity, String[] names, Object[] state, Object[] previousState) {

		final Track track = getTrack(entity);
		if (track == null) {
			return;
		}

		final Mapper mapper = Mapper.of(entity.getClass());
		final MailMessage message = new MailMessage();

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

		final ScriptBindings bindings = new ScriptBindings(values);
		final ScriptHelper scriptHelper = new CompositeScriptHelper(bindings);

		final List<Map<String, String>> tags = new ArrayList<>();
		final List<Map<String, String>> tracks = new ArrayList<>();
		final Set<String> tagFields = new HashSet<>();

		String msg = previousState == null ?
				I18n.get("Record created") :
				I18n.get("Record updated");

		// find first matched message
		for (TrackMessage tm : track.messages()) {
			if (hasEvent(tm, TrackEvent.ALWAYS) ||
				hasEvent(tm, previousState == null ? TrackEvent.CREATE : TrackEvent.UPDATE)) {
				boolean matched = tm.fields().length == 0;
				for (String field : tm.fields()) {
					if (isBlank(field)) {
						matched = true;
						break;
					}
					matched = previousState != null && !Objects.equal(values.get(field), oldValues.get(field));
					if (matched) {
						break;
					}
				}
				if (matched && isBlank(tm.tag()) && scriptHelper.test(tm.condition())) {
					msg = tm.message();
					break;
				}
			}
		}

		for (TrackField field : track.fields()) {

			if (!hasEvent(field, TrackEvent.ALWAYS) &&
				!hasEvent(field, previousState == null ? TrackEvent.CREATE: TrackEvent.UPDATE)) {
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
			if (hasEvent(tm, TrackEvent.ALWAYS) ||
				hasEvent(tm, previousState == null ? TrackEvent.CREATE: TrackEvent.UPDATE)) {
				if (!isBlank(tm.tag()) && scriptHelper.test(tm.condition())) {
					final Map<String, String> item = new HashMap<>();
					item.put("title", tm.message());
					item.put("style", tm.tag());
					tags.add(item);
				}
			}
		}

		final Map<String, Object> json = new HashMap<>();
		json.put("title", msg);
		json.put("tags", tags);
		json.put("tracks", tracks);

		message.setSubject(msg);
		message.setBody(toJSON(json));
		message.setAuthor(user);
		message.setRelatedId(entity.getId());
		message.setRelatedModel(entity.getClass().getName());
		message.setType(MailConstants.MESSAGE_TYPE_NOTIFICATION);
		add(message);

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
			add(follower);
		}
	}

	/**
	 * This method should be called from
	 * {@link AuditInterceptor#beforeTransactionCompletion(Transaction)} method
	 * to finish change recording.
	 *
	 * @param tx
	 *            the transaction in which the change tracking is being done
	 */
	public void onComplete(Transaction tx) {
		final List<Model> pending = PENDING.get();
		if (pending == null) {
			return;
		}
		// prevent concurrent update
		PENDING.remove();
		try {
			for (Model entity : pending) {
				if (entity instanceof MailMessage) {
					Beans.get(MailMessageRepository.class).save((MailMessage) entity);
				} else {
					JPA.em().persist(entity);
				}
			}
		} finally {
			JPA.em().flush();
			PENDING.remove();
		}
	}
}
