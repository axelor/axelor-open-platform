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
import javax.persistence.Cacheable;
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
import javax.validation.constraints.NotNull;

import org.hibernate.annotations.Index;
import org.hibernate.annotations.Type;

import com.axelor.auth.db.AuditableModel;
import com.axelor.db.JPA;
import com.axelor.db.Model;
import com.axelor.db.Query;
import com.axelor.meta.ActionHandler;
import com.axelor.meta.db.MetaAction;
import com.axelor.meta.db.MetaModel;
import com.google.common.base.Objects;
import com.google.common.base.Objects.ToStringHelper;

@Entity
@Cacheable
@Table(name = "WORKFLOW_WORKFLOW")
public class Workflow extends AuditableModel {
	
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long id;

	@NotNull
	@Index(name = "WORKFLOW_WORKFLOW_NAME_IDX")
	private String name;

	@NotNull
	@Index(name = "WORKFLOW_WORKFLOW_META_MODEL_IDX")
	@ManyToOne(fetch = FetchType.LAZY, cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	private MetaModel metaModel;

	@ManyToOne(fetch = FetchType.LAZY, cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	private Node node;

	private Integer maxNodeCounter = 1;

	@ManyToOne(fetch = FetchType.LAZY, cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	private MetaAction condition;

	private Integer sequence = 0;

	@Lob
	@Basic(fetch = FetchType.LAZY)
	@Type(type = "org.hibernate.type.TextType")
	private String description;

	private Boolean active = Boolean.TRUE;

	@Lob
	@Basic(fetch = FetchType.LAZY)
	@Type(type = "org.hibernate.type.TextType")
	private String bpmn;

	@Lob
	@Basic(fetch = FetchType.LAZY)
	@Type(type = "org.hibernate.type.TextType")
	private String xmlData;

	private String ref;

	public Workflow() {
	}

	public Workflow(String name) {
		this.name = name;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public MetaModel getMetaModel() {
		return metaModel;
	}

	public void setMetaModel(MetaModel metaModel) {
		this.metaModel = metaModel;
	}

	public Node getNode() {
		return node;
	}

	public void setNode(Node node) {
		this.node = node;
	}

	public Integer getMaxNodeCounter() {
		if (maxNodeCounter == null) return 0;
        return maxNodeCounter;
	}

	public void setMaxNodeCounter(Integer maxNodeCounter) {
		this.maxNodeCounter = maxNodeCounter;
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

	public Boolean getActive() {
		if (active == null) return Boolean.FALSE;
        return active;
	}

	public void setActive(Boolean active) {
		this.active = active;
	}

	public String getBpmn() {
		return bpmn;
	}

	public void setBpmn(String bpmn) {
		this.bpmn = bpmn;
	}

	public String getXmlData() {
		return xmlData;
	}

	public void setXmlData(String xmlData) {
		this.xmlData = xmlData;
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
		if (!(obj instanceof Workflow)) return false;

		Workflow other = (Workflow) obj;
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
		tsh.add("maxNodeCounter", this.getMaxNodeCounter());
		tsh.add("sequence", this.getSequence());
		tsh.add("active", this.getActive());
		tsh.add("ref", this.getRef());

		return tsh.omitNullValues().toString();
	}

	public static Workflow findByName(String name) {
		return Workflow.all()
				.filter("self.name = :name")
				.bind("name", name)
				.fetchOne();
	}

	/**
	 * Make the entity managed and persistent.
	 *
	 * @see EntityManager#persist(Object)
	 */
	public Workflow persist() {
		return JPA.persist(this);
	}

	/**
	 * Merge the state of the entity into the current persistence context.
	 *
	 * @see EntityManager#merge(Object)
	 */
	public Workflow merge() {
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
	public Workflow save() {
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
	 * Find a <code>Workflow</code> by <code>id</code>.
	 *
	 */
	public static Workflow find(Long id) {
		return JPA.find(Workflow.class, id);
	}

	/**
	 * Return a {@link Query} instance for <code>Workflow</code> to filter
	 * on all the records.
	 *
	 */
	public static Query<? extends Workflow> all() {
		return JPA.all(Workflow.class);
	}

	/**
	 * A shortcut method to <code>Workflow.all().filter(...)</code>
	 *
	 */
	public static Query<? extends Workflow> filter(String filter, Object... params) {
		return JPA.all(Workflow.class).filter(filter, params);
	}

	@SuppressWarnings("rawtypes")
	public boolean isRunnable(ActionHandler actionHandler){

		if ( this.condition == null ) { return true; }

		actionHandler.getRequest().setAction( this.condition.getName() );
		for ( Object data : (List) actionHandler.execute().getData()) {

			if ( ((Map) data).containsKey("errors") && ((Map) data).get("errors") != null && !( (Map) ((Map) data).get("errors") ).isEmpty() ) { return false; }

		}

		return true;

	}
}
