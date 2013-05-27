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
				workflow.setName("Workflow Sales Order");
				workflow.setMetaModel( MetaModel.find(24L) );
				workflow.save();
			}

		});

	}

	@Test
	public void testImport() {
		
		String bpmnXml = WorkflowImporter.convertStreamToString( Thread.currentThread().getContextClassLoader().getResourceAsStream("data/OrderBpmn2.0.xml") );
		workflowImporter.run( bpmnXml );
		
		Workflow workflow = Workflow.all().filter("self.name = ?1", "Workflow Sales Order").fetchOne();
		Assert.assertNotNull(workflow);
		Assert.assertNotNull(workflow.getNode());
		Assert.assertNotNull(Node.all().filter("self.type = ?1", "intermediary").fetchOne());
		
	}
}
