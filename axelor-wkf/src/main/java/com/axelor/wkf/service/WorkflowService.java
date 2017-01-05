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
package com.axelor.wkf.service;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.axelor.auth.AuthUtils;
import com.axelor.auth.db.User;
import com.axelor.meta.ActionHandler;
import com.axelor.wkf.IWorkflow;
import com.axelor.wkf.db.Instance;
import com.axelor.wkf.db.Node;
import com.axelor.wkf.db.Transition;
import com.axelor.wkf.db.Workflow;
import com.axelor.wkf.db.repo.InstanceRepository;
import com.axelor.wkf.db.repo.NodeRepository;
import com.axelor.wkf.db.repo.WorkflowRepository;
import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.google.inject.persist.Transactional;

public class WorkflowService implements IWorkflow {

	protected Logger log = LoggerFactory.getLogger( getClass() );

	private ActionHandler actionHandler;
	private User user;

	protected int maxNodeCounter;
	private Map<Object, Object> context;

	protected Instance instance;

	private WorkflowRepository workflows;
	private InstanceRepository instances;
	private NodeRepository nodes;

	@Inject
	public WorkflowService (WorkflowRepository workflows, InstanceRepository instances, NodeRepository nodes) {
		this.workflows = workflows;
		this.instances = instances;
		this.nodes = nodes;
		this.context = Maps.newHashMap();
		this.user = AuthUtils.getUser();
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

		Instance instance = instances.all()
				.filter("self.workflow.metaModel.fullName = ?1 AND self.metaModelId = ?2", klass, id)
				.fetchOne();

		if (instance != null){ return instance; }
		else {
			Workflow workflow = getWorkflow(klass);
			return workflow != null ? this.createInstance(workflow, id) : null;
		}

	}

	@Override
	public Workflow getWorkflow( String klass ){
		final List<Workflow> all = ( List<Workflow> ) workflows.all()
				.filter("self.metaModel.fullName = ?1 AND self.active = true", klass).order("self.sequence")
				.fetch();

		for (Workflow workflow : all){
			if (workflows.isRunnable(workflow, actionHandler) ) {
				return workflow;
			}
		}
		return null;
	}

// RUN WKF

	@Override
	public Map<Object, Object> run( String klass, ActionHandler handler ){

		init( klass, handler ).playNodes( instance.getNodes() );
		persist();

		log.debug( "Final context ::: {}", context );
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
	protected void playNodes(Set<Node> nodes){
		log.debug("Play nodes" );
		for (Node node : nodes){
			log.debug("Play node ::: {}", node.getName() );
			this.nodes.execute(node, instance, user, actionHandler, context);
			log.debug( "Context ::: {}", context );
		}
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

		return instances.save(instance);
	}

	@Transactional
	protected Instance persist(){
		return instances.save(instance);
	}

}
