/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2018 Axelor (<http://axelor.com>).
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
package com.axelor.db.converters;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.persistence.FlushModeType;
import javax.persistence.TypedQuery;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.axelor.db.JPA;
import com.axelor.db.Model;
import com.axelor.db.Query;
import com.axelor.db.QueryBinder;
import com.axelor.db.mapper.Mapper;
import com.axelor.db.mapper.Property;
import com.axelor.db.mapper.PropertyType;
import com.google.inject.persist.Transactional;

/**
 * The service can be used to migrate all the encrypted field values.
 * 
 * <p>
 * This utility method {@link #migrate()} can be used to migrate encrypted field
 * values if you want to change encryption algorithm or password or salt.
 * 
 * <p>
 * Following encryption settings will be read from
 * <code>application.properties</code> for the migration.
 * 
 * <ul>
 * <li><code>encryption.algorithm.old</code></li> the old algorithm, empty if
 * not set previously
 * <li><code>encryption.password.old</code></li> the old password, empty if not
 * set previously
 * </ul>
 * 
 * and new settings:
 * 
 * <ul>
 * <li><code>encryption.algorithm</code></li> the new algorithm, empty if want
 * to use default
 * <li><code>encryption.password</code></li> the new password (required)
 * </ul>
 * 
 */
public class EncryptedFieldService {

	private static final Logger LOG = LoggerFactory.getLogger(EncryptedFieldService.class);

	@Transactional
	public void migrate() {
		JPA.models().stream().forEach(this::migrate);
	}

	@SuppressWarnings("all")
	@Transactional
	public void migrate(Class<?> model) {
		final List<Property> encrypted = Arrays.stream(Mapper.of(model).getProperties()).filter(Property::isEncrypted)
				.collect(Collectors.toList());

		if (encrypted.isEmpty()) {
			return;
		}

		boolean hasBinary = encrypted.stream().anyMatch(p -> p.getType() == PropertyType.BINARY);
		List<String> names = encrypted.stream().map(Property::getName).collect(Collectors.toList());

		StringBuilder sb = new StringBuilder("SELECT ").append("new Map(")
				.append(names.stream().map(n -> "self." + n + " as " + n).collect(Collectors.joining(", ")))
				.append(") FROM ").append(model.getSimpleName()).append(" self");

		TypedQuery<Map> selectQuery = JPA.em().createQuery(sb.toString(), Map.class);
		TypedQuery<Long> countQuery = JPA.em().createQuery("SELECT COUNT(m.id) FROM " + model.getName() + " m",
				Long.class);

		QueryBinder.of(selectQuery).setFlushMode(FlushModeType.COMMIT);
		QueryBinder.of(countQuery).setFlushMode(FlushModeType.COMMIT);

		Query<?> updater = Query.of(model.asSubclass(Model.class)).autoFlush(false);

		long count = (Long) countQuery.getSingleResult();
		long offset = 0;
		int limit = hasBinary ? 40 : 1000;

		LOG.info("Updating: {}", model.getName());
		LOG.info("Records: {}", count);

		selectQuery.setMaxResults(limit);
		while (offset < count) {
			selectQuery.setFirstResult((int) offset);
			List<Map> values = selectQuery.getResultList();
			LOG.info("Records from: {} to {}", offset, Math.min(count, (offset + limit)));
			offset += limit;
			values.forEach(map -> updater.update(map));
		}
		
		LOG.info("Field encryption complete.");
		LOG.warn("Remove 'encryption.password.old' from 'application.properties'");
	}
}
