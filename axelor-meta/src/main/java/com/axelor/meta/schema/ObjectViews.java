/**
 * Copyright (c) 2012-2013 Axelor. All Rights Reserved.
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
 * Copyright (c) 2012-2013 Axelor. All Rights Reserved.
 */
package com.axelor.meta.schema;

import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import com.axelor.meta.schema.actions.Action;
import com.axelor.meta.schema.actions.ActionAttrs;
import com.axelor.meta.schema.actions.ActionCondition;
import com.axelor.meta.schema.actions.ActionExport;
import com.axelor.meta.schema.actions.ActionGroup;
import com.axelor.meta.schema.actions.ActionImport;
import com.axelor.meta.schema.actions.ActionMethod;
import com.axelor.meta.schema.actions.ActionRecord;
import com.axelor.meta.schema.actions.ActionValidate;
import com.axelor.meta.schema.actions.ActionView;
import com.axelor.meta.schema.actions.ActionWS;
import com.axelor.meta.schema.actions.ActionWorkflow;
import com.axelor.meta.schema.views.AbstractView;
import com.axelor.meta.schema.views.CalendarView;
import com.axelor.meta.schema.views.ChartView;
import com.axelor.meta.schema.views.FormView;
import com.axelor.meta.schema.views.GridView;
import com.axelor.meta.schema.views.MenuItem;
import com.axelor.meta.schema.views.Portal;
import com.axelor.meta.schema.views.Search;
import com.axelor.meta.schema.views.SearchFilters;
import com.axelor.meta.schema.views.Selection;
import com.axelor.meta.schema.views.TreeView;

@XmlType
@XmlRootElement(name = "object-views")
public class ObjectViews {

	public static final String NAMESPACE = "http://apps.axelor.com/xml/ns/object-views";

	public static final String VERSION = "0.9";

	@XmlElement(name = "menuitem", type = MenuItem.class)
	private List<MenuItem> menus;
	
	@XmlElement(name = "action-menu", type = MenuItem.class)
	private List<MenuItem> actionMenus;

	@XmlElement(name = "selection")
	private List<Selection> selections;

	@XmlElements({
		@XmlElement(name = "form", type = FormView.class),
		@XmlElement(name = "grid", type = GridView.class),
		@XmlElement(name = "tree", type = TreeView.class),
		@XmlElement(name = "chart", type = ChartView.class),
		@XmlElement(name = "portal", type = Portal.class),
		@XmlElement(name = "search", type = Search.class),
		@XmlElement(name = "calendar", type = CalendarView.class),
		@XmlElement(name = "search-filters", type = SearchFilters.class),
	})
	private List<AbstractView> views;
	
	@XmlElements({
		@XmlElement(name = "action-validate", type=ActionValidate.class),
		@XmlElement(name = "action-condition", type=ActionCondition.class),
		@XmlElement(name = "action-record", type=ActionRecord.class),
		@XmlElement(name = "action-method", type=ActionMethod.class),
		@XmlElement(name = "action-attrs", type=ActionAttrs.class),
		@XmlElement(name = "action-view", type=ActionView.class),
		@XmlElement(name = "action-ws", type=ActionWS.class),
		@XmlElement(name = "action-import", type=ActionImport.class),
		@XmlElement(name = "action-export", type=ActionExport.class),
		@XmlElement(name = "action-group", type=ActionGroup.class),
		@XmlElement(name = "action-workflow", type=ActionWorkflow.class)
	})
	private List<Action> actions;
	
	public List<MenuItem> getMenus() {
		return menus;
	}
	
	public void setMenus(List<MenuItem> menus) {
		this.menus = menus;
	}
	
	public List<MenuItem> getActionMenus() {
		return actionMenus;
	}
	
	public void setActionMenus(List<MenuItem> actionMenus) {
		this.actionMenus = actionMenus;
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
