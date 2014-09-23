/**
 * Axelor Business Solutions
 *
 * Copyright (C) 2012-2014 Axelor (<http://axelor.com>).
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
package com.axelor.wkf.db;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.Cacheable;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import com.axelor.auth.db.AuditableModel;
import com.axelor.db.JPA;
import com.axelor.db.Model;
import com.axelor.db.Query;
import com.axelor.db.annotations.Widget;
import com.google.common.base.Objects;
import com.google.common.base.Objects.ToStringHelper;

@Entity
@Cacheable
@Table(name = "WORKFLOW_INSTANCE", uniqueConstraints = { @UniqueConstraint(name = "UNIQUE_WORKFLOW_INSTANCE_WORKFLOW_META_MODEL_ID", columnNames = { "workflow", "metaModelId" }) })
public class Instance extends AuditableModel {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long id;

	@Widget(title = /*$$(*/"Workflow"/*)*/)
	@ManyToOne(fetch = FetchType.LAZY, cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	private Workflow workflow;

	@Widget(title = /*$$(*/"Model Id"/*)*/)
	private Long metaModelId = 0L;

	@Widget(title = /*$$(*/"Nodes"/*)*/)
	@ManyToMany(fetch = FetchType.LAZY, cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	private Set<Node> nodes;

	@Widget(title = /*$$(*/"Executed transitions"/*)*/)
	@ManyToMany(fetch = FetchType.LAZY, cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	private Set<Transition> executedTransitions;

	@Widget(title = /*$$(*/"Histories"/*)*/)
	@OneToMany(fetch = FetchType.LAZY, mappedBy = "instance", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<InstanceHistory> histories;

	@Widget(title = /*$$(*/"Counters"/*)*/)
	@OneToMany(fetch = FetchType.LAZY, mappedBy = "instance", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<InstanceCounter> counters;

	public Instance() {
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Workflow getWorkflow() {
		return workflow;
	}

	public void setWorkflow(Workflow workflow) {
		this.workflow = workflow;
	}

	public Long getMetaModelId() {
		if (metaModelId == null) return 0L;
        return metaModelId;
	}

	public void setMetaModelId(Long metaModelId) {
		this.metaModelId = metaModelId;
	}

	public Set<Node> getNodes() {
		return nodes;
	}

	public void setNodes(Set<Node> nodes) {
		this.nodes = nodes;
	}

	/**
	 * Add the given {@link #Node} item to the {@code nodes}.
	 *
	 */
	public void addNode(Node item) {
		if (nodes == null) {
			nodes = new HashSet<Node>();
		}
		nodes.add(item);
	}

	/**
	 * Remove the given {@link #Node} item from the {@code nodes}.
	 *
	 */
	public void removeNode(Node item) {
		if (nodes == null) {
			return;
		}
		nodes.remove(item);
	}

	/**
	 * Clear the {@code nodes} collection.
	 *
	 * <p>
	 * It calls the {@code this.flush()} method to avoid unexpected errors
	 * if any of the item in the collection is changed.
	 * </p>
	 */
	public void clearNodes() {
		if (nodes != null) {
			nodes.clear();
		}
	}

	public Set<Transition> getExecutedTransitions() {
		return executedTransitions;
	}

	public void setExecutedTransitions(Set<Transition> executedTransitions) {
		this.executedTransitions = executedTransitions;
	}

	/**
	 * Add the given {@link #Transition} item to the {@code executedTransitions}.
	 *
	 */
	public void addExecutedTransition(Transition item) {
		if (executedTransitions == null) {
			executedTransitions = new HashSet<Transition>();
		}
		executedTransitions.add(item);
	}

	/**
	 * Remove the given {@link #Transition} item from the {@code executedTransitions}.
	 *
	 */
	public void removeExecutedTransition(Transition item) {
		if (executedTransitions == null) {
			return;
		}
		executedTransitions.remove(item);
	}

	/**
	 * Remove the given {@link #Transition} item from the {@code executedTransitions}.
	 *
	 */
	public void removeAllExecutedTransition(List<Transition> items) {
		if (executedTransitions == null) {
			return;
		}
		executedTransitions.removeAll(items);
	}

	/**
	 * Clear the {@code executedTransitions} collection.
	 *
	 * <p>
	 * It calls the {@code this.flush()} method to avoid unexpected errors
	 * if any of the item in the collection is changed.
	 * </p>
	 */
	public void clearExecutedTransitions() {
		if (executedTransitions != null) {
			executedTransitions.clear();
		}
	}

	public List<InstanceHistory> getHistories() {
		return histories;
	}

	public void setHistories(List<InstanceHistory> histories) {
		this.histories = histories;
	}

	/**
	 * Add the given {@link #InstanceHistory} item to the {@code histories}.
	 *
	 * <p>
	 * It sets {@code item.instance = this} to ensure the proper relationship.
	 * </p>
	 */
	public void addHistory(InstanceHistory item) {
		if (histories == null) {
			histories = new ArrayList<InstanceHistory>();
		}
		histories.add(item);
		item.setInstance(this);
	}

	/**
	 * Remove the given {@link #InstanceHistory} item from the {@code histories}.
	 *
	 */
	public void removeHistory(InstanceHistory item) {
		if (histories == null) {
			return;
		}
		histories.remove(item);
	}

	/**
	 * Clear the {@code histories} collection.
	 *
	 * <p>
	 * It calls the {@code this.flush()} method to avoid unexpected errors
	 * if any of the item in the collection is changed.
	 * </p>
	 */
	public void clearHistories() {
		if (histories != null) {
			histories.clear();
		}
	}

	public List<InstanceCounter> getCounters() {
		return counters;
	}

	public void setCounters(List<InstanceCounter> counters) {
		this.counters = counters;
	}

	/**
	 * Add the given {@link #InstanceCounter} item to the {@code counters}.
	 *
	 * <p>
	 * It sets {@code item.instance = this} to ensure the proper relationship.
	 * </p>
	 */
	public void addCounter(InstanceCounter item) {
		if (counters == null) {
			counters = new ArrayList<InstanceCounter>();
		}
		counters.add(item);
		item.setInstance(this);
	}

	/**
	 * Remove the given {@link #InstanceCounter} item from the {@code counters}.
	 *
	 */
	public void removeCounter(InstanceCounter item) {
		if (counters == null) {
			return;
		}
		counters.remove(item);
	}

	/**
	 * Clear the {@code counters} collection.
	 *
	 * <p>
	 * It calls the {@code this.flush()} method to avoid unexpected errors
	 * if any of the item in the collection is changed.
	 * </p>
	 */
	public void clearCounters() {
		if (counters != null) {
			counters.clear();
		}
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null) return false;
		if (this == obj) return true;
		if (!(obj instanceof Instance)) return false;

		Instance other = (Instance) obj;
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
		tsh.add("metaModelId", this.getMetaModelId());

		return tsh.omitNullValues().toString();
	}

	/**
	 * Make the entity managed and persistent.
	 *
	 * @see EntityManager#persist(Object)
	 */
	public Instance persist() {
		return JPA.persist(this);
	}

	/**
	 * Merge the state of the entity into the current persistence context.
	 *
	 * @see EntityManager#merge(Object)
	 */
	public Instance merge() {
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
	public Instance save() {
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
	 * Find a <code>Instance</code> by <code>id</code>.
	 *
	 */
	public static Instance find(Long id) {
		return JPA.find(Instance.class, id);
	}

	/**
	 * Return a {@link Query} instance for <code>Instance</code> to filter
	 * on all the records.
	 *
	 */
	public static Query<? extends Instance> all() {
		return JPA.all(Instance.class);
	}

	/**
	 * A shortcut method to <code>Instance.all().filter(...)</code>
	 *
	 */
	public static Query<? extends Instance> filter(String filter, Object... params) {
		return JPA.all(Instance.class).filter(filter, params);
	}
}
