package com.axelor.wkf.workflow;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.axelor.auth.AuthUtils;
import com.axelor.auth.db.User;
import com.axelor.db.JPA;
import com.axelor.db.Model;
import com.axelor.meta.service.MetaModelService;
import com.axelor.wkf.db.Instance;
import com.axelor.wkf.db.InstanceCounter;
import com.axelor.wkf.db.InstanceHistory;
import com.axelor.wkf.db.Node;
import com.axelor.wkf.db.Transition;
import com.axelor.wkf.db.WaitingNode;
import com.axelor.wkf.db.Workflow;
import com.google.common.base.Preconditions;
import com.google.inject.persist.Transactional;

public class WorkFlowEngine {
	
	private static final Logger LOG = LoggerFactory.getLogger(WorkFlowEngine.class);
	
	private final String XOR = "xor";
	private final String AND = "and";
	
	private int maxNodeCounter;
	private Map<Node, Integer> nodeCounters;
	private Set<Node> executedNodes;
	private Map<Node, Set<Transition>> waitingNodes;
	
	private Instance instance;
	private DateTime dateTime;
	private User user;
	
	@Inject
	public WorkFlowEngine (){
		
		this.nodeCounters = new HashMap<Node, Integer>();
		this.executedNodes = new HashSet<Node>();
		this.waitingNodes = new HashMap<Node, Set<Transition>>();

		this.maxNodeCounter = 1;
		this.dateTime = new DateTime();
		this.user = AuthUtils.getUser();
		
	}
	
	/**
	 * Init the service from one instance.
	 * 
	 * 
	 */
	private <T extends Model> WorkFlowEngine init (Workflow wkf, T bean) {
		
		LOG.debug("INIT Wkf Service :::");
		
		this.maxNodeCounter = wkf.getMaxNodeCounter();
		this.instance = this.getInstance(wkf, bean.getId());
		this.loadWaitingNodes(this.instance);
		this.loadExecutedNodes(this.instance);
		
		return this;
		
	}

// SPECIFIC GETTER	
	
	/**
	 * Get instance from one workflow and one object id. If we have no instance with this params,
	 * one instance is created.
	 * Set current instance of this with the instance found or create and return new current instance of this.
	 * 
	 * @param wkf
	 * 		Target workflow.
	 * @param id
	 * 		Model id.
	 * 
	 * @return
	 * 		The instance founded.
	 */
	public Instance getInstance(Workflow wkf, Long id){

		Instance instance = Instance.all().filter("self.workflow = ?1 AND self.metaModelId = ?2", wkf, id).fetchOne();
		
		if (instance != null){
			return instance;
		}
		else {
			return this.createInstance(wkf, id);
		}
		
	}
	
	/**
	 * Get workflow from class.
	 * 
	 * @param klass
	 * 		Target Class. The class must be extend Model.
	 * 
	 * @return
	 * 		The workflow founded.	
	 * 
	 * @see Model
	 */
	public <T extends Model> Workflow getWorkflow(Class<T> klass){
		
		return Workflow.all().filter("self.metaModel = ?1", MetaModelService.getMetaModel(klass)).fetchOne();
		
	}

// RUN WKF
	
	/**
	 * Run workflow from specific class for all records from this class.
	 * 
	 * @param klass
	 * 		Target class.
	 */
	public <T extends Model> void run(Class<T> klass){
		
		Workflow wkf = this.getWorkflow(klass);
		
		if (wkf != null){
			
			LOG.debug("Run workflow {}", wkf.getName());
			
			for (T bean : JPA.all(klass).fetch())
				this.run(wkf, bean);
			
		}
		else {
			
			LOG.debug("No workflow for entity {}", klass);
			
		}
		
	}
	
	/**
	 * Run workflow from specific class for one specific record.
	 * 
	 * @param klass
	 * @param id
	 */
	public <T extends Model> void run(T bean){
		
		Workflow wkf = this.getWorkflow(bean.getClass());
		
		if (wkf != null){
			
			this.run(wkf, bean);
			
		}
		else {
			
			LOG.debug("No workflow for entity {}", bean.getClass());
			
		}
		
	}
	
	/**
	 * Run workflow from specific class for one specific wkf and one specific record.
	 * 
	 * @param klass
	 * @param id
	 */
	private <T extends Model> void run(Workflow wkf, T bean){
		
		this.updateInstance(this.init(wkf, bean).playNodes(this.instance.getNodes()));
		
	}

// PLAY WKF
	
