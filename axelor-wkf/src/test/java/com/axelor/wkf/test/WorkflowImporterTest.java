package com.axelor.wkf.test;

import javax.inject.Inject;
import javax.xml.bind.JAXBException;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.axelor.db.JPA;
import com.axelor.meta.db.MetaModel;
import com.axelor.meta.service.MetaModelService;
import com.axelor.test.GuiceModules;
import com.axelor.test.GuiceRunner;
import com.axelor.wkf.WkfTest;
import com.axelor.wkf.db.Node;
import com.axelor.wkf.db.Workflow;
import com.axelor.wkf.db.node.StartEvent;
import com.axelor.wkf.workflow.WorkflowImporter;

@RunWith(GuiceRunner.class)
@GuiceModules({ WkfTest.class })
public class WorkflowImporterTest {

	@Inject
	WorkflowImporter workflowImporter;

	@BeforeClass
	public static void setUp() throws JAXBException {

		JPA.runInTransaction(new Runnable() {

			@Override
			public void run() {
				new MetaModelService().process();
				
				Workflow workflow = new Workflow();
				workflow.setName("Test");
				workflow.setMetaModel( MetaModel.all().filter("self.fullName = ?1", "com.axelor.wkf.db.Workflow").fetchOne() );
				workflow.save();
			}

		});

	}

	@Test
	public void testImport() {
		
		Workflow workflow = Workflow.all().filter("self.name = ?1", "Test").fetchOne();
		Assert.assertNotNull(workflow);
		Assert.assertEquals("com.axelor.wkf.db.Workflow", workflow.getMetaModel().getFullName());
		
		String bpmnXml = WorkflowImporter.convertStreamToString( Thread.currentThread().getContextClassLoader().getResourceAsStream("data/OrderBpmn2.0.xml") );
		workflowImporter.run( bpmnXml );
		
		Assert.assertNotNull(workflow.getNode());
		Assert.assertNotNull(Node.all().fetch());
		Assert.assertEquals(1, StartEvent.allStartEvent().count());
		
	}
}
