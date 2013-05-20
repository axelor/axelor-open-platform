package com.axelor.wkf.data;

import java.util.ArrayList;

import com.axelor.meta.db.MetaAction;
import com.axelor.meta.service.MetaModelService;
import com.axelor.wkf.db.Node;
import com.axelor.wkf.db.Transition;
import com.axelor.wkf.db.Workflow;

public class CreateData {

	public static Workflow createWorkflow() {
		
		Transition startTransition = new Transition();
		startTransition.setName("startTransition");
		startTransition.setSequence(0);
		
		Transition endTransition = new Transition();
		endTransition.setName("endTransition");
		endTransition.setSequence(0);
		
		MetaAction metaAction = new MetaAction();
		metaAction.setName("action-alert-test");
		metaAction.setXml("<action-validate name=\"action-alert-test\"><error message=\"It's a test\" if=\"true\"/></action-validate>");
		metaAction.setType("action-validate");
		
		Node node1 = new Node();
		node1.setName("Node 1");
		node1.setType("start");
		node1.setStartTransitions(new ArrayList<Transition>());
		node1.addEndTransition(startTransition);
		
		Node node2 = new Node();
		node2.setName("Node 1");
		node2.setType("intermediary");
		node2.setAction( metaAction );
		node2.addStartTransition(startTransition);
		node2.addEndTransition(endTransition);
		
		Node node3 = new Node();
		node3.setName("Node 2");
		node3.setType("stop");
		node3.setLogicOperator("and");
		node3.addStartTransition(endTransition);
		node3.setEndTransitions(new ArrayList<Transition>());
		
		startTransition.setStartNode(node1);
		startTransition.setNextNode(node2);
		
		endTransition.setStartNode(node2);
		endTransition.setNextNode(node3);
		
		Workflow workflow = new Workflow();
		
		workflow.setName("Wkf");
		workflow.setMetaModel( MetaModelService.getMetaModel(Workflow.class) );
		workflow.setNode(node1);
		workflow.setMaxNodeCounter(1);
		
		return workflow.save();
	}
	

}
