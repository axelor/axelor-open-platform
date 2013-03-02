package com.axelor.db;

import com.axelor.rpc.filter.Filter;

public interface JpaSecurity {
	
	Filter getFilter(String action, Class<? extends Model> model, Object... ids);
	
	void check(String action, Class<? extends Model> model);

	void check(String action, Class<? extends Model> model, Long id);
	
	void check(String action, Model entity);
}
