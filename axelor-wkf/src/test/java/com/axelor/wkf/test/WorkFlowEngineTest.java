package com.axelor.wkf.test;

import javax.inject.Inject;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.axelor.db.JPA;
import com.axelor.meta.service.MetaModelService;
import com.axelor.test.GuiceModules;
import com.axelor.test.GuiceRunner;
import com.axelor.wkf.IWorkflow;
import com.axelor.wkf.WkfTest;
import com.axelor.wkf.data.CreateData;
import com.axelor.wkf.db.Workflow;
import com.axelor.wkf.workflow.WorkflowFactory;

@RunWith(GuiceRunner.class)
@GuiceModules({ WkfTest.class })
public class WorkFlowEngineTest {
	
	private static Workflow wkf;
	
	@Inject
	WorkflowFactory<Workflow> workflowFactory;

	@BeforeClass
	public static void setUp(){
		
		JPA.runInTransaction(new Runnable() {
						
			@Override
			public void run() {
				new MetaModelService().process();
				wkf = CreateData.createWorkflow();
			}
			
		});
		
	}
	
	@Test
	public void run() {

		IWorkflow<Workflow> iWorkflow1 = workflowFactory.newEngine();
		
		iWorkflow1.run(wkf);
		Assert.assertEquals(iWorkflow1.getInstance(wkf, wkf.getId()).getNodes().size(), 1);

		workflowFactory.newEngine();
		workflowFactory.newEngine();
		workflowFactory.newEngine();
	}

}
