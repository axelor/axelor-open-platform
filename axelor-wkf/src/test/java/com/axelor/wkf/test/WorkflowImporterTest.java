/**
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2014 Axelor (<http://axelor.com>).
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

import com.axelor.db.JpaSupport;
import com.axelor.inject.Beans;
import com.axelor.meta.db.repo.MetaModelRepository;
import com.axelor.meta.service.MetaModelService;
import com.axelor.test.GuiceModules;
import com.axelor.test.GuiceRunner;
import com.axelor.wkf.WkfTest;
import com.axelor.wkf.db.Node;
import com.axelor.wkf.db.StartEvent;
import com.axelor.wkf.db.Workflow;
import com.axelor.wkf.db.repo.WorkflowRepository;
import com.axelor.wkf.service.WorkflowImporter;
import com.google.inject.persist.Transactional;

@RunWith(GuiceRunner.class)
@GuiceModules({ WkfTest.class })
public class WorkflowImporterTest extends JpaSupport {

	@Inject
	WorkflowImporter workflowImporter;

	@BeforeClass
	@Transactional
	public static void setUp() throws JAXBException {
				
		new MetaModelService().process();

		WorkflowRepository workflows = Beans.get(WorkflowRepository.class);
		MetaModelRepository models = Beans.get(MetaModelRepository.class);
		Workflow workflow = new Workflow();
		workflow.setName("Test");
		workflow.setMetaModel(models.all().filter("self.fullName = ?1", "com.axelor.wkf.db.Workflow").fetchOne());
		workflows.save(workflow);
	}

	@Test
	public void testImport() {
		
		Workflow workflow = all(Workflow.class).filter("self.name = ?1", "Test").fetchOne();
		Assert.assertNotNull(workflow);
		Assert.assertEquals("com.axelor.wkf.db.Workflow", workflow.getMetaModel().getFullName());
		
		String bpmnXml = WorkflowImporter.convertStreamToString( Thread.currentThread().getContextClassLoader().getResourceAsStream("data/OrderBpmn2.0.xml") );
		workflowImporter.run( bpmnXml );

		Assert.assertNotNull(workflow.getNode());
		Assert.assertNotNull(all(Node.class).fetch());
		Assert.assertEquals(1, all(StartEvent.class).count());
	}
}
