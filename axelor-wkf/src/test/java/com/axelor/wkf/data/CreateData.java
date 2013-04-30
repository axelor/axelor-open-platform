package com.axelor.wkf.data;

import java.util.ArrayList;

import com.axelor.meta.db.MetaAction;
import com.axelor.meta.service.MetaModelService;
import com.axelor.wkf.db.Node;
import com.axelor.wkf.db.Transition;
import com.axelor.wkf.db.Workflow;

public class CreateData {

	public static Workflow createWorkflow() {
		
		Transition transition = new Transition();
		transition.setName("Transition");
		transition.setSequence(0);
		
		MetaAction metaAction = new MetaAction();
		metaAction.setName("action-alert-test");
		metaAction.setXml("<action-validate name=\"action-alert-test\"><error message=\"It's a test\" if=\"true\"/></action-validate>");
		metaAction.setType("action-validate");
		
		Node node1 = new Node();
		node1.setName("Node 1");
		node1.setType("start");
		node1.setAction( metaAction );
		node1.setEndTransitions(new ArrayList<Transition>());
		
		Node node2 = new Node();
		node2.setName("Node 2");
		node2.setType("stop");
		node2.setLogicOperator("and");
		node2.setEndTransitions(new ArrayList<Transition>());
		
		transition.setStartNode(node1);
		transition.setNextNode(node2);
		node1.getEndTransitions().add(transition);
		
		Workflow workflow = new Workflow();
		
		workflow.setName("Wkf");
		workflow.setMetaModel( MetaModelService.getMetaModel(Workflow.class) );
		workflow.setNode(node1);
		workflow.setMaxNodeCounter(1);
		
		node1.setWorkflow(workflow);
		node2.setWorkflow(workflow);
		
		return workflow.save();
	}
	

}
