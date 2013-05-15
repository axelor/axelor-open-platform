package com.axelor.wkf.test;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.xml.bind.JAXBException;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.axelor.db.JPA;
import com.axelor.db.mapper.Mapper;
import com.axelor.meta.service.MetaModelService;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.axelor.test.GuiceModules;
import com.axelor.test.GuiceRunner;
import com.axelor.wkf.WkfTest;
import com.axelor.wkf.action.Action;
import com.axelor.wkf.data.CreateData;
import com.axelor.wkf.db.Workflow;
import com.axelor.wkf.workflow.WorkflowService;

@RunWith(GuiceRunner.class)
@GuiceModules({ WkfTest.class })
public class WorkFlowEngineTest {

	private static Workflow wkf;

	@Inject
	WorkflowService workflowService;

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

		ActionRequest request = new ActionRequest();

		Map<String, Object> data = Mapper.toMap(wkf);
		request.setData(data);
		request.setModel("com.axelor.wkf.db.Workflow");

		Assert.assertNotNull( workflowService.run(wkf).getData() );
		Assert.assertEquals( workflowService.getInstance(wkf, wkf.getId()).getNodes().size(), 1 );

	}

	@Test
	public void actionTest() {

		ActionRequest request = new ActionRequest();

		Map<String, Object> data = Mapper.toMap(wkf);
		request.setData(data);
		request.setModel("com.axelor.wkf.db.Workflow");

		Action actionWorkflow = new Action();

		ActionResponse value = actionWorkflow.execute("action-alert-test", request);

		Assert.assertNotNull(value);
		Assert.assertNotNull(value.getData());
		Assert.assertTrue(value.getData() instanceof List);
		Assert.assertFalse(((List<?>) value.getData()).isEmpty());

	}

}
