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
package com.axelor.meta;

import java.nio.file.Path;
import java.nio.file.Paths;

import com.axelor.app.AppSettings;
import com.axelor.meta.db.MetaFile;
import com.google.common.base.Preconditions;

public final class MetaFiles {

	private static final String DEFAULT_UPLOAD_PATH = "{java.io.tmpdir}/axelor/attachments";
	private static final String UPLOAD_PATH = AppSettings.get().getPath("file.upload.dir", DEFAULT_UPLOAD_PATH);
	
	private MetaFiles() {
		
	}
	
	public static Path getPath(MetaFile file) {
		Preconditions.checkNotNull(file, "file instance can't be null");
		return Paths.get(UPLOAD_PATH, file.getFilePath());
	}
}
