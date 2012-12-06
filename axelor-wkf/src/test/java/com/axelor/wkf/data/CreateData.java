package com.axelor.wkf.data;

import java.util.ArrayList;

import com.axelor.meta.service.MetaModelService;
import com.axelor.wkf.db.Node;
import com.axelor.wkf.db.Transition;
import com.axelor.wkf.db.Workflow;

public class CreateData {

	public static Workflow createWorkflow() {
		
		Transition transition = new Transition();
		transition.setName("Transition");
		transition.setSequence(0);
		
		Node node1 = new Node();
		node1.setName("Node 1");
		node1.setType("start");
		node1.setEndLogicOperator("or");
		node1.setEndTransitions(new ArrayList<Transition>());
		
		Node node2 = new Node();
		node2.setName("Node 2");
		node2.setType("stop");
		node2.setStartLogicOperator("and");
		node2.setEndLogicOperator("or");
		node2.setEndTransitions(new ArrayList<Transition>());
		
		transition.setStartNode(node1);
		transition.setNextNode(node2);
		node1.getEndTransitions().add(transition);
		
		Workflow workflow = new Workflow();
		
		workflow.setName("Wkf");
		workflow.setMetaModel(MetaModelService.getMetaModel(Workflow.class));
		workflow.setNode(node1);
		workflow.setMaxNodeCounter(1);
		
		node1.setWorkflow(workflow);
		node2.setWorkflow(workflow);
		
		return workflow.save();
	}
	

}
