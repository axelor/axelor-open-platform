/**
 * Copyright (c) 2012-2013 Axelor. All Rights Reserved.
 *
 * The contents of this file are subject to the Common Public
 * Attribution License Version 1.0 (the “License”); you may not use
 * this file except in compliance with the License. You may obtain a
 * copy of the License at:
 *
 * http://license.axelor.com/.
 *
 * The License is based on the Mozilla Public License Version 1.1 but
 * Sections 14 and 15 have been added to cover use of software over a
 * computer network and provide for limited attribution for the
 * Original Developer. In addition, Exhibit A has been modified to be
 * consistent with Exhibit B.
 *
 * Software distributed under the License is distributed on an “AS IS”
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 *
 * The Original Code is part of "Axelor Business Suite", developed by
 * Axelor exclusively.
 *
 * The Original Developer is the Initial Developer. The Initial Developer of
 * the Original Code is Axelor.
 *
 * All portions of the code written by Axelor are
 * Copyright (c) 2012-2013 Axelor. All Rights Reserved.
 */
package com.axelor.wkf.workflow;

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
import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.google.inject.persist.Transactional;

public class WorkflowService implements IWorkflow {

	protected Logger log = LoggerFactory.getLogger(WorkflowService.class);

	private ActionHandler actionHandler;
	private User user;

	protected int maxNodeCounter;
	private Map<Object, Object> context;

	protected Instance instance;

	@Inject
	public WorkflowService ( ){

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
			node.execute(actionHandler, user, instance, context);
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

		return instance.save();

	}
	
	@Transactional
	protected Instance persist(){

		return instance.save();
		
	}

}
