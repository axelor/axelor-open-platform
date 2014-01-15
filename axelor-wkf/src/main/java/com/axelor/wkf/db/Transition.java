/**
 * Copyright (c) 2012-2014 Axelor. All Rights Reserved.
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
 * Copyright (c) 2012-2014 Axelor. All Rights Reserved.
 */
package com.axelor.wkf.db;

import java.util.List;
import java.util.Map;

import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.validation.constraints.NotNull;

import org.hibernate.annotations.Index;
import org.hibernate.annotations.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.axelor.auth.AuthUtils;
import com.axelor.auth.db.AuditableModel;
import com.axelor.auth.db.Role;
import com.axelor.auth.db.User;
import com.axelor.db.JPA;
import com.axelor.db.Model;
import com.axelor.db.Query;
import com.axelor.meta.ActionHandler;
import com.axelor.meta.db.MetaAction;
import com.google.common.base.Objects;
import com.google.common.base.Objects.ToStringHelper;

@Entity
@Table(name = "WORKFLOW_TRANSITION")
public class Transition extends AuditableModel {
	
	@Transient
	protected Logger logger = LoggerFactory.getLogger( getClass() );

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	private Node startNode;

	@NotNull
	@ManyToOne(fetch = FetchType.LAZY, cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	private Node nextNode;

	@Index(name = "WORKFLOW_TRANSITION_NAME_IDX")
	private String name;

	@ManyToOne(fetch = FetchType.LAZY, cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	private MetaAction condition;

	private Integer sequence = 0;

	@Lob
	@Basic(fetch = FetchType.LAZY)
	@Type(type = "org.hibernate.type.TextType")
	private String description;

	private String signal;

	@ManyToOne(fetch = FetchType.LAZY, cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	private Role role;

	private String ref;

	public Transition() {
	}
	
	public Transition(String name) {
		this.name = name;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Node getStartNode() {
		return startNode;
	}

	public void setStartNode(Node startNode) {
		this.startNode = startNode;
	}

	public Node getNextNode() {
		return nextNode;
	}

	public void setNextNode(Node nextNode) {
		this.nextNode = nextNode;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public MetaAction getCondition() {
		return condition;
	}

	public void setCondition(MetaAction condition) {
		this.condition = condition;
	}

	public Integer getSequence() {
		if (sequence == null) return 0;
        return sequence;
	}

	public void setSequence(Integer sequence) {
		this.sequence = sequence;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getSignal() {
		return signal;
	}

	public void setSignal(String signal) {
		this.signal = signal;
	}

	public Role getRole() {
		return role;
	}

	public void setRole(Role role) {
		this.role = role;
	}

	public String getRef() {
		return ref;
	}

	public void setRef(String ref) {
		this.ref = ref;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj == null) return false;
		if (this == obj) return true;
		if (!(obj instanceof Transition)) return false;
		
		Transition other = (Transition) obj;
		if (this.getId() != null && other.getId() != null) {
			return Objects.equal(this.getId(), other.getId());
		}
		
		return false;
	}
	
	@Override
	public int hashCode() {
		return super.hashCode();
	}
	
	@Override
	public String toString() {
		ToStringHelper tsh = Objects.toStringHelper(this);

		tsh.add("id", this.getId());
		tsh.add("name", this.getName());
		tsh.add("sequence", this.getSequence());
		tsh.add("signal", this.getSignal());
		tsh.add("ref", this.getRef());

		return tsh.omitNullValues().toString();
	}
	
	public static Transition findByName(String name) {
		return Transition.all()
				.filter("self.name = :name")
				.bind("name", name)
				.fetchOne();
	}

	/**
	 * Make the entity managed and persistent.
	 * 
	 * @see EntityManager#persist(Object)
	 */
	public Transition persist() {
		return JPA.persist(this);
	}

	/**
	 * Merge the state of the entity into the current persistence context.
	 * 
	 * @see EntityManager#merge(Object)
	 */
	public Transition merge() {
		return JPA.merge(this);
	}

	/**
	 * Save the state of the entity.<br>
	 * <br>
	 * It uses either {@link #persist()} or {@link #merge()} and calls
	 * {@link #flush()} to synchronize values with database.
	 * 
	 * @see #persist(Model)
	 * @see #merge(Model)
	 * 
	 */
	public Transition save() {
		return JPA.save(this);
	}
	
	/**
	 * Remove the entity instance.
	 * 
	 * @see EntityManager#remove(Object)
	 */
	public void remove() {
		JPA.remove(this);
	}
	
	/**
	 * Refresh the state of the instance from the database, overwriting changes
	 * made to the entity, if any.
	 * 
	 * @see EntityManager#refresh(Object)
	 */
	public void refresh() {
		JPA.refresh(this);
	}
	
	/**
	 * Synchronize the persistence context to the underlying database.
	 * 
	 * @see EntityManager#flush()
	 */
	public void flush() {
		JPA.flush();
	}
	
	/**
	 * Find a <code>Transition</code> by <code>id</code>.
	 *
	 */
	public static Transition find(Long id) {
		return JPA.find(Transition.class, id);
	}
	
	/**
	 * Return a {@link Query} instance for <code>Transition</code> to filter
	 * on all the records.
	 *
	 */
	public static Query<Transition> all() {
		return JPA.all(Transition.class);
	}
	
	/**
	 * A shortcut method to <code>Transition.all().filter(...)</code>
	 *
	 */
	public static Query<Transition> filter(String filter, Object... params) {
		return JPA.all(Transition.class).filter(filter, params);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public boolean execute( ActionHandler actionHandler, Map<Object, Object> context, User user ){ 

		logger.debug("Execute transition ::: {}", getName() );
		
		if ( signal != null && (!actionHandler.getContext().containsKey("_signal") || !actionHandler.getContext().get("_signal").equals( signal )) ) {
			logger.debug("Signal ::: {}", signal);
			return false;
		}
		
		if ( role != null ){

			if ( user == null ) { return false; }
			
			if ( !AuthUtils.hasRole(user, role.getName()) ) {
				logger.debug( "Role ::: {}", role.getName() );
				context.put("flash", JPA.translate("You have no sufficient rights."));
				return false;
			}
			
		}

		if ( condition != null ) {

			logger.debug( "Condition ::: {}", condition.getName() );
			actionHandler.getRequest().setAction( condition.getName() );
			for ( Object data : (List) actionHandler.execute().getData()) {
				if ( data instanceof Boolean ) { return (Boolean) data; }
				if ( data instanceof Map && ((Map) data).containsKey("errors") && ((Map) data).get("errors") != null && !( (Map) ((Map) data).get("errors") ).isEmpty() ) {

					logger.debug( "Context with Errors ::: {}", data );
					context.putAll( (Map) data );
					return false;
					
				}

			}
		}

		return true;
	}

}
