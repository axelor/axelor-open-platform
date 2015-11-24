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
package com.axelor.db.internal;

import org.hibernate.event.spi.PreInsertEvent;
import org.hibernate.event.spi.PreInsertEventListener;

import com.axelor.db.EntityHelper;
import com.axelor.db.JpaSequence;
import com.axelor.db.Model;
import com.axelor.db.mapper.Mapper;
import com.axelor.db.mapper.Property;
import com.axelor.meta.db.MetaSequence;

public class SequenceUpdateListener implements PreInsertEventListener {

	private static final long serialVersionUID = 1L;

	@Override
	public boolean onPreInsert(PreInsertEvent event) {
		final Object entity = event.getEntity();
		if ((entity instanceof MetaSequence) || !(entity instanceof Model)) {
			return false;
		}

		final Mapper mapper = Mapper.of(EntityHelper.getEntityClass(entity));
		for (Property property : mapper.getSequenceFields()) {
			if (property.isSequence() && property.get(entity) == null) {
				property.set(entity, JpaSequence.nextValue(property.getSequenceName()));
			}
		}

		return false;
	}
}
