package com.axelor.data;

import java.io.IOException;
import java.util.Map;

import com.axelor.data.csv.CSVImporter;
import com.google.inject.ImplementedBy;

@ImplementedBy(CSVImporter.class)
public interface Importer {
	
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
