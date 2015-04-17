/**
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2015 Axelor (<http://axelor.com>).
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
import com.axelor.mail.db.MailFollower;
import com.axelor.mail.db.MailMessage;
import com.axelor.script.CompositeScriptHelper;
import com.axelor.script.ScriptBindings;
import com.axelor.script.ScriptHelper;
import com.google.common.base.Objects;

/**
 * This class provides change tracking for auditing and notifications.
 *
 */
final class AuditTracker {

	private static final ThreadLocal<List<Model>> PENDING = new ThreadLocal<>();

	private void add(Model entity) {
		if (PENDING.get() == null) {
			PENDING.set(new ArrayList<Model>());
		}
		PENDING.get().add(entity);
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

		final StringBuilder builder = new StringBuilder();
		final Map<String, String> tags = new LinkedHashMap<>();

		String msg = previousState == null ?
				I18n.get("Record created") :
				I18n.get("Record updated");

		// find first matched message
		for (TrackMessage tm : track.messages()) {
			if (hasEvent(tm, TrackEvent.ALWAYS) ||
				hasEvent(tm, previousState == null ? TrackEvent.CREATE: TrackEvent.UPDATE)) {
				if (isBlank(tm.tag()) && scriptHelper.test(tm.condition())) {
					msg = tm.message();
					break;
				}
			}
		}

		// find matched tags
		for (TrackMessage tm : track.messages()) {
			if (hasEvent(tm, TrackEvent.ALWAYS) ||
				hasEvent(tm, previousState == null ? TrackEvent.CREATE: TrackEvent.UPDATE)) {
				if (!isBlank(tm.tag()) && scriptHelper.test(tm.condition())) {
					tags.put(tm.message(), tm.tag());
				}
			}
		}

		builder.append("<div class='track-container'>");
		builder.append("<span class='track-message'>").append(msg).append("</span>");

		if (track.fields().length > 0) {
			builder.append("<ul class='track-fields'>");
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
				title = Inflector.getInstance().titleize(name);
			}

			final Object value = values.get(name);
			final Object oldValue = oldValues.get(name);

			String dispayValue = format(property, value);
			if (previousState != null) {
				if (Objects.equal(value, oldValue)) {
					continue;
				}
				if (oldValue != null) {
					dispayValue = format(property, oldValue) + " &raquo; " + dispayValue;
				}
			}

			builder
			.append("<li>")
			.append("<strong>").append(title).append("</strong>: ").append(dispayValue)
			.append("</li>");
		}

		if (track.fields().length > 0) {
			builder.append("</ul>");
		}

		builder.append("<div class='track-tags'>");
		for (String tag : tags.keySet()) {
			builder.append("<span class='label label-" + tags.get(tag) + "'>").append(tag).append("</span>");
		}
		builder.append("</div>"); // track-tags
		builder.append("</div>"); // track-container

		message.setBody(builder.toString());
		message.setAuthor(user);
		message.setRelatedId(entity.getId());
		message.setRelatedModel(entity.getClass().getName());
		add(message);

		try {
			message.setRelatedName(mapper.getNameField().get(entity).toString());
		} catch (Exception e) {
		}

		if (previousState == null) {
			final MailFollower follower = new MailFollower();
			follower.setRelatedId(entity.getId());
			follower.setRelatedModel(entity.getClass().getName());
			follower.setUser(user);
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
			for (Model message : pending) {
				JPA.em().persist(message);
			}
		} finally {
			JPA.em().flush();
			PENDING.remove();
		}
	}
}
