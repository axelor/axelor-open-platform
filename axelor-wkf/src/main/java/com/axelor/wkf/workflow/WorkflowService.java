package com.axelor.wkf.workflow;

import java.util.ArrayList;
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
import com.axelor.meta.ActionHandler;
import com.axelor.wkf.IWorkflow;
import com.axelor.wkf.action.Action;
import com.axelor.wkf.db.Instance;
import com.axelor.wkf.db.InstanceCounter;
import com.axelor.wkf.db.InstanceHistory;
import com.axelor.wkf.db.Node;
import com.axelor.wkf.db.Transition;
import com.axelor.wkf.db.WaitingNode;
import com.axelor.wkf.db.Workflow;
import com.google.common.base.Preconditions;
import com.google.inject.persist.Transactional;

public class WorkflowService implements IWorkflow {
	
	protected Logger log = LoggerFactory.getLogger(WorkflowService.class);
	
	private Action actionWorkflow;
	private ActionHandler actionHandler;
	
	protected int maxNodeCounter;
	protected Map<Node, Integer> nodeCounters;
	protected Set<Node> executedNodes;
	protected Map<Node, Set<Transition>> waitingNodes;
	
	protected Instance instance;
	protected DateTime dateTime;
	protected User user;
	
	@Inject
	public WorkflowService ( Action actionWorkflow ){
		
		this.nodeCounters = new HashMap<Node, Integer>();
		this.executedNodes = new HashSet<Node>();
		this.waitingNodes = new HashMap<Node, Set<Transition>>();

		this.dateTime = new DateTime();
		this.user = AuthUtils.getUser();
		
		this.actionWorkflow = actionWorkflow;
		
	}

// ACTION REQUEST	
	
