/**
 * Copyright (c) 2012-2014 Axelor. All Rights Reserved.
 *
 * The contents of this file are subject to the Common Public
 * Attribution License Version 1.0 (the "License"); you may not use
 * this file except in compliance with the License. You may obtain a
 * copy of the License at:
 *
 * http://license.axelor.com/.
 *
 * The License is based on the Mozilla Public License Version 1.1 but
 * Sections 14 and 15 have been added to cover use of software over a
 * computer network and provide for limited attribution for the
 * Original Developer. In addition, Exhibit A has been modified to be
 * consistent with Exhibit B.
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 *
 * The Original Code is part of "Axelor Business Suite", developed by
 * Axelor exclusively.
 *
 * The Original Developer is the Initial Developer. The Initial Developer of
 * the Original Code is Axelor.
 *
 * All portions of the code written by Axelor are
 * Copyright (c) 2012-2014 Axelor. All Rights Reserved.
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
