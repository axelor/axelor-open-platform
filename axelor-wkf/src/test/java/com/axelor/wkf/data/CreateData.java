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
package com.axelor.wkf.data;

import java.util.ArrayList;

import com.axelor.inject.Beans;
import com.axelor.meta.db.MetaAction;
import com.axelor.meta.db.repo.MetaActionRepository;
import com.axelor.meta.service.MetaModelService;
import com.axelor.wkf.db.Node;
import com.axelor.wkf.db.Transition;
import com.axelor.wkf.db.Workflow;
import com.axelor.wkf.db.node.EndEvent;
import com.axelor.wkf.db.node.NodeTask;
import com.axelor.wkf.db.node.StartEvent;

public class CreateData {

	public static Workflow createWorkflow() {

		final MetaActionRepository actions = Beans.get(MetaActionRepository.class);
		
		Transition startTransition = new Transition();
		startTransition.setName("startTransition");
		startTransition.setSequence(0);
		
		Transition intermediaryTransition = new Transition();
		intermediaryTransition.setName("intermediaryTransition");
		intermediaryTransition.setSequence(0);
		
		Transition endTransition = new Transition();
		endTransition.setName("endTransition");
		endTransition.setSequence(0);
		
		MetaAction metaAction1 = new MetaAction();
		metaAction1.setName("action-test-1");
		metaAction1.setXml("<action-record name=\"action-test-1\" model=\"com.axelor.wkf.db.Workflow\"><field name=\"archived\" expr=\"eval: true\"/></action-record>");
		metaAction1.setType("action-record");
		actions.save(metaAction1);
		
		MetaAction metaAction2 = new MetaAction();
		metaAction2.setName("action-test-2");
		metaAction2.setXml("<action-record name=\"action-test-2\" model=\"com.axelor.wkf.db.Workflow\"><field name=\"active\" expr=\"eval: !archived\"/></action-record>");
		metaAction2.setType("action-record");
		actions.save(metaAction2);
		
		MetaAction metaAction3 = new MetaAction();
		metaAction3.setName("action-test-3");
		metaAction3.setXml("<action-attrs name=\"action-test-3\" ><attribute name=\"hidden\" for=\"archived\" expr=\"eval: true\"/></action-attrs>");
		metaAction3.setType("action-attrs");
		actions.save(metaAction3);
		
		MetaAction metaAction4 = new MetaAction();
		metaAction4.setName("action-test-4");
		metaAction4.setXml("<action-attrs name=\"action-test-4\" ><attribute name=\"readonly\" for=\"archived\" expr=\"eval: true\"/></action-attrs>");
		metaAction4.setType("action-attrs");
		actions.save(metaAction4);

		MetaAction metaAction5 = new MetaAction();
		metaAction5.setName("action-test-5");
		metaAction5.setXml("<action-group name=\"action-test-5\" ><action name=\"action-test-1\"/><action name=\"action-test-2\"/></action-group>");
		metaAction5.setType("action-group");
		
		MetaAction metaAction6 = new MetaAction();
		metaAction6.setName("action-test-6");
		metaAction6.setXml("<action-group name=\"action-test-6\" ><action name=\"action-test-3\"/><action name=\"action-test-4\"/><action name=\"save\"/></action-group>");
		metaAction6.setType("action-group");
		
		MetaAction metaCondition = new MetaAction();
		metaCondition.setName("action-condition");
		metaCondition.setXml("<action-condition name=\"action-condition\" ><check field=\"archived\" if=\"archived\" error=\"This is an error !\"/></action-condition>");
		metaCondition.setType("action-condition");
		actions.save(metaCondition);
		
		Node start = new StartEvent();
		start.setName("Start");
		start.setType("startEvent");
		start.setStartTransitions(new ArrayList<Transition>());
		start.addEndTransition(startTransition);
		
		Node task1 = new NodeTask();
		task1.setName("Task 1");
		task1.setType("task");
		task1.setAction( metaAction5 );
		task1.addStartTransition(startTransition);
		task1.addEndTransition(intermediaryTransition);
		
		Node task2 = new NodeTask();
		task2.setName("Task 2");
		task2.setType("task");
		task2.setAction( metaAction6 );
		task2.addStartTransition(intermediaryTransition);
		task2.addEndTransition(endTransition);
		
		Node stop = new EndEvent();
		stop.setName("End");
		stop.setType("endEvent");
		stop.addStartTransition(endTransition);
		stop.setEndTransitions(new ArrayList<Transition>());
		
		startTransition.setStartNode(start);
		startTransition.setNextNode(task1);
		
		intermediaryTransition.setStartNode(task1);
		intermediaryTransition.setCondition(metaCondition);
		intermediaryTransition.setNextNode(task2);
		
		endTransition.setStartNode(task2);
		endTransition.setNextNode(stop);
		
		Workflow workflow = new Workflow();
		
		workflow.setName("Wkf");
		workflow.setMetaModel( MetaModelService.getMetaModel(Workflow.class) );
		workflow.setNode(start);
		workflow.setMaxNodeCounter(1);
		
		return workflow.save();
	}
}
