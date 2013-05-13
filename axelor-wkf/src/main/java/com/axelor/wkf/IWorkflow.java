package com.axelor.wkf;

import com.axelor.db.Model;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.axelor.wkf.db.Instance;
import com.axelor.wkf.db.Workflow;

public interface IWorkflow {

	static final String XOR = "xor", AND = "and";
	
	Instance getInstance (Workflow wkf, long id);
	Workflow getWorkflow (Class<?> klass);
	
	ActionResponse run (ActionRequest actionRequest);
	ActionResponse run (Workflow wkf, ActionRequest actionRequest);

	ActionResponse run (Class<Model> klass);
	ActionResponse run (Model bean);
	ActionResponse run (Workflow wkf, Model bean);
	
}
