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
import com.axelor.db.annotations.Widget;
import com.axelor.i18n.I18n;
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

	@Widget(title = /*$$(*/"Start node"/*)*/)
	@ManyToOne(fetch = FetchType.LAZY, cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	private Node startNode;

	@NotNull
	@Widget(title = /*$$(*/"Next node"/*)*/)
	@ManyToOne(fetch = FetchType.LAZY, cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	private Node nextNode;

	@Widget(title = /*$$(*/"Name"/*)*/)
	@Index(name = "WORKFLOW_TRANSITION_NAME_IDX")
	private String name;

	@Widget(title = /*$$(*/"Condition"/*)*/)
	@ManyToOne(fetch = FetchType.LAZY, cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	private MetaAction condition;

	@Widget(title = /*$$(*/"Sequence"/*)*/)
	private Integer sequence = 0;

	@Lob
	@Widget(title = /*$$(*/"Description"/*)*/)
	@Basic(fetch = FetchType.LAZY)
	@Type(type = "org.hibernate.type.TextType")
	private String description;

	@Widget(title = /*$$(*/"Signal"/*)*/)
	private String signal;

	@Widget(title = /*$$(*/"Role"/*)*/)
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
	public static Query<? extends Transition> all() {
		return JPA.all(Transition.class);
	}

	/**
	 * A shortcut method to <code>Transition.all().filter(...)</code>
	 *
	 */
	public static Query<? extends Transition> filter(String filter, Object... params) {
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
				context.put("flash", I18n.get("You have no sufficient rights."));
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
