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
package com.axelor.wkf.db.node;

import javax.persistence.Entity;

import com.axelor.db.JPA;
import com.axelor.db.Query;
import com.axelor.wkf.db.Node;
import com.google.common.base.Objects;
import com.google.common.base.Objects.ToStringHelper;

@Entity
public class MessageEvent extends Node {
	
	private String account;
	
	private String template;
	
	private String templateModel;
	
	private Boolean persist;

	public String getAccount() {
		return account;
	}

	public void setAccount(String account) {
		this.account = account;
	}

	public String getTemplate() {
		return template;
	}

	public void setTemplate(String template) {
		this.template = template;
	}

	public String getTemplateModel() {
		return templateModel;
	}

	public void setTemplateModel(String templateModel) {
		this.templateModel = templateModel;
	}

	public Boolean getPersist() {
		return persist;
	}

	public void setPersist(Boolean persist) {
		this.persist = persist;
	}

	@Override
	public String toString() {
		ToStringHelper tsh = Objects.toStringHelper(this);

		tsh.add("id", this.getId());
		tsh.add("name", this.getName());
		tsh.add("type", this.getType());
		tsh.add("persist", this.getPersist());
		tsh.add("ref", this.getRef());

		return tsh.omitNullValues().toString();
	}

	/**
	 * Find a <code>IntermediateMessageEvent</code> by <code>id</code>.
	 *
	 */
	public static MessageEvent find(Long id) {
		return JPA.find(MessageEvent.class, id);
	}

	/**
	 * Return a {@link Query} instance for <code>IntermediateMessageEvent</code> to filter
	 * on all the records.
	 *
	 */
	public static Query<? extends MessageEvent> all() {
		return JPA.all(MessageEvent.class);
	}

	/**
	 * A shortcut method to <code>IntermediateMessageEvent.all().filter(...)</code>
	 *
	 */
	public static Query<? extends MessageEvent> filter(String filter, Object... params) {
		return all().filter(filter, params);
	}

}
