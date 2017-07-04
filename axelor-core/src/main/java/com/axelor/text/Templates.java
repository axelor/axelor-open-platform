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
package com.axelor.text;

import java.io.File;
import java.io.IOException;
import java.io.Reader;

/**
 * This interface defines API for template engine integration.
 * 
 */
public interface Templates {

	/**
	 * Create a new {@link Template} instance from the given template text.
	 * 
	 * @param text
	 *            the template text
	 * @return an instance of {@link Template}
	 */
	Template fromText(String text);

	/**
	 * Create a new {@link Template} instance from the given template file.
	 * 
	 * @param file
	 *            the template file
	 * @return an instance of {@link Template}
	 * @throws IOException
	 *             if file read throws {@link IOException}
	 */
	Template from(File file) throws IOException;

	/**
	 * Create a new {@link Template} instance from the given reader.
	 * 
	 * @param reader
	 *            the {@link Reader} to read the template
	 * @return an instance of {@link Template}
	 * @throws IOException
	 *             if reader throws {@link IOException}
	 */
	Template from(Reader reader) throws IOException;
}
