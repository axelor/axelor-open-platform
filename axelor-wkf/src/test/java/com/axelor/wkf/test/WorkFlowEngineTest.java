package com.axelor.wkf.test;

import java.util.Map;

import javax.inject.Inject;
import javax.xml.bind.JAXBException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.axelor.db.JPA;
import com.axelor.db.mapper.Mapper;
import com.axelor.meta.service.MetaModelService;
import com.axelor.rpc.ActionRequest;
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
	
	private Workflow wkf;
	
	@Inject
	WorkflowFactory<Workflow> workflowFactory;

	@Before
	public void setUp() throws JAXBException{
		
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
		
		ActionRequest request = new ActionRequest();

		Map<String, Object> data = Mapper.toMap(wkf);
		request.setData(data);
		request.setModel("com.axelor.wkf.db.Workflow");
		
		IWorkflow<Workflow> workflow1 = workflowFactory.newEngine();
		
		workflow1.run( wkf );
		Assert.assertEquals( workflow1.getInstance(wkf, wkf.getId() ).getNodes().size(), 1);

	}

}