	/**
	 * Play a set of nodes.
	 * 
	 * @param nodes
	 * 		A set of nodes.
	 * 
	 * @return
	 * 		Set of running nodes.
	 */
	private Set<Node> playNodes(Set<Node> nodes){
		
		Set<Node> lastNodes = new HashSet<Node>();
		
		for (Node node : nodes){
			lastNodes.addAll(this.playNode(node));
		}
		
		return lastNodes;
		
	}
	
	/**
	 * Play a node.
	 * 
	 * @param node
	 * 		One node.
	 * 
	 * @return
	 * 		Set of running nodes.
	 */
	private Set<Node> playNode(Node node){

		LOG.debug("Play node {}", node.getName());
		
		this.testMaxPassedNode(node);
		
		Set<Node> nodes = new HashSet<Node>();
		
		if (!node.getEndTransitions().isEmpty()){
			
			if (this.isPlayable(node)){
				
				nodes.addAll(this.playTransitions(node.getEndTransitions()));
				this.counterAdd(this.instance, node);
				this.addExecutedNodes(node);
			}
			
		}
		else {
			
			nodes.add(node);
			
		}
			
		return nodes;
	}
	
	/**
	 * Play transitions.
	 * 
	 * @param transitions
	 * 		List of transitions.
	 * @param endLogicOperator
	 * 		Operator logic (or, xor).
	 * 
	 * @return
	 * 		Set of running nodes.
	 */
	private Set<Node> playTransitions(List<Transition> transitions){
		
		Set<Node> nodes = new HashSet<Node>();
		
		for (Transition transition : this.sortTransitions(transitions)){
			
			nodes.addAll(this.playTransition(transition));
			
		}
		
		return nodes;
		
		
	}

	/**
	 * Play transition.
	 * 
	 * @param transition
	 * 		One transition.
	 * 
	 * @return
	 * 		Set of running nodes.
	 */
	@SuppressWarnings("unused")
	private Set<Node> playTransition(Transition transition) {

		Preconditions.checkArgument(transition.getStartNode().getWorkflow().getMetaModel().equals(transition.getNextNode().getWorkflow().getMetaModel()));

		LOG.debug("Play transition {}", transition.getName());
		
		Set<Node> nodes = new HashSet<Node>();
		
		this.addHistory(this.instance, transition);
		
		if (transition.getCondition() != null){
			
			//TODO: Run condition.
			if (true){
				
				//TODO: Run action of next node.
				this.addWaitingNodes(transition);
				nodes.addAll(this.playNode(transition.getNextNode()));
			}
			else {
				nodes.add(transition.getStartNode());
			}
			
		}
		else {
			
			//TODO: Run action of next node.
			this.addWaitingNodes(transition);
			nodes.addAll(this.playNode(transition.getNextNode()));
			
		}
		
		return nodes;
		
	}
	
// RAISING EXCEPTION
	
	/**
	 * Throw error if the counter for this node is greater than max node counter.
	 */
	private void testMaxPassedNode (Node node) {
		
		int counter = 1;
		
		if (nodeCounters.containsKey(node)){
			
			counter += nodeCounters.get(node);
			
		}

		nodeCounters.put(node, counter);

		LOG.debug("compteur {} ::: max {}", counter, this.maxNodeCounter);
		
		if (counter > this.maxNodeCounter)
			throw new Error(String.format("We passed by the node %s %d time", node.getName(), counter));
		
	}

// HELPER	
	
	/**
	 * Load waiting nodes from instance
	 * 
	 * @param instance
	 * @return
	 */
	private void loadWaitingNodes(Instance instance){
		
		this.waitingNodes.clear();
			
		for (WaitingNode waitingNode : this.instance.getWaitingNodes()){
			
			this.waitingNodes.put(waitingNode.getNode(), waitingNode.getTransitions());
			
		}
	}
	
	/**
	 * Add waitingNode for next node of this transition if start logic operator of next node is AND.
	 * 
	 * @param transition
	 * 		Target transition
	 */
	private void addWaitingNodes(Transition transition){
		
		Node node = transition.getNextNode();
		
		if (node.getLogicOperator().equals(AND)){
			
			if (!this.waitingNodes.containsKey(node)){
				
				this.waitingNodes.put(node, new HashSet<Transition>());
				
			}

			this.waitingNodes.get(node).add(transition);
			
		}
		
	}
	
