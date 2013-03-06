package com.axelor.wkf.workflow;

import javax.inject.Inject;
import javax.inject.Provider;

import com.axelor.db.Model;
import com.axelor.wkf.IWorkflow;
import com.google.inject.Singleton;

@Singleton
public class WorkflowFactory <T extends Model> {
	
	Provider< WorkflowService<T> > workflowEngineprovider;
	
	@Inject
	public WorkflowFactory( Provider< WorkflowService<T> > workflowEngineprovider ) {
		System.out.println("TEST");
		this.workflowEngineprovider = workflowEngineprovider;
		
	}
	
	public IWorkflow<T> newEngine() { return workflowEngineprovider.get(); }
	
}
