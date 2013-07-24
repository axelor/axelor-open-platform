package com.axelor.wkf.workflow;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.axelor.meta.ActionHandler;
import com.axelor.wkf.IWorkflow;
import com.axelor.wkf.db.Instance;
import com.axelor.wkf.db.InstanceCounter;
import com.axelor.wkf.db.InstanceHistory;
import com.axelor.wkf.db.Node;
import com.axelor.wkf.db.Transition;
import com.axelor.wkf.db.Workflow;
import com.axelor.wkf.db.node.ExclusiveGateway;
import com.axelor.wkf.db.node.Gateway;
import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.inject.persist.Transactional;

public class WorkflowService implements IWorkflow {

	protected Logger log = LoggerFactory.getLogger(WorkflowService.class);

	private ActionHandler actionHandler;

	protected int maxNodeCounter;
	private Map<Object, Object> context;

	protected Instance instance;

	@Inject
	public WorkflowService ( ){

		this.context = Maps.newHashMap();
	}

// ACTION REQUEST

	/**
	 * Init the service from one instance.
	 *
	 *
	 */
	protected WorkflowService init (String klass, ActionHandler actionHandler) {

		log.debug( "INIT Wkf engine context ::: {}", actionHandler.getContext() );
		log.debug( "INIT Wkf engine class ::: {}", klass );

		Preconditions.checkNotNull(klass);
		this.instance = getInstance(klass, (Long) actionHandler.getContext().get("id"));

		log.debug("INIT Wkf instance ::: {}", this.instance);

		Preconditions.checkNotNull(this.instance);
		this.actionHandler = actionHandler;
		this.maxNodeCounter = this.instance.getWorkflow().getMaxNodeCounter();

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
	public Workflow getWorkflow( String klass ){

		List<Workflow> workflows = Workflow.all()
				.filter("self.metaModel.fullName = ?1 AND self.active = true", klass)
				.order("self.sequence")
				.fetch();

		for (Workflow workflow : workflows){
			
			if ( workflow.isRunnable(actionHandler) ){ return workflow; }
			
		}

		return null;

	}

// RUN WKF

	@Override
	public Map<Object, Object> run( String klass, ActionHandler handler ){

		updateInstance( init( klass, handler ).playNodes( instance.getNodes() ) );
		return this.context;

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
			lastNodes.addAll( playTransitions( node.getEndTransitions() ) );
		}

		return lastNodes;

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

		for ( Transition transition : transitions ){

			log.debug("Play transition ::: {}", transition);

			if ( transition.execute( actionHandler ) ) {

				nodes.addAll( playTransition( transition ) );
				
				if ( transition.getNextNode() instanceof ExclusiveGateway ){
					break;
				}
				
			}

		}

		return nodes;
		
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
	protected Set<Node> playTransition( Transition transition ){

		addHistory( transition );
		Node node = transition.getNextNode();
		log.debug( "Play node ::: {}", node.getName() );
		
		testMaxPassedNode(node);
		
		if ( node.getType().equals("task") ) {
			updateContext( node.execute( actionHandler ) ); 
		}
		else if ( node.getType().equals("gateway") && ( (Gateway) node ).getOperator().equals("parallel")) {

			if ( (Boolean) node.execute( actionHandler, this.instance, transition ) ){ return playTransitions( node.getEndTransitions() ); }
			return Sets.newHashSet();
			
			
		}
		else if ( node.getType().equals("endEvent") ){
			return Sets.newHashSet(node); 
		}
		
		return playTransitions( node.getEndTransitions() );

	}

// RAISING EXCEPTION

	/**
	 * Throw error if the counter for this node is greater than max node counter.
	 */
	protected void testMaxPassedNode (Node node) {

		int counter = counterAdd(node);
		
		log.debug( "compteur {} ::: max {}", counter, maxNodeCounter );

		if ( counter > maxNodeCounter) {
			throw new Error( String.format( "We passed by the node %s %d time", node.getName(), counter ) );
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
	protected int counterAdd( Node node ){

		InstanceCounter counter = InstanceCounter.findByInstanceAndNode(instance, node);

		if (counter != null){

			counter.setCounter( counter.getCounter() + 1 );

		}
		else {

			counter = new InstanceCounter();

			counter.setNode( node );
			counter.setCounter( 1 );
			instance.addCounter( counter );

		}
		
		return counter.getCounter();

	}

// HELPER

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

		instance.setNodes( new HashSet<Node>() );
		instance.setExecutedTransitions( new HashSet<Transition>() );

		instance.addNode( wkf.getNode() );

		return instance.save();

	}

	@Transactional
	protected Instance updateInstance(Set<Node> nodes){

		log.debug( "Final nodes ::: {}", nodes );

		instance.getNodes().addAll( nodes );

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
	protected void addHistory(Transition transition){

		InstanceHistory history = new InstanceHistory();

		history.setTransition( transition );
		instance.addHistory( history );
		
		instance.getNodes().remove( transition.getStartNode() );

	}

	@SuppressWarnings("rawtypes")
	protected void updateContext( Object data ){
				
		for ( Object newContext : (List) data ) { updateContext( context, newContext ); }
		
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	protected void updateContext( Map<Object, Object> context, Object values ){
				
		Map data2 = (Map) values;
		
		for ( Object key : data2.keySet()) {
	
			if ( !context.containsKey(key) ) { context.put(key, data2.get(key)); }
			else {
				if ( context.get(key) instanceof Map ) { updateContext( (Map) context.get(key), data2.get(key) ); }
				else { context.put(key, data2.get(key)); }
			}
		}
		
		log.debug("Updated context : {}", context);
		
	}

}
