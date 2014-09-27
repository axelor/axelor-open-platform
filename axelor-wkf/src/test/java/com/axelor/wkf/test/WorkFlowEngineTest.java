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
import com.axelor.wkf.service.WorkflowService;
import com.google.common.collect.Maps;

@RunWith(GuiceRunner.class)
@GuiceModules({ WkfTest.class })
public class WorkFlowEngineTest {

	private static Workflow wkf;

	@Inject
	private WorkflowService workflowService;
	
	@Inject
	private ActionHandler handler;
	
	private ActionHandler createHandler(String action, Map<String, Object> context) {
		
		ActionRequest request = new ActionRequest();
		
		Map<String, Object> data = Maps.newHashMap();
		request.setData(data);
		request.setModel("com.axelor.wkf.db.Workflow");
		request.setAction(action);

		data.put("context", context);
		
		return handler.forRequest(request);
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
