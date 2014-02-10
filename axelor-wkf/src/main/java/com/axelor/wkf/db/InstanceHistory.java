/**
 * Copyright (c) 2012-2014 Axelor. All Rights Reserved.
 *
 * The contents of this file are subject to the Common Public
 * Attribution License Version 1.0 (the "License"); you may not use
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
 * Software distributed under the License is distributed on an "AS IS"
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

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import com.axelor.auth.db.AuditableModel;
import com.axelor.db.JPA;
import com.axelor.db.Model;
import com.axelor.db.Query;
import com.google.common.base.Objects;
import com.google.common.base.Objects.ToStringHelper;

@Entity
@Table(name = "WORKFLOW_INSTANCE_HISTORY")
public class InstanceHistory extends AuditableModel {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	private Instance instance;

	@ManyToOne(fetch = FetchType.LAZY, cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	private Transition transition;


	public InstanceHistory() {
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Instance getInstance() {
		return instance;
	}

	public void setInstance(Instance instance) {
		this.instance = instance;
	}

	public Transition getTransition() {
		return transition;
	}

	public void setTransition(Transition transition) {
		this.transition = transition;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj == null) return false;
		if (this == obj) return true;
		if (!(obj instanceof InstanceHistory)) return false;
		
		InstanceHistory other = (InstanceHistory) obj;
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

		return tsh.omitNullValues().toString();
	}

	/**
	 * Make the entity managed and persistent.
	 * 
	 * @see EntityManager#persist(Object)
	 */
	public InstanceHistory persist() {
		return JPA.persist(this);
	}

	/**
	 * Merge the state of the entity into the current persistence context.
	 * 
	 * @see EntityManager#merge(Object)
	 */
	public InstanceHistory merge() {
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
	public InstanceHistory save() {
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
	 * Find a <code>InstanceHistory</code> by <code>id</code>.
	 *
	 */
	public static InstanceHistory find(Long id) {
		return JPA.find(InstanceHistory.class, id);
	}
	
	/**
	 * Return a {@link Query} instance for <code>InstanceHistory</code> to filter
	 * on all the records.
	 *
	 */
	public static Query<InstanceHistory> all() {
		return JPA.all(InstanceHistory.class);
	}
	
	/**
	 * A shortcut method to <code>InstanceHistory.all().filter(...)</code>
	 *
	 */
	public static Query<InstanceHistory> filter(String filter, Object... params) {
		return JPA.all(InstanceHistory.class).filter(filter, params);
	}
}
