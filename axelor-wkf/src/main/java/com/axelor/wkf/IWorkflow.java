package com.axelor.wkf;

import com.axelor.db.Model;
import com.axelor.rpc.ActionRequest;
import com.axelor.wkf.db.Instance;
import com.axelor.wkf.db.Workflow;

public interface IWorkflow<T extends Model> {

	static final String XOR = "xor", AND = "and";
	
	Instance getInstance (Workflow wkf, long id);
	Workflow getWorkflow (Class<?> klass);
	
	void run (ActionRequest actionRequest);
	void run (Workflow wkf, ActionRequest actionRequest);

	void run (Class<T> klass);
	void run (T bean);
	void run (Workflow wkf, T bean);
	
}
