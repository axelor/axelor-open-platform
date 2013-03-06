package com.axelor.wkf;

import com.axelor.db.Model;
import com.axelor.wkf.db.Instance;
import com.axelor.wkf.db.Workflow;

public interface IWorkflow<T extends Model> {

	Instance getInstance (Workflow wkf, long id);
	Workflow getWorkflow (Class<? extends Model> klass);
	
	void run (Class<T> klass);
	void run (T bean);
	void run (Workflow wkf, T bean);
	
}
