package com.axelor.wkf;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.axelor.db.JPA;
import com.axelor.meta.service.MetaModelService;
import com.axelor.test.GuiceModules;
import com.axelor.test.GuiceRunner;
import com.axelor.wkf.data.CreateData;
import com.axelor.wkf.db.Workflow;
import com.axelor.wkf.service.WkfService;

@RunWith(GuiceRunner.class)
@GuiceModules({ WkfTest.class })
public class TestRunner {
	
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
		
		WkfService wkfService = new WkfService();
		
		WkfRunner.runBean(wkf);
		
		Assert.assertEquals(wkfService.getInstance(wkf, wkf.getId()).getNodes().size(), 1);
		
	}

}
