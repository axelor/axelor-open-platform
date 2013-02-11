package com.axelor.meta.views;

import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlType
@XmlRootElement(name = "object-views")
public class ObjectViews {
	
	@XmlElements({
		@XmlElement(name = "menuitem", type = MenuItem.class),
		@XmlElement(name = "action-menu", type = ActionMenuItem.class)
	})
	private List<MenuItem> menuItems;

	@XmlElement(name = "selection")
	private List<Selection> selections;

	@XmlElements({
		@XmlElement(name = "form", type = FormView.class),
		@XmlElement(name = "grid", type = GridView.class),
		@XmlElement(name = "tree", type = TreeView.class),
		@XmlElement(name = "portal", type = Portal.class),
		@XmlElement(name = "search", type = Search.class)
	})
	private List<AbstractView> views;
	
	@XmlElements({
		@XmlElement(name = "action-validate", type=Action.ActionValidate.class),
		@XmlElement(name = "action-condition", type=ActionCondition.class),
		@XmlElement(name = "action-record", type=Action.ActionRecord.class),
		@XmlElement(name = "action-method", type=Action.ActionMethod.class),
		@XmlElement(name = "action-attrs", type=Action.ActionAttrs.class),
		@XmlElement(name = "action-view", type=Action.ActionView.class),
		@XmlElement(name = "action-ws", type=ActionWS.class),
		@XmlElement(name = "action-import", type=ActionImport.class),
		@XmlElement(name = "action-export", type=ActionExport.class)
	})
	private List<Action> actions;
	
	public List<MenuItem> getMenuItems() {
		return menuItems;
	}
	
	public void setMenuItems(List<MenuItem> menuItems) {
		this.menuItems = menuItems;
	}
	
	public List<Selection> getSelections() {
		return selections;
	}
	
	public void setSelections(List<Selection> selections) {
		this.selections = selections;
	}
	
	public List<AbstractView> getViews() {
		return views;
	}

	public void setViews(List<AbstractView> views) {
		this.views = views;
	}
	
	public List<Action> getActions() {
		return actions;
	}
	
	public void setActions(List<Action> actions) {
		this.actions = actions;
	}
}
