package com.axelor.wkf.db.node.message;

import javax.persistence.Entity;

import com.axelor.db.JPA;
import com.axelor.db.Query;
import com.axelor.db.Widget;
import com.axelor.wkf.db.node.IntermediateMessageEvent;
import com.google.common.base.Objects;
import com.google.common.base.Objects.ToStringHelper;

@Entity
public class IntermediateSocialNetworkEvent extends IntermediateMessageEvent {

	@Widget(selection = "node.social.network.selection")
	private String socialNetwork;
	
	private Boolean privateMessage;
	
	public Boolean getPrivateMessage() {
		return privateMessage;
	}

	public void setPrivateMessage(Boolean privateMessage) {
		this.privateMessage = privateMessage;
	}
	
	@Override
	public String toString() {
		ToStringHelper tsh = Objects.toStringHelper(this);

		tsh.add("id", this.getId());
		tsh.add("name", this.getName());
		tsh.add("type", this.getType());
		tsh.add("ref", this.getRef());
		tsh.add("privateMessage", this.getPrivateMessage());

		return tsh.omitNullValues().toString();
	}
	
	/**
	 * Find a <code>IntermediateSocialNetworkEvent</code> by <code>id</code>.
	 *
	 */
	public static IntermediateSocialNetworkEvent find(Long id) {
		return JPA.find(IntermediateSocialNetworkEvent.class, id);
	}

	/**
	 * Return a {@link Query} instance for <code>IntermediateSocialNetworkEvent</code> to filter
	 * on all the records.
	 *
	 */
	public static Query<IntermediateSocialNetworkEvent> allIntermediateSocialNetworkEvent() {
		return JPA.all(IntermediateSocialNetworkEvent.class);
	}
	
	/**
	 * A shortcut method to <code>IntermediateSocialNetworkEvent.all().filter(...)</code>
	 *
	 */
	public static Query<IntermediateSocialNetworkEvent> filterIntermediateSocialNetworkEvent(String filter, Object... params) {
		return JPA.all(IntermediateSocialNetworkEvent.class).filter(filter, params);
	}
	
}
