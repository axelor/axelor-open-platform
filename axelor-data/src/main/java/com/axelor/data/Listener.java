package com.axelor.data;

import com.axelor.db.Model;

/**
 * Listener interface provides some events fired by Importer.
 *
 */
public interface Listener {
	
	/**
	 * Invoked when line is imported
	 * @param bean
	 */
	void imported(Model bean);
	
	/**
	 * Invoked when file is imported
	 * @param counter
	 */
	void imported(Integer counter);
}