	/**
	 * Init the service from one instance.
	 * 
	 * 
	 */
	protected WorkflowService init (String klass, ActionHandler actionHandler) {
		
		log.debug("INIT Wkf engine context ::: {}", actionHandler.getContext());
		log.debug("INIT Wkf engine class ::: {}", klass);

		Preconditions.checkNotNull(klass);
		this.instance = getInstance(klass, (Long)actionHandler.getContext().get("id"));
		
		log.debug("INIT Wkf instance ::: {}", this.instance);
		
		Preconditions.checkNotNull(this.instance);
		this.actionHandler = actionHandler;
		this.maxNodeCounter = this.instance.getWorkflow().getMaxNodeCounter();
		loadWaitingNodes( instance );
		loadExecutedNodes( instance );
		
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
	@Override
	public Instance getInstance(String klass, long id){

		Instance instance = Instance.all().filter("self.workflow.metaModel.fullName = ?1 AND self.metaModelId = ?2", klass, id).fetchOne();
		
		if (instance != null){ return instance; }
		else {
			Workflow workflow = getWorkflow(klass);
			return workflow != null ? this.createInstance(workflow, id) : null;
		}
		
	}

	@Override
	public Workflow getWorkflow(String klass){
		
		List<Workflow> workflows = Workflow.all()
				.filter("self.metaModel.fullName = ?1 AND self.active = true", klass)
				.order("self.sequence")
				.fetch();
		
		for (Workflow workflow : workflows){ 
			if ( workflow.getCondition() != null ){
				actionWorkflow.execute(workflow.getCondition(), actionHandler);
				if ( !actionWorkflow.isInError() ){
					return workflow;
				}
			}
			else {
				return workflow;
			}
		}
		
		return null;
		
	}

// RUN WKF

	@Override
	public Map<Object, Object> run( String klass, ActionHandler handler ){
		
		updateInstance( init( klass, handler ).playNodes( instance.getNodes() ) );
		return actionWorkflow.getData();
		
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
	protected Set<Node> playNodes(Set<Node> nodes){
		
		Set<Node> lastNodes = new HashSet<Node>();

		log.debug("Play nodes ::: {}", nodes );
		
		for (Node node : nodes){
			lastNodes.addAll( playNode( node ) );
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
	protected Set<Node> playNode(Node node){

		log.debug("Play node ::: {}", node.getName());
		
		testMaxPassedNode(node);
		
		Set<Node> nodes = new HashSet<Node>();
		
		if ( !node.getEndTransitions().isEmpty() ){
			
			if ( isPlayable(node) ){

				if ( node.getAction() != null ){ actionWorkflow.execute( node.getAction(), actionHandler ); }
				nodes.addAll( playTransitions( node.getEndTransitions() ) );
				counterAdd( instance, node );
				addExecutedNodes( node );
			}
			
		}
		else {

			if ( node.getAction() != null ){ actionWorkflow.execute( node.getAction(), actionHandler ); }
			nodes.add( node );
			
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
	protected Set<Node> playTransitions(List<Transition> transitions){
		
		Set<Node> nodes = new HashSet<Node>();
		
		log.debug("Play transitions ::: {}", transitions);	
		
		for ( Transition transition : transitions ){

			if ( transition.getSignal() != null ) { nodes.addAll( playTransitionFromSignal(transition) ); }
			else if ( transition.getRole() != null ) { nodes.addAll( playTransitionFromRole(transition) ); }
			else { nodes.addAll( playTransition(transition) ); }
			
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
	protected Set<Node> playTransitionFromSignal(Transition transition) {
		
		Set<Node> nodes = new HashSet<Node>();

		log.debug( "Play transition for signal ::: {}", transition.getSignal() );
		
		if ( actionHandler.getContext().containsKey("_signal") &&  actionHandler.getContext().get("_signal").equals( transition.getSignal() ) ) {

			if ( transition.getRole() != null ) { nodes.addAll( playTransitionFromRole(transition) ); }
			else { nodes.addAll( playTransition(transition) ); }
			
		}
		else { nodes.add( transition.getStartNode() ); }

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
	protected Set<Node> playTransitionFromRole(Transition transition) {
		
		Set<Node> nodes = new HashSet<Node>();

		log.debug("Play transition for role ::: {}", transition.getRole().getName());
		
		if ( transition.getRole().getUsers().contains( user ) ) { nodes.addAll( playTransition(transition) ); }
		else { nodes.add( transition.getStartNode() ); }
		
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
	protected Set<Node> playTransition(Transition transition) {

		
		Set<Node> nodes = new HashSet<Node>();
		
		if ( transition.getCondition() != null ){

			log.debug("Play transition condition ::: {}", transition.getCondition().getName() );
			
			actionWorkflow.execute( transition.getCondition(), actionHandler );
			
			if ( !actionWorkflow.isInError( ) ){

				addHistory( instance, transition );
				addWaitingNodes( transition );
				nodes.addAll( playNode( transition.getNextNode() ) );
				
			}
			else {

				nodes.add( transition.getStartNode() );
				
			}
			
		}
		else {
			
			log.debug("Play transition without condition");

			addHistory( instance, transition );
			addWaitingNodes( transition );
			nodes.addAll( playNode( transition.getNextNode() ) );
			
		}
		
		log.debug("Nodes returned at the end ::: {}", nodes);
		
		return nodes;
		
	}
	
// RAISING EXCEPTION
	
	/**
	 * Throw error if the counter for this node is greater than max node counter.
	 */
	protected void testMaxPassedNode (Node node) {
		
		int counter = 1;
		
		if (nodeCounters.containsKey(node)){
			
			counter += nodeCounters.get(node);
			
		}

		nodeCounters.put(node, counter);

		log.debug( "compteur {} ::: max {}", counter, this.maxNodeCounter );
		
		if (counter > this.maxNodeCounter) {
			throw new Error( String.format( "We passed by the node %s %d time", node.getName(), counter ) );
		}
		
	}

// HELPER	
	
	/**
	 * Load waiting nodes from instance
	 * 
	 * @param instance
	 * @return
	 */
	protected void loadWaitingNodes(Instance instance){
		
		waitingNodes.clear();
			
		for (WaitingNode waitingNode : instance.getWaitingNodes()){
			
			waitingNodes.put( waitingNode.getNode(), waitingNode.getTransitions() );
			
		}
	}
	
	/**
	 * Add waitingNode for next node of this transition if start logic operator of next node is AND.
	 * 
	 * @param transition
	 * 		Target transition
	 */
	protected void addWaitingNodes(Transition transition){
		
		Node node = transition.getNextNode();
		
		if ( node.getLogicOperator() == null || node.getLogicOperator().trim().equals("")) { return ; }
		
		if (node.getLogicOperator().equals( AND )){
			
			if ( !waitingNodes.containsKey(node) ){ waitingNodes.put( node, new HashSet<Transition>() ); }

			waitingNodes.get( node ).add( transition );
			
		}
		
	}
	
	/**
	 * Add executedNodes if start logic operator of node is XOR.
	 * 
	 * @param transition
	 * 		Target transition
	 */
	protected void addExecutedNodes(Node node){

		if ( node.getLogicOperator() == null || node.getLogicOperator().trim().equals("")) { return ; }
		
		if (node.getLogicOperator().equals( XOR )){
			executedNodes.add( node );
		}
		
	}
	
	/**
	 * Load executed nodes from instance
	 * 
	 * @param instance
	 * @return
	 */
	protected void loadExecutedNodes(Instance instance){
		
		executedNodes.clear();
		executedNodes.addAll( instance.getExecutedNodes() );
		
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
	protected boolean isPlayable( Node node ){
		
		if ( node.getLogicOperator() == null || node.getLogicOperator().trim().equals("")) { return true; }
		
		if ( node.getLogicOperator().equals( XOR ) && executedNodes.contains( node ) ){
			return false;
		}
		else if ( node.getLogicOperator().equals( AND ) ){
			
			if ( waitingNodes.containsKey( node ) && !waitingNodes.get( node ).containsAll( node.getStartTransitions() ) ){
				
				return false;
				
			}
			else {
				
				waitingNodes.remove( node );
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
		
		instance.setWorkflow( wkf );
		instance.setMetaModelId( id );
		
		instance.setCreationDate( dateTime );
		instance.setCreationUser( user );
		
		instance.setNodes( new HashSet<Node>() );
		instance.setExecutedNodes( new HashSet<Node>() );
		instance.setWaitingNodes( new ArrayList<WaitingNode>() );

		instance.addNode( wkf.getNode() );
		
		return instance.save();
		
	}
	
	@Transactional
	protected Instance updateInstance(Set<Node> nodes){
		
		log.debug( "Final nodes ::: {}", nodes );
		
		instance.clearNodes();
		instance.getNodes().addAll( nodes );
		instance.getExecutedNodes().addAll( executedNodes );
		
		return instance.save();
	}
	
	/**
	 * Add a new history in Instance from a transition.
	 * 
	 * @param instance
	 * 		Target instance.
	 * @param transition
	 * 		Target transition.
	 */
	protected void addHistory(Instance instance, Transition transition){
		
		InstanceHistory history = new InstanceHistory();
		
		history.setCreationDate( dateTime );
		history.setCreationUser( user );
		history.setTransition( transition );		
		instance.addHistory( history );
		
	}
	
	/**
	 * Increment counter of one node for one instance.
	 * 
	 * @param instance
	 * 		Target instance.
	 * @param node
	 * 		Target node.
	 */
	protected void counterAdd(Instance instance, Node node){
		
		InstanceCounter counter = InstanceCounter.all().filter("self.instance = ?1 AND self.node = ?2", instance, node).fetchOne();
		
		if (counter != null){
			
			counter.setCounter( counter.getCounter() + 1 );
			
		}
		else {
			
			counter = new InstanceCounter();

			counter.setNode( node );
			counter.setCounter( 1 );
			instance.addCounter( counter );
			
		}
		
	}
	
}
