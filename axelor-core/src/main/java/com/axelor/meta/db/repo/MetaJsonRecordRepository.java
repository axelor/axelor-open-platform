/**
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
package com.axelor.meta.db.repo;

import com.axelor.db.JpaRepository;
import com.axelor.inject.Beans;
import com.axelor.meta.db.MetaJsonModel;
import com.axelor.meta.db.MetaJsonRecord;
import com.axelor.rpc.JsonContext;

/**
 * Repository implementation to handle custom model records.
 *
 */
public class MetaJsonRecordRepository extends JpaRepository<MetaJsonRecord> {

	/**
	 * Create new instance of the {@link MetaJsonRecordRepository}.
	 * 
	 */
	public MetaJsonRecordRepository() {
		super(MetaJsonRecord.class);
	}
	
	@Override
	public MetaJsonRecord save(MetaJsonRecord entity) {
		final MetaJsonModelRepository models = Beans.get(MetaJsonModelRepository.class);
		final MetaJsonModel model = models.findByName(entity.getJsonModel());
		// set name value
		if (model != null && model.getNameField() != null) {
			entity.setName((String) new JsonContext(entity).get(model.getNameField()));
		}
		return super.save(entity);
	}
}
