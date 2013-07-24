package com.axelor.wkf.test;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.axelor.db.JPA;
import com.axelor.meta.service.MetaModelService;
import com.axelor.test.GuiceModules;
import com.axelor.test.GuiceRunner;
import com.axelor.wkf.WkfTest;
import com.axelor.wkf.data.CreateData;
import com.axelor.wkf.db.Node;
import com.axelor.wkf.db.node.EndEvent;
import com.axelor.wkf.db.node.StartEvent;
import com.axelor.wkf.db.node.Task;

@RunWith(GuiceRunner.class)
@GuiceModules({ WkfTest.class })
public class WorkFlowDbTest {

	@Test
	public void test() {
		
		JPA.runInTransaction(new Runnable() {

			@Override
			public void run() {
				new MetaModelService().process();
				CreateData.createWorkflow();
			}

		});
		
		Assert.assertEquals(1, StartEvent.allStartEvent().count());
		Assert.assertEquals(2, Task.allTask().count());
		Assert.assertEquals(1, EndEvent.allEndEvent().count());
		Assert.assertEquals(4, Node.all().count());
		
	}

}
