package com.axelor.wkf.test;

import javax.inject.Inject;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.axelor.db.JPA;
import com.axelor.meta.service.MetaModelService;
import com.axelor.test.GuiceModules;
import com.axelor.test.GuiceRunner;
import com.axelor.wkf.WkfTest;
import com.axelor.wkf.data.CreateData;
import com.axelor.wkf.db.Workflow;
import com.axelor.wkf.workflow.WorkFlowEngine;

@RunWith(GuiceRunner.class)
@GuiceModules({ WkfTest.class })
public class WorkFlowEngineTest {
	
	@Inject
	private WorkFlowEngine workFlowEngine;
	
	private Workflow wkf;

	@Before
	public void setUp(){
		
		final MetaModelService modelService = new MetaModelService();
		
		JPA.runInTransaction(new Runnable() {

			@Override
			public void run() {

				modelService.process();
				wkf = CreateData.createWorkflow();
				
			}
			
		});
		
	}
	
	@Test
	public void run() {
				
		workFlowEngine.run(wkf);
		
		Assert.assertEquals(workFlowEngine.getInstance(wkf, wkf.getId()).getNodes().size(), 1);
		
	}

}
