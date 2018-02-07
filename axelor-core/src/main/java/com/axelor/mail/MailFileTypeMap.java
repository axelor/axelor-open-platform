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
package com.axelor.mail;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import javax.activation.FileTypeMap;
import javax.activation.MimetypesFileTypeMap;

/**
 * Custom {@link FileTypeMap} implementation.
 *
 * <p>
 * This class uses {@link Files#probeContentType(java.nio.file.Path)} to find
 * file type and fallbacks to {@link MimetypesFileTypeMap} if it can't find the
 * type.
 * </p>
 */
class MailFileTypeMap extends FileTypeMap {

	private final FileTypeMap fallback = new MimetypesFileTypeMap();

	@Override
	public String getContentType(File file) {
		try {
			return Files.probeContentType(file.toPath());
		} catch (IOException e) {
			return fallback.getContentType(file);
		}
	}

	@Override
	public String getContentType(String filename) {
		return fallback.getContentType(filename);
	}
}
