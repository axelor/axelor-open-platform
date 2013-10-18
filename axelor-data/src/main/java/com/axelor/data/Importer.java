/**
 * Copyright (c) 2012-2013 Axelor. All Rights Reserved.
 *
 * The contents of this file are subject to the Common Public
 * Attribution License Version 1.0 (the “License”); you may not use
 * this file except in compliance with the License. You may obtain a
 * copy of the License at:
 *
 * http://license.axelor.com/.
 *
 * The License is based on the Mozilla Public License Version 1.1 but
 * Sections 14 and 15 have been added to cover use of software over a
 * computer network and provide for limited attribution for the
 * Original Developer. In addition, Exhibit A has been modified to be
 * consistent with Exhibit B.
 *
 * Software distributed under the License is distributed on an “AS IS”
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 *
 * The Original Code is part of "Axelor Business Suite", developed by
 * Axelor exclusively.
 *
 * The Original Developer is the Initial Developer. The Initial Developer of
 * the Original Code is Axelor.
 *
 * All portions of the code written by Axelor are
 * Copyright (c) 2012-2013 Axelor. All Rights Reserved.
 */
package com.axelor.data;

import java.io.IOException;
import java.util.Map;

import com.axelor.data.adapter.BooleanAdapter;
import com.axelor.data.adapter.DataAdapter;
import com.axelor.data.adapter.JodaAdapter;
import com.axelor.data.adapter.NumberAdapter;
import com.axelor.data.csv.CSVImporter;
import com.google.inject.ImplementedBy;

/**
 * The {@link Importer} interface to run import.
 * @author axelor
 *
 */
@ImplementedBy(CSVImporter.class)
public interface Importer {

	static final int DEFAULT_BATCH_SIZE = 20;

	public static DataAdapter[] defaultAdapters = {
		new DataAdapter("LocalDate", JodaAdapter.class, "type", "LocalDate", "format", "dd/MM/yyyy"),
		new DataAdapter("LocalTime", JodaAdapter.class, "type", "LocalTime", "format", "HH:mm"),
		new DataAdapter("LocalDateTime", JodaAdapter.class, "type", "LocalDateTime", "format", "dd/MM/yyyy HH:mm"),
		new DataAdapter("DateTime", JodaAdapter.class, "type", "DateTime", "format", "dd/MM/yyyy HH:mm"),
		new DataAdapter("Boolean", BooleanAdapter.class, "falsePattern", "(0|f|n|false|no)"),
		new DataAdapter("Number", NumberAdapter.class, "decimalSeparator", ".", "thousandSeparator", ",")
	};

	/**
	 * Run the data import task.
	 *
	 * The file name mappings should be passed to process data from multiple
	 * files. For example:
	 *
	 * <pre>
	 * 	Map<String, String[]) mappings = new HashMap<String, String[])();
	 * 	String[] files = {
	 *  	'so1.csv',
	 *  	'so2.csv',
	 *  };
	 * 	mappings.put("sale.order", files);
	 * 	importer.run(mappings);
	 *
	 * </pre>
	 *
	 * If mappings is empty or null then multifile import configuration is
	 * ignored.
	 *
	 * @param mappings
	 *            file name mappings for multifile data import
	 *
	 * @throws IOException
	 */
	void run(Map<String, String[]> mappings) throws IOException;
}
