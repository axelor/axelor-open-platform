package com.axelor.wkf.db;

import javax.persistence.Entity;

import com.axelor.db.JPA;
import com.axelor.db.Query;
import com.axelor.db.Widget;
import com.google.common.base.Objects;
import com.google.common.base.Objects.ToStringHelper;

@Entity
public class Gateway extends Node {

	@Widget(selection = "node.logic.operator.selection")
	private String operator;

	public String getOperator() {
		return operator;
	}

	public void setOperator(String logicOperator) {
		this.operator = logicOperator;
	}
	
	@Override
	public String toString() {
		ToStringHelper tsh = Objects.toStringHelper(this);

		tsh.add("id", this.getId());
		tsh.add("name", this.getName());
		tsh.add("type", this.getType());
		tsh.add("operator", this.getOperator());
		tsh.add("ref", this.getRef());

		return tsh.omitNullValues().toString();
	}
	
	/**
	 * Find a <code>Gateway</code> by <code>id</code>.
	 *
	 */
	public static Gateway find(Long id) {
		return JPA.find(Gateway.class, id);
	}
	
	/**
	 * Return a {@link Query} instance for <code>Gateway</code> to filter
	 * on all the records.
	 *
	 */
	public static Query<Gateway> allGateway() {
		return JPA.all(Gateway.class);
	}
	
	/**
	 * A shortcut method to <code>Gateway.all().filter(...)</code>
	 *
	 */
	public static Query<Gateway> filterGateway(String filter, Object... params) {
		return JPA.all(Gateway.class).filter(filter, params);
	}
	
}
