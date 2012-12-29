package com.axelor.data;

import com.axelor.db.Model;

public interface Listener {
	
	void imported(Model bean);
	
	void imported(Integer counter);
}
