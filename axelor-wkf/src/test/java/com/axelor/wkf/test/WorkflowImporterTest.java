/**
 * Axelor Business Solutions
 *
 * Copyright (C) 2012-2014 Axelor (<http://axelor.com>).
 *
 * This program is free software: you can redistribute it and/or  modify
 * it under the terms of the GNU Affero General Public License, version 3,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.axelor.wkf.test;

import javax.inject.Inject;
import javax.xml.bind.JAXBException;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.axelor.db.JPA;
import com.axelor.db.Query;
import com.axelor.meta.db.MetaModel;
import com.axelor.meta.service.MetaModelService;
import com.axelor.test.GuiceModules;
import com.axelor.test.GuiceRunner;
import com.axelor.wkf.WkfTest;
import com.axelor.wkf.db.Node;
import com.axelor.wkf.db.Workflow;
import com.axelor.wkf.db.node.StartEvent;
import com.axelor.wkf.service.WorkflowImporter;

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
				workflow.setMetaModel(Query.of(MetaModel.class).filter("self.fullName = ?1", "com.axelor.wkf.db.Workflow").fetchOne() );
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
		Assert.assertEquals(1, StartEvent.all().count());
		
	}
}
