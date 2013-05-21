package com.axelor.wkf;

import java.util.Map;

import com.axelor.meta.ActionHandler;
import com.axelor.wkf.db.Instance;
import com.axelor.wkf.db.Workflow;

public interface IWorkflow {

	static final String XOR = "xor", AND = "and";

	Instance getInstance(String klass, long id);
	Workflow getWorkflow(String klass);

	Map<Object, Object> run(String klass, ActionHandler actionHandler);
	
}
