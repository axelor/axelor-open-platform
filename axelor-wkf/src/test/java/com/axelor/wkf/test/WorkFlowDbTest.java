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
import com.axelor.wkf.db.node.NodeTask;
import com.axelor.wkf.db.node.StartEvent;

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
		Assert.assertEquals(2, NodeTask.allNodeTask().count());
		Assert.assertEquals(1, EndEvent.allEndEvent().count());
		Assert.assertEquals(4, Node.all().count());
		
	}

}
