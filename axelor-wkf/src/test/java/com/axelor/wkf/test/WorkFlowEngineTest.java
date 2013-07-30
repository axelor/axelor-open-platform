package com.axelor.wkf.test;

import java.util.Map;

import javax.inject.Inject;
import javax.xml.bind.JAXBException;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.axelor.db.JPA;
import com.axelor.db.mapper.Mapper;
import com.axelor.meta.ActionHandler;
import com.axelor.meta.service.MetaModelService;
import com.axelor.rpc.ActionRequest;
import com.axelor.test.GuiceModules;
import com.axelor.test.GuiceRunner;
import com.axelor.wkf.WkfTest;
import com.axelor.wkf.data.CreateData;
import com.axelor.wkf.db.Workflow;
import com.axelor.wkf.workflow.WorkflowService;
import com.google.common.collect.Maps;
import com.google.inject.Injector;

@RunWith(GuiceRunner.class)
@GuiceModules({ WkfTest.class })
public class WorkFlowEngineTest {

	private static Workflow wkf;

	@Inject
	WorkflowService workflowService;
	
	@Inject
	Injector injector;
	
	private ActionHandler createHandler(String action, Map<String, Object> context) {
		
		ActionRequest request = new ActionRequest();
		
		Map<String, Object> data = Maps.newHashMap();
		request.setData(data);
		request.setModel("com.axelor.wkf.db.Workflow");
		request.setAction(action);

		data.put("context", context);
		
		return new ActionHandler(request, injector);
	}

	@BeforeClass
	public static void setUp() throws JAXBException {

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

		Map<String, Object> context = Mapper.toMap(wkf);
		ActionHandler actionHandler = createHandler("", context);

		Assert.assertNotNull( workflowService.run(wkf.getClass().getName(), actionHandler) );
		Assert.assertEquals( 1, workflowService.getInstance(wkf.getMetaModel().getFullName(), wkf.getId()).getNodes().size() );

	}

}
