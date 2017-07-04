/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2017 Axelor (<http://axelor.com>).
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
package com.axelor.meta.schema;

import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import com.axelor.common.VersionUtils;
import com.axelor.meta.schema.actions.Action;
import com.axelor.meta.schema.actions.ActionAttrs;
import com.axelor.meta.schema.actions.ActionCondition;
import com.axelor.meta.schema.actions.ActionExport;
import com.axelor.meta.schema.actions.ActionGroup;
import com.axelor.meta.schema.actions.ActionImport;
import com.axelor.meta.schema.actions.ActionMethod;
import com.axelor.meta.schema.actions.ActionRecord;
import com.axelor.meta.schema.actions.ActionReport;
import com.axelor.meta.schema.actions.ActionScript;
import com.axelor.meta.schema.actions.ActionValidate;
import com.axelor.meta.schema.actions.ActionView;
import com.axelor.meta.schema.actions.ActionWS;
import com.axelor.meta.schema.views.AbstractView;
import com.axelor.meta.schema.views.CalendarView;
import com.axelor.meta.schema.views.CardsView;
import com.axelor.meta.schema.views.ChartView;
import com.axelor.meta.schema.views.CustomView;
import com.axelor.meta.schema.views.Dashboard;
import com.axelor.meta.schema.views.FormView;
import com.axelor.meta.schema.views.GanttView;
import com.axelor.meta.schema.views.GridView;
import com.axelor.meta.schema.views.KanbanView;
import com.axelor.meta.schema.views.MenuItem;
import com.axelor.meta.schema.views.Search;
import com.axelor.meta.schema.views.SearchFilters;
import com.axelor.meta.schema.views.Selection;
import com.axelor.meta.schema.views.TreeView;

@XmlType
@XmlRootElement(name = "object-views")
public class ObjectViews {

	public static final String NAMESPACE = "http://axelor.com/xml/ns/object-views";

	public static final String VERSION = VersionUtils.getVersion().feature;

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
		@XmlElement(name = "dashboard", type = Dashboard.class),
		@XmlElement(name = "search", type = Search.class),
		@XmlElement(name = "calendar", type = CalendarView.class),
		@XmlElement(name = "gantt", type = GanttView.class),
		@XmlElement(name = "cards", type = CardsView.class),
		@XmlElement(name = "kanban", type = KanbanView.class),
		@XmlElement(name = "custom", type = CustomView.class),
		@XmlElement(name = "search-filters", type = SearchFilters.class),
	})
	private List<AbstractView> views;
	
	@XmlElements({
		@XmlElement(name = "action-validate", type=ActionValidate.class),
		@XmlElement(name = "action-condition", type=ActionCondition.class),
		@XmlElement(name = "action-record", type=ActionRecord.class),
		@XmlElement(name = "action-method", type=ActionMethod.class),
		@XmlElement(name = "action-attrs", type=ActionAttrs.class),
		@XmlElement(name = "action-script", type=ActionScript.class),
		@XmlElement(name = "action-view", type=ActionView.class),
		@XmlElement(name = "action-ws", type=ActionWS.class),
		@XmlElement(name = "action-import", type=ActionImport.class),
		@XmlElement(name = "action-export", type=ActionExport.class),
		@XmlElement(name = "action-group", type=ActionGroup.class),
		@XmlElement(name = "action-report", type=ActionReport.class),
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
