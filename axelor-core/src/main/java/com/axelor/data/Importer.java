/**
 * Axelor Business Solutions
 *
 * Copyright (C) 2012-2014 Axelor (<http://axelor.com>).
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
	 * Set global context.
	 *
	 * @param context the global context
	 */
	void setContext(Map<String, Object> context);

	/**
	 * Add a data import event listener.
	 *
	 * @param listener the listener
	 */
	void addListener(Listener listener);

	/**
	 * Clear listeners.
	 *
	 */
	void clearListener();

	/**
	 * Run the specified import task.
	 *
	 * @param task the task to run
	 */
	void runTask(ImportTask task);

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
