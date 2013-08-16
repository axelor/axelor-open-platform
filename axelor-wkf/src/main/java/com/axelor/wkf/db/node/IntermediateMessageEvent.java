package com.axelor.wkf.db.node;

import javax.persistence.Entity;

import com.axelor.db.JPA;
import com.axelor.db.Query;
import com.axelor.db.Widget;
import com.axelor.wkf.db.Node;
import com.google.common.base.Objects;
import com.google.common.base.Objects.ToStringHelper;

@Entity
public class IntermediateMessageEvent extends Node {

	@Widget(selection = "node.message.type.selection")
	private String messageType;
	
	private String account;
	
	private String template;
	
	private String templateModel;
	
	private Boolean persist;

	public String getMessageType() {
		return messageType;
	}

	public void setMessageType(String messageType) {
		this.messageType = messageType;
	}

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
		tsh.add("messageType", this.getMessageType());
		tsh.add("ref", this.getRef());

		return tsh.omitNullValues().toString();
	}
	
	/**
	 * Find a <code>IntermediateMessageEvent</code> by <code>id</code>.
	 *
	 */
	public static IntermediateMessageEvent find(Long id) {
		return JPA.find(IntermediateMessageEvent.class, id);
	}
	
	/**
	 * Return a {@link Query} instance for <code>IntermediateMessageEvent</code> to filter
	 * on all the records.
	 *
	 */
	public static Query<IntermediateMessageEvent> allIntermediateMessageEvent() {
		return JPA.all(IntermediateMessageEvent.class);
	}
	
	/**
	 * A shortcut method to <code>IntermediateMessageEvent.all().filter(...)</code>
	 *
	 */
	public static Query<IntermediateMessageEvent> filterIntermediateMessageEvent(String filter, Object... params) {
		return JPA.all(IntermediateMessageEvent.class).filter(filter, params);
	}
	
}
