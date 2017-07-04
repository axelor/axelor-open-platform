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
package com.axelor.db.search;

import java.lang.annotation.ElementType;

import org.hibernate.search.annotations.Factory;
import org.hibernate.search.annotations.Store;
import org.hibernate.search.cfg.SearchMapping;

import com.axelor.auth.db.User;
import com.axelor.dms.db.DMSFile;
import com.axelor.meta.db.MetaFile;

/**
 * The factory to configure indexed entities programmatically.
 * 
 */
public final class SearchMappingFactory {

	@Factory
	public SearchMapping get() {

		final SearchMapping mapping = new SearchMapping();
		
		mapping.entity(User.class)
			.indexed().indexName("users")
			.property("code", ElementType.FIELD).field()
			.property("name", ElementType.FIELD).field().sortableField()
			.property("email", ElementType.FIELD).field();

		// meta file itself is not indexed but embedded in DMSFile index
		mapping.entity(MetaFile.class)
			.property("fileName", ElementType.FIELD).field().store(Store.YES).sortableField()
			.property("filePath", ElementType.FIELD).field().bridge(MetaFileBridge.class)
			.property("description", ElementType.FIELD).field();

		mapping.entity(DMSFile.class)
			.indexed().indexName("files")
			.property("fileName", ElementType.FIELD).field().store(Store.YES).sortableField()
			.property("relatedModel", ElementType.FIELD).field()
			.property("relatedId", ElementType.FIELD).field().store(Store.YES)
			.property("metaFile", ElementType.FIELD).indexEmbedded()
			.property("content", ElementType.FIELD).field();

		// get more mappings
		SearchSupport.get().contribute(mapping);

		return mapping;
	}
}
