/**
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2017 Axelor (<http://axelor.com>).
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
package com.axelor.wkf.db.repo;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.axelor.auth.db.User;
import com.axelor.db.JpaRepository;
import com.axelor.meta.ActionHandler;
import com.axelor.wkf.db.Instance;
import com.axelor.wkf.db.InstanceCounter;
import com.axelor.wkf.db.InstanceHistory;
import com.axelor.wkf.db.Node;
import com.axelor.wkf.db.Transition;

public class NodeRepository extends JpaRepository<Node> {

	protected Logger logger = LoggerFactory.getLogger(getClass());

	private TransitionRepository transitions;

	private InstanceCounterRepository counters;

	@Inject
	public NodeRepository(TransitionRepository transitions, InstanceCounterRepository counters) {
		super(Node.class);
		this.transitions = transitions;
		this.counters = counters;
	}

	public void execute(Node node, Instance instance, User user, ActionHandler handler, Map<Object, Object> context) {
		for (Transition transition : node.getEndTransitions()) {
			if (transitions.execute(transition, user, handler, context)) {
				execute(transition.getNextNode(), transition, instance, user, handler, context);
			}
		}
	}

	public void execute(Node node, Transition transition, Instance instance, User user, ActionHandler handler, Map<Object, Object> context) {

		logger.debug("Execute node ::: {}", node.getName());

		testMaxPassedNode(node, instance);
		historize(node, instance, transition);

		execute(node, instance, user, handler, context);
	}

	/**
	 * Add a new history in Instance from a transition.
	 *
	 * @param instance
	 * 		Target instance.
	 * @param transition
	 * 		Target transition.
	 */
	protected void historize(Node node, Instance instance, Transition transition ){

		InstanceHistory history = new InstanceHistory();

		history.setTransition(transition);
		instance.addHistory(history);
		instance.addNode(node);
		instance.removeNode(transition.getStartNode());

		logger.debug("Instance state ::: {}", instance.getNodes() );
	}

	@SuppressWarnings("unchecked")
	protected void updateContext(Map<String, Object> context, Object data){
		if (data instanceof List) {
			for (Object data2 : (List<?>) data) {
				updateContext(context, data2);
			}
		}
		if (data instanceof Map) {
			final Map<String, Object> data2 = (Map<String, Object>) data;
			for (String key : data2.keySet()) {
				if (context.get(key) instanceof Map) {
					updateContext((Map<String, Object>) context.get(key), data2.get(key));
				} else if (context.containsKey(key)) {
					context.put(key, data2.get(key));
				} else {
					context.put(key, data2.get(key));
				}
			}
		}
	}

	// RAISING EXCEPTION

	/**
	 * Throw error if the counter for this node is greater than max node counter.
	 */
	protected void testMaxPassedNode(Node node, Instance instance) {

		int max = instance.getWorkflow().getMaxNodeCounter();
		int counter = counterAdd(node, instance);

		logger.debug( "compteur {} ::: max {}", counter, max);

		if (counter > max) {
			throw new Error( String.format( "We passed by the node %s %d time", node.getName(), counter));
		}
	}

	/**
	 * Increment counter of one node for one instance.
	 *
	 * @param instance
	 * 		Target instance.
	 * @param node
	 * 		Target node.
	 */
	protected int counterAdd(Node node, Instance instance ){
		InstanceCounter counter = counters.findByInstanceAndNode(instance, node);
		if (counter != null){
			counter.setCounter( counter.getCounter() + 1 );
		} else {
			counter = new InstanceCounter();
			counter.setNode(node);
			counter.setCounter(1);
			instance.addCounter(counter);
		}
		return counter.getCounter();
	}
}
