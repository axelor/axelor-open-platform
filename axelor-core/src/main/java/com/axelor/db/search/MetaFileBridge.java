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
package com.axelor.db.search;

import org.apache.lucene.document.Document;
import org.hibernate.search.bridge.LuceneOptions;
import org.hibernate.search.bridge.builtin.TikaBridge;

import com.axelor.common.StringUtils;
import com.axelor.meta.MetaFiles;
import com.axelor.meta.db.MetaFile;

/**
 * Custom {@link TikaBridge} implementation to be used with {@link MetaFile} to
 * resolve correct filePath.
 *
 */
public class MetaFileBridge extends TikaBridge {

	@Override
	public void set(String name, Object value, Document document, LuceneOptions luceneOptions) {
		String filePath = null;
		if (value instanceof String) {
			filePath = (String) value;
		} else if (value instanceof MetaFile) {
			filePath = ((MetaFile) value).getFilePath();
		}
		filePath = StringUtils.isBlank(filePath) ? null : MetaFiles.getPath(filePath).toAbsolutePath().toString();
		super.set(name, filePath, document, luceneOptions);
	}
}