	/**
	 * Add executedNodes if start logic operator of node is XOR.
	 * 
	 * @param transition
	 * 		Target transition
	 */
	private void addExecutedNodes(Node node){
		
		if (node.getLogicOperator().equals(XOR)){
			this.executedNodes.add(node);
		}
		
	}
	
	/**
	 * Load executed nodes from instance
	 * 
	 * @param instance
	 * @return
	 */
	private void loadExecutedNodes(Instance instance){
		
		this.executedNodes.clear();
		this.executedNodes.addAll(this.instance.getExecutedNodes());
		
	}
	
	/**
	 * The node is playable if his start logic operator is different of XOR or equal and not executed.
	 * 
	 * @param node
	 * 		Target node.
	 * 
	 * @return
	 * 		True if the node is playable else false.
	 */
	private boolean isPlayable(Node node){
		
		if (node.getLogicOperator().equals(XOR) && this.executedNodes.contains(node)){
			return false;
		}
		else if (node.getLogicOperator().equals(AND)){
			
			if (this.waitingNodes.containsKey(node) && !this.waitingNodes.get(node).containsAll(node.getStartTransitions())){
				
				return false;
				
			}
			else {
				
				this.waitingNodes.remove(node);
				return true;
				
			}
		}
		else {
			return true;
		}
	}
	
	/**
	 * Create new Instance for one wkf and one record.
	 * 
	 * @param wkf
	 * 		One workflow.
	 * @param id
	 * 		One id of one record.
	 * 
	 * @return
	 * 		New instance.
	 */
	@Transactional
	protected Instance createInstance(Workflow wkf, Long id){
		
		Instance instance = new Instance();
		
		instance.setWorkflow(wkf);
		instance.setMetaModelId(id);
		
		instance.setNodes(new HashSet<Node>());
		instance.getNodes().add(wkf.getNode());
		
		instance.setCreationDate(this.dateTime);
		instance.setCreationUser(this.user);
		
		instance.setExecutedNodes(new HashSet<Node>());
		instance.setWaitingNodes(new ArrayList<WaitingNode>());
		
		return instance.save();
		
	}
	
	@Transactional
	protected Instance updateInstance(Set<Node> nodes){
		
		this.instance.getNodes().clear();
		this.instance.getNodes().addAll(nodes);
		
		this.instance.getExecutedNodes().addAll(this.executedNodes);
		
		return this.instance.save();
	}
	
	/**
	 * Add a new history in Instance from a transition.
	 * 
	 * @param instance
	 * 		Target instance.
	 * @param transition
	 * 		Target transition.
	 */
	private void addHistory(Instance instance, Transition transition){
		
		InstanceHistory history = new InstanceHistory();
		
		history.setInstance(instance);
		history.setCreationDate(this.dateTime);
		history.setCreationUser(this.user);
		history.setTransition(transition);
		
		if (instance.getHistories() == null){
			instance.setHistories(new ArrayList<InstanceHistory>());
		}
		
		instance.getHistories().add(history);
		
	}
	
	/**
	 * Increment counter of one node for one instance.
	 * 
	 * @param instance
	 * 		Target instance.
	 * @param node
	 * 		Target node.
	 */
	private void counterAdd(Instance instance, Node node){
		
		InstanceCounter counter = InstanceCounter.all().filter("self.instance = ?1 AND self.node = ?2", instance, node).fetchOne();
		
		if (counter != null){
			counter.setCounter(counter.getCounter() + 1);
		}
		else {
			
			counter = new InstanceCounter();
			counter.setInstance(instance);
			counter.setNode(node);
			counter.setCounter(1);
			
			if (instance.getCounters() == null){
				instance.setCounters(new ArrayList<InstanceCounter>());
			}
			
			instance.getCounters().add(counter);
			
		}
		
	}
	   
	/**
	 * Sort transitions by sequence.
	 * 
	 * @param transitions.
	 * 
	 * @return
	 * 		The sorted out transitions.
	 */
	private List<Transition> sortTransitions(List<Transition> transitions){
		
		Comparator<Transition> comparator = new Comparator<Transition>() {
			
			@Override
			public int compare(Transition t1, Transition t2) {
				
				if (t1.getSequence() < t2.getSequence()) return -1;
				if (t1.getSequence() > t2.getSequence()) return 1;
				return 0;
			}
		};
		
		Collections.sort(transitions, comparator);
		
		return transitions;
	}
}
