/**
 * Copyright (c) 2012-2014 Axelor. All Rights Reserved.
 *
 * The contents of this file are subject to the Common Public
 * Attribution License Version 1.0 (the "License"); you may not use
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
 * Software distributed under the License is distributed on an "AS IS"
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
 * Copyright (c) 2012-2014 Axelor. All Rights Reserved.
 */
package com.axelor.meta.service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.Map;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.axelor.common.FileUtils;
import com.axelor.db.JPA;
import com.axelor.meta.MetaScanner;
import com.axelor.meta.db.MetaAction;
import com.axelor.meta.db.MetaActionMenu;
import com.axelor.meta.db.MetaFilter;
import com.axelor.meta.db.MetaMenu;
import com.axelor.meta.db.MetaModule;
import com.axelor.meta.db.MetaSelect;
import com.axelor.meta.db.MetaSelectItem;
import com.axelor.meta.db.MetaTranslation;
import com.axelor.meta.db.MetaView;
import com.axelor.meta.domains.DomainModels;
import com.axelor.meta.domains.Entity;
import com.axelor.meta.domains.Module;
import com.axelor.meta.domains.Property;
import com.axelor.meta.loader.XMLViews;
import com.axelor.meta.schema.ObjectViews;
import com.axelor.meta.schema.actions.Action;
import com.axelor.meta.schema.actions.ActionCondition;
import com.axelor.meta.schema.actions.ActionCondition.Check;
import com.axelor.meta.schema.actions.ActionValidate;
import com.axelor.meta.schema.actions.ActionValidate.Validator;
import com.axelor.meta.schema.actions.ActionView;
import com.axelor.meta.schema.actions.ActionView.View;
import com.axelor.meta.schema.views.AbstractContainer;
import com.axelor.meta.schema.views.AbstractView;
import com.axelor.meta.schema.views.AbstractWidget;
import com.axelor.meta.schema.views.Button;
import com.axelor.meta.schema.views.CalendarView;
import com.axelor.meta.schema.views.Field;
import com.axelor.meta.schema.views.FormView;
import com.axelor.meta.schema.views.GridView;
import com.axelor.meta.schema.views.Group;
import com.axelor.meta.schema.views.Label;
import com.axelor.meta.schema.views.Menu;
import com.axelor.meta.schema.views.Menu.Item;
import com.axelor.meta.schema.views.MenuItem;
import com.axelor.meta.schema.views.Notebook;
import com.axelor.meta.schema.views.Page;
import com.axelor.meta.schema.views.Portal;
import com.axelor.meta.schema.views.Portlet;
import com.axelor.meta.schema.views.Search;
import com.axelor.meta.schema.views.Search.SearchField;
import com.axelor.meta.schema.views.Search.SearchSelect;
import com.axelor.meta.schema.views.SearchFilters;
import com.axelor.meta.schema.views.SearchFilters.SearchFilter;
import com.axelor.meta.schema.views.Separator;
import com.axelor.meta.schema.views.SimpleContainer;
import com.axelor.meta.schema.views.SimpleWidget;
import com.axelor.meta.schema.views.TreeView;
import com.axelor.meta.schema.views.TreeView.TreeColumn;
import com.google.common.base.CaseFormat;
import com.google.common.base.Charsets;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.Files;
import com.google.common.io.Resources;

public class MetaExportTranslation {

	private static final String LOCAL_SCHEMA_DOMAIN = "domain-models.xsd";
	private static Logger log = LoggerFactory.getLogger(MetaExportTranslation.class);

	private final String fieldType = "field";
	private final String helpType = "help";
	private final String placeholderType = "placeholder";
	private final String docType = "documentation";
	private final String groupType = "group";
	private final String pageType = "page";
	private final String separatorType = "separator";
	private final String labelType = "label";
	private final String buttonType = "button";
	private final String menuType = "menu";
	private final String actionMenuType = "actionMenu";
	private final String viewType = "view";
	private final String treeType = "tree";
	private final String portletType = "portlet";
	private final String filterType = "filter";
	private final String searchType = "search";
	private final String actionType = "action";
	private final String selectType = "select";
	private final String otherType = "other";
	private final String viewFieldType = "viewField";

	private File exportFile;
	private String exportLanguage;
	private String currentModule;

	private List<String> internalFields = Lists.newArrayList("createdOn", "updatedOn", "createdBy", "updatedBy", "id", "version", "archived");
	Map<String, Map<String, String>> doc = Maps.newLinkedHashMap();

	public void exportTranslations(String exportPath, String exportLanguage) throws Exception {

		this.exportLanguage = exportLanguage;

		List<MetaModule> modules = MetaModule.all().filter("self.installed = true").fetch();
		List<String> viewList = Lists.newArrayList();
		List<String> menuList = Lists.newArrayList();
		this.loadViewFromEntry(viewList, menuList, "");

		for(MetaModule module : modules) {
			File file = FileUtils.getFile(exportPath, module.getName(), this.exportLanguage + ".csv");
			if(file.isFile() && file.exists()) {
				file.delete();
			}
			this.exportFile = file;
			this.currentModule = module.getName();

			log.info("Export {} module to {}.", module.getName(), file.getPath());

			this.exportMenus();
			this.exportMenuActions();
			this.exportObjects();
			this.exportSelects();
			this.exportViews();
			this.exportActions();
			this.exportOther();
			this.exportDoc(viewList, menuList);
		}
	}

	private void exportSearchFilters(SearchFilters searchFilters) {
		for (SearchFilter filter : searchFilters.getFilters()) {
			String translation = this.getTranslation(filter.getDefaultTitle(), "", searchFilters.getModel(), this.filterType);
			this.appendToFile(searchFilters.getModel(), "", this.filterType, filter.getDefaultTitle(), translation);
		}
		this.exportMetaFilter(searchFilters.getName(), searchFilters.getModel());
	}

	private void exportMetaFilter(String filterView, String model) {
		MetaFilter filter = MetaFilter.all().filter("self.filterView = ?1", filterView).fetchOne();
		if (filter == null) {
			return;
		}
		String translation = this.getTranslation(filter.getTitle(), "", model, this.filterType);
		this.appendToFile(model, "", this.filterType, filter.getTitle(), translation);
	}

	private void exportOther() {
		for (MetaTranslation translation : MetaTranslation.all().filter("self.type = ?1 AND self.language = ?2 AND self.module = ?3",  this.otherType, this.exportLanguage, this.currentModule).order("key").fetch()) {
			this.appendToFile("", "", this.otherType, translation.getKey(), translation.getTranslation());
		}
	}

	private void exportActions() {
		for (MetaAction metaAction : MetaAction.findByModule(this.currentModule).order("name").order("type").fetch()) {
			try {
				ObjectViews views = (ObjectViews) XMLViews.fromXML(metaAction.getXml());
				if(views.getActions() != null) {
					Action action = (Action) views.getActions().get(0);
					this.loadAction(action);
				}
			}
			catch(Exception ex) {
				log.error("Error while exporting action : {}", metaAction.getName());
				log.error("With following exception : {}", ex);
			}
		}
	}

	private void loadAction(Action action) {
		if(action instanceof ActionView) {
			ActionView actionView = (ActionView) action;
			String translation = this.getTranslation(actionView.getDefaultTitle(), "", null, this.actionType);
			this.appendToFile("", "", this.actionType, actionView.getDefaultTitle(), translation);
			this.exportMetaFilter("act:" + action.getName(), actionView.getModel());
		}
		else if(action instanceof ActionValidate) {
			ActionValidate actionValidate = (ActionValidate) action;
			for(Validator validator : actionValidate.getValidators()) {
				String translation = this.getTranslation(validator.getMessage(), "", null, this.actionType);
				this.appendToFile("", "", this.actionType, validator.getMessage(), translation);
			}
		}
		else if(action instanceof ActionCondition) {
			ActionCondition actionCondition = (ActionCondition) action;
			for(Check check : actionCondition.getConditions()) {
				if(check.getDefaultError() != null) {
					String translation = this.getTranslation(check.getDefaultError(), "", null, this.actionType);
					this.appendToFile("", "", this.actionType, check.getDefaultError(), translation);
				}
			}
		}
	}

	private void exportMenuActions() {
		for (MetaActionMenu actionMenu : MetaActionMenu.findByModule(this.currentModule).order("name").fetch()) {
			String translation = this.getTranslation(actionMenu.getTitle(), "", null, this.actionMenuType);
			this.appendToFile("", "", this.actionMenuType, actionMenu.getTitle(), translation);
		}
	}

	private void exportViews() {
		for (MetaView view : MetaView.findByModule(this.currentModule).order("name").order("type").fetch()) {
			AbstractView abstractView = this.fromXML(view);
			if(abstractView != null) {
				this.loadView(abstractView);
			}
		}
	}

	private AbstractView fromXML(MetaView view) {
		try {
			ObjectViews views = (ObjectViews) XMLViews.fromXML(view.getXml());
			if (views != null && views.getViews() != null)
					return views.getViews().get(0);
		}
		catch(Exception ex) {
			log.error("Error while exporting {}.", view.getName());
			log.error("Unable to export data.");
			log.error("With following exception:", ex);
		}
		return null;
	}

	private void loadView(AbstractView abstractView) {
		this.loadAbstractView(abstractView);
		if(abstractView instanceof FormView) {
			for (AbstractWidget widget : ((FormView) abstractView).getItems()) {
				this.loadWidget(abstractView, widget);
			}
		}
		else if(abstractView instanceof GridView) {
			for (AbstractWidget widget : ((GridView) abstractView).getItems()) {
				this.loadWidget(abstractView, widget);
			}
		}
		else if(abstractView instanceof TreeView) {
			for (TreeColumn column : ((TreeView) abstractView).getColumns()) {
				this.loadTreeColumn(abstractView, column);
			}
		}
		else if(abstractView instanceof Portal) {
			for (AbstractWidget widget : ((Portal) abstractView).getItems()) {
				this.loadWidget(abstractView, widget);
			}
		}
		else if(abstractView instanceof CalendarView) {
			for (AbstractWidget widget : ((CalendarView) abstractView).getItems()) {
				this.loadWidget(abstractView, widget);
			}
		}
		else if (abstractView instanceof SearchFilters) {
			this.exportSearchFilters((SearchFilters) abstractView);
		}
		else if (abstractView instanceof Search) {
			this.exportSearch((Search) abstractView);
		}
	}

	private void exportSearch(Search abstractView) {
		for (SearchSelect searchSelect : abstractView.getSelects()) {
			if(!Strings.isNullOrEmpty(searchSelect.getDefaultTitle())) {
				String translation = this.getTranslation(searchSelect.getDefaultTitle(), "", null, this.searchType);
				this.appendToFile("", "", this.searchType, searchSelect.getDefaultTitle(), translation);
			}
		}
		for (SearchField searchField : abstractView.getSearchFields()) {
			if(!Strings.isNullOrEmpty(searchField.getDefaultTitle())) {
				String translation = this.getTranslation(searchField.getDefaultTitle(), "", null, this.searchType);
				this.appendToFile("", "", this.searchType, searchField.getDefaultTitle(), translation);
			}
			else {
				String translation = this.getTranslation(searchField.getName(), "", null, this.searchType);
				this.appendToFile("", "", this.searchType, searchField.getName(), translation);
			}
		}
		for (SearchField searchField : abstractView.getResultFields()) {
			if(!Strings.isNullOrEmpty(searchField.getDefaultTitle())) {
				String translation = this.getTranslation(searchField.getDefaultTitle(), "", null, this.searchType);
				this.appendToFile("", "", this.searchType, searchField.getDefaultTitle(), translation);
			}
			else {
				String translation = this.getTranslation(searchField.getName(), "", null, this.searchType);
				this.appendToFile("", "", this.searchType, searchField.getName(), translation);
			}
		}
	}

	private void loadTreeColumn(AbstractView abstractView, TreeColumn column) {
		if(!Strings.isNullOrEmpty(column.getDefaultTitle())) {
			String translation = this.getTranslation(column.getDefaultTitle(), "", null, this.treeType);
			this.appendToFile("", "", this.treeType, column.getDefaultTitle(), translation);
		}
	}

	private void loadButton(AbstractView abstractView, Button button) {
		if(!Strings.isNullOrEmpty(button.getDefaultTitle())) {
			String translation = this.getTranslation(button.getDefaultTitle(), "", abstractView.getModel(), this.buttonType);
			this.appendToFile(abstractView.getModel(), "", this.buttonType, button.getDefaultTitle(), translation);
		}

		if(!Strings.isNullOrEmpty(button.getDefaultPrompt())) {
			String translation = this.getTranslation(button.getDefaultPrompt(), "", abstractView.getModel(), this.buttonType);
			this.appendToFile(abstractView.getModel(), "", this.buttonType, button.getDefaultPrompt(), translation);
		}

		if(!Strings.isNullOrEmpty(button.getDefaultHelp())) {
			String translation = this.getTranslation(button.getDefaultHelp(), "", abstractView.getModel(), this.buttonType);
			this.appendToFile(abstractView.getModel(), "", this.buttonType, button.getDefaultHelp(), translation);
		}
	}

	private void loadSimpleWidget(SimpleWidget widget, String model, String type) {

		if(!Strings.isNullOrEmpty(widget.getDefaultTitle())) {
			String translation = this.getTranslation(widget.getDefaultTitle(), "", model, type);
			this.appendToFile(model, "", type, widget.getDefaultTitle(), translation);
		}

		if(!Strings.isNullOrEmpty(widget.getDefaultHelp())) {
			String translation = this.getTranslation(widget.getDefaultHelp(), "", model, type);
			this.appendToFile(model, "", type, widget.getDefaultHelp(), translation);
		}

	}

	private void loadAbstractView(AbstractView abstractView) {
		if(abstractView.getToolbar() != null) {
			for (Button button : abstractView.getToolbar()) {
				this.loadButton(abstractView, button);
			}
		}
		if(abstractView.getMenubar() != null) {
			for (Menu menu : abstractView.getMenubar()) {
				this.loadMenu(abstractView, menu);
			}
		}

		String translation = this.getTranslation(abstractView.getDefaultTitle(), "", abstractView.getModel(), this.viewType);
		this.appendToFile(abstractView.getModel(), "", this.viewType, abstractView.getDefaultTitle(), translation);

	}

	private void loadMenu(AbstractView abstractView, Menu menu) {
		if(!Strings.isNullOrEmpty(menu.getTitle())) {
			String translation = this.getTranslation(menu.getDefaultTitle(), "", abstractView.getModel(), this.buttonType);
			this.appendToFile(abstractView.getModel(), "", this.buttonType, menu.getDefaultTitle(), translation);
		}
		if(menu.getItems() != null) {
			for (Item item : menu.getItems()) {
				this.loadMenuItem(abstractView, item);
			}
		}
	}

	private void loadMenuItem(AbstractView abstractView, MenuItem menuItem) {
		if(!Strings.isNullOrEmpty(menuItem.getDefaultTitle())) {
			String translation = this.getTranslation(menuItem.getDefaultTitle(), "", abstractView.getModel(), this.buttonType);
			this.appendToFile(abstractView.getModel(), "", this.buttonType, menuItem.getDefaultTitle(), translation);
		}
	}

	private void loadWidget(AbstractView abstractView, AbstractWidget widget) {
		if(widget instanceof AbstractContainer) {
			this.loadContainer(abstractView, (AbstractContainer) widget);
		}
		else if(widget instanceof Button) {
			this.loadButton(abstractView, (Button) widget);
		}
		else if(widget instanceof Separator) {
			this.loadSimpleWidget((SimpleWidget) widget, abstractView.getModel(), this.separatorType);
		}
		else if(widget instanceof Label) {
			this.loadSimpleWidget((SimpleWidget) widget, abstractView.getModel(), this.labelType);
		}
		else if(widget instanceof Field) {
			Field field = (Field) widget;
			this.exportField(abstractView, field);
			if(field.getViews() != null) {
				for (AbstractView subAbstractView : field.getViews()) {
					this.loadView(subAbstractView);
				}
			}
		}
	}

	private void exportField(AbstractView abstractView, Field field) {
		if(!Strings.isNullOrEmpty(field.getDefaultTitle()) || !Strings.isNullOrEmpty(field.getDefaultHelp())) {
			String translation = this.getTranslation(field.getDefaultTitle(), "", abstractView.getModel(), this.fieldType);
			String translationHelp = this.getTranslation(field.getDefaultHelp(), "", abstractView.getModel(), this.helpType);
			this.appendToFile(abstractView.getModel(), "", this.viewFieldType, field.getDefaultTitle(), translation, field.getDefaultHelp(), translationHelp);
		}
		if(!Strings.isNullOrEmpty(field.getDefaultPlaceholder())) {
			String translation = this.getTranslation(field.getDefaultPlaceholder(), "", abstractView.getModel(), this.placeholderType);
			this.appendToFile(abstractView.getModel(), "", this.placeholderType, field.getDefaultPlaceholder(), translation);
		}
		if(internalFields.contains(field.getName()) && Strings.isNullOrEmpty(field.getDefaultTitle())) {
			String translation = this.getTranslation(field.getName(), "", abstractView.getModel(), this.fieldType);
			this.appendToFile(abstractView.getModel(), field.getName(), this.fieldType, field.getDefaultTitle(), translation);
		}
	}

	private void loadContainer(AbstractView view, AbstractContainer container) {
		if(container instanceof Notebook) {
			if(((Notebook)container).getPages() != null) {
				for (Page page : ((Notebook)container).getPages()) {
					this.loadWidget(view, (AbstractWidget) page) ;
				}
			}
		}
		else if(container instanceof Portlet) {
			this.loadSimpleWidget((Portlet) container, "", this.portletType);
		}
		else if(container instanceof SimpleContainer) {
			SimpleContainer simpleContainer = (SimpleContainer) container;

			if(simpleContainer.getItems() != null) {
				if(simpleContainer instanceof Group)
					this.loadSimpleWidget((SimpleWidget) container, view.getModel(), this.groupType);
				else if(simpleContainer instanceof Page)
					this.loadSimpleWidget((SimpleWidget) container, view.getModel(), this.pageType);

				for (AbstractWidget widget : simpleContainer.getItems()) {
					this.loadWidget(view, widget) ;
				}
			}
		}
	}

	private void exportObjects() {
		List<URL> files = MetaScanner.findAll(this.currentModule, "domains", "(.*?)\\.xml");

		for(URL file : files) {
			this.exportFile(file);
		}
	}

	private void exportFile(URL file) {
		try {
			DomainModels object = this.unmarshalObject(file.openStream());
			for (Entity entity : object.getEntities()) {
				this.exportEntity(entity, object.getModule());
			}
		}
		catch(Exception ex) {
			log.error("Error while exporting {}.", file.getFile());
			log.error("Unable to export data.");
			log.error("With following exception:", ex);
		}
	}

	private void exportEntity(Entity entity, Module module) throws IOException {
		String packageName = module.getPackageName()+"."+entity.getName();

		for (Property field : entity.getFields()) {
			String translation = this.getTranslation(field.getName(), "", packageName, this.fieldType);
			String translationHelp = this.getTranslation(field.getName(), "", packageName, this.helpType);
			this.appendToFile(packageName, field.getName(), this.fieldType, field.getTitle(), translation, field.getHelp(), translationHelp);
		}
	}

	private void exportMenus() {
		for (MetaMenu menu : MetaMenu.findByModule(this.currentModule).order("name").fetch()) {
			String translation = this.getTranslation(menu.getTitle(), "", null, this.menuType);
			this.appendToFile("", "", this.menuType, menu.getTitle(), translation);
		}
	}

	private void exportSelects() {
		for (MetaSelect select : MetaSelect.findByModule(this.currentModule).order("name").fetch()) {
			for (MetaSelectItem item : select.getItems()) {
				String translation = this.getTranslation(item.getTitle(), "", null, this.selectType);
				this.appendToFile("", "", this.selectType, item.getTitle(), translation);
			}
		}
	}

	private void exportDoc(List<String> viewList, List<String> menuList) {
		Class<?> klass = ActionView.class;
		for (String name : menuList) {
			MetaAction action = MetaAction.all().filter("self.name = ?1 AND self.module = ?2 AND self.type = ?3", name, currentModule, CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_HYPHEN, klass.getSimpleName())).fetchOne();
			if(action != null) {
				this.sendToDoc(name, name, "");
			}
		}

		for (String name : viewList) {
			try {
				MetaView view = MetaView.all().filter("self.name = ?1 AND self.module = ?2", name, currentModule).fetchOne();
				if(view != null) {
					AbstractView abstractView = this.fromXML(view);
					if(abstractView != null) {
						this.viewToDoc(abstractView, abstractView.getName(), 1);
					}
				}
			}
			catch(Exception ex) {
				ex.printStackTrace();
			}
		}

		this.loadDoc();
	}

	private void abstractViewToDoc(AbstractView abstractView) {
		if(abstractView.getToolbar() != null) {
			for (Button button : abstractView.getToolbar()) {
				this.sendToDoc(abstractView.getName(), button.getName(), button.getDefaultHelp());
			}
		}
		if(abstractView.getMenubar() != null) {
			for (Menu menu : abstractView.getMenubar()) {
				if(menu.getItems() != null) {
					for (Item item : menu.getItems()) {
						this.sendToDoc(abstractView.getName(), item.getName(), "");
					}
				}
			}
		}
	}

	private void viewToDoc(AbstractView abstractView, String viewName, int level) {

		this.sendToDoc(abstractView.getName(), viewName, "");

		if(level == 1) {
			this.abstractViewToDoc(abstractView);
		}

		if(abstractView instanceof FormView) {
			for (AbstractWidget widget : ((FormView) abstractView).getItems()) {
				this.widgetToDoc(viewName, widget, level) ;
			}
		}
	}

	private void containerToDoc(String viewName, AbstractContainer container, int level) {
		if(container instanceof Notebook) {
			if(((Notebook)container).getPages() != null) {
				for (Page page : ((Notebook)container).getPages()) {
					this.widgetToDoc(viewName, (AbstractWidget) page, level) ;
				}
			}
		}
		else if(container instanceof SimpleContainer) {
			SimpleContainer simpleContainer = (SimpleContainer) container;
			if(simpleContainer.getItems() != null) {
				for (AbstractWidget widget : simpleContainer.getItems()) {
					this.widgetToDoc(viewName, widget, level);
				}
			}
		}
	}

	private void widgetToDoc(String viewName, AbstractWidget widget, int level) {
		if(widget instanceof AbstractContainer) {
			AbstractContainer container = (AbstractContainer) widget;
			this.containerToDoc(viewName, container, level);
		}
		else if(widget instanceof Button) {
			this.sendToDoc(viewName, ( (Button) widget).getName(), ( (Button) widget).getDefaultHelp());
		}
		else if(widget instanceof Field) {
			Field field = (Field) widget;
			if(!field.getExport()) {
				return;
			}
			this.sendToDoc(viewName, field.getName(), field.getDefaultHelp());
			if(field.getDocumentation() && level == 1) {
				if(field.getViews() != null) {
					for (AbstractView subAbstractView : field.getViews()) {
						this.viewToDoc(subAbstractView, viewName, level+1);
					}
				}
				else if(field.getFormView() != null) {
					MetaView view = MetaView.all().filter("self.name = ?1 AND self.module = ?2", field.getFormView(), currentModule).fetchOne();
					if(view != null) {
						AbstractView subAbstractView = this.fromXML(view);
						if(subAbstractView != null) {
							this.viewToDoc(subAbstractView, subAbstractView.getName(), level+1);
						}
					}
				}
				else {
					String name = this.findView(field.getTarget(), "form");
					MetaView view = MetaView.all().filter("self.name = ?1 AND self.module = ?2", name, currentModule).fetchOne();
					if(view != null) {
						AbstractView subAbstractView = this.fromXML(view);
						if(subAbstractView != null) {
							this.viewToDoc(subAbstractView, subAbstractView.getName(), level+1);
						}
					}
				}
			}
		}
	}

	private void sendToDoc(String key, String value, String help) {
		if(Strings.isNullOrEmpty(key) || Strings.isNullOrEmpty(value)) {
			return;
		}

		if(!doc.containsKey(key)) {
			Map<String, String> map = Maps.newLinkedHashMap();
			map.put(value, help);
			doc.put(key, map);
		}
		else {
			Map<String, String> map = doc.get(key);
			if(!map.containsKey(value)) {
				map.put(value, help);
			}
		}
	}

	@SuppressWarnings("rawtypes")
	private void loadDoc() {
		for (String key : doc.keySet()) {
			Map<String, String> values = doc.get(key);
			for (String value : values.keySet()) {
				String translation = JPA.translate(value, "", key, this.docType);
				String docTranslation = this.getTranslation(value, "", key, this.docType);
				List<Map> list = MetaView.all().filter("self.name = ?1 AND self.module = ?2", key, currentModule).select("model").fetch(1, 0);
				String helpTranslation = "";
				if(list != null && !list.isEmpty()) {
					helpTranslation = this.getTranslation(value, "", list.get(0).get("model").toString(), this.helpType);
				}
				this.appendToFile(key, value, this.docType, translation, docTranslation, values.get(value), helpTranslation);
			}

		}
		doc.clear();
	}

	private String createHeader() {

		boolean first = true;
		StringBuilder sb = new StringBuilder();
		List<String> headerList = ImmutableList.of("domain", "name", "type", "title", "title_t", "help", "help_t");

		for (String column : headerList) {

			if(!first) {
				sb.append(",");
			}

			first = false;
			sb.append("\"").append(column).append("\"");
		}

		return sb.append("\n").toString();
	}

	private String getTranslation(String key, String defaultValue, String domain, String type) {
		MetaTranslation translation = null;

		List<Object> params = Lists.newArrayList();
		String query = "self.key = ?1 AND self.language = ?2";
		params.add(key);
		params.add(this.exportLanguage);

		if(!Strings.isNullOrEmpty(domain)) {
			query += " AND (self.domain IS NULL OR self.domain = ?3)";
			params.add(domain);
		}

		if(type != null) {
			query += " AND self.type = ?" + (params.size() + 1);
			params.add(type);
		}
		else {
			query += " AND self.type IS NULL";
		}

		translation = MetaTranslation.all().filter(query, params.toArray()).order("domain").fetchOne();

		if (translation != null && !Strings.isNullOrEmpty(translation.getTranslation())) {
			return translation.getTranslation();
		}
		return defaultValue;
	}

	private DomainModels unmarshalObject(InputStream openInputStream) {
		try {
			JAXBContext context = JAXBContext.newInstance(DomainModels.class);
			Unmarshaller unmarshaller = context.createUnmarshaller();

			SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
			Schema schema = schemaFactory.newSchema(Resources.getResource(LOCAL_SCHEMA_DOMAIN));

			unmarshaller.setSchema(schema);
			return (DomainModels) unmarshaller.unmarshal(openInputStream);
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private void appendToFile(String domain, String name, String type, String title, String titleT, String... more) {
		StringBuilder sb = new StringBuilder();
		sb.append("\"").append(domain == null ? "" : domain).append("\"").append(",");
		sb.append("\"").append(name == null ? "" : name).append("\"").append(",");
		sb.append("\"").append(type == null ? "" : type).append("\"").append(",");
		sb.append("\"").append(title == null ? "" : title).append("\"").append(",");
		sb.append("\"").append(titleT == null ? "" : titleT).append("\"").append(",");
		if(more == null || more.length == 0){
			sb.append("\"").append("\"").append(",").append("\"").append("\"");
		}
		else {
			if(more.length >= 1) sb.append("\"").append(more[0] == null ? "" : more[0]).append("\"").append(",");
			if(more.length >= 2) sb.append("\"").append(more[1] == null ? "" : more[1]).append("\"");
		}
		sb.append("\n");
		this.appendToFile(sb.toString());
	}

	private void appendToFile(String content) {
		if(Strings.isNullOrEmpty(content)) {
			return;
		}

		try {
			if(!this.exportFile.exists()) {
				Files.createParentDirs(this.exportFile);
				Files.write(this.createHeader(), this.exportFile, Charsets.UTF_8);
			}

			Files.append(content, this.exportFile, Charsets.UTF_8);
		}
		catch(Exception ex) {
			log.error("Error while append content to file : {}", ex);
		}
	}

	public void loadViewFromEntry(List<String> viewList, List<String> menuList, String parent) {
		List<MetaMenu> menu = this.getMenus(parent);

		for (MetaMenu item : menu) {
			if(item.getAction() != null) {
				this.addView(viewList, menuList, item);
			}
			else {
				this.loadViewFromEntry(viewList, menuList, item.getName());
			}
		}
	}

	private void addView(List<String> viewList, List<String> menuList, MetaMenu item) {
		MetaAction metaAction = item.getAction();
		ObjectViews views;

		try {
			views = (ObjectViews) XMLViews.fromXML(metaAction.getXml());
		} catch (JAXBException e) {
			return ;
		}

		if(views.getActions() != null && views.getActions().size() > 0) {
			this.addView(viewList, menuList, views.getActions().get(0));
		}
	}

	private void addView(List<String> viewList, List<String> menuList, Action action) {
		if(action instanceof ActionView) {
			ActionView actionView = (ActionView) action;
			if(!menuList.contains(actionView.getName())) {
				menuList.add(actionView.getName());
			}
			this.addView(viewList, actionView);
		}
	}

	private void addView(List<String> viewList, ActionView action) {
		if(action.getViews() == null) {
			return ;
		}
		for (View view : action.getViews()) {
			if(!"form".equals(view.getType())) {
				continue;
			}

			String viewName = view.getName();
			if(Strings.isNullOrEmpty(viewName)) {
				viewName = this.findView(action.getModel(), view.getType());
			}
			if(!viewList.contains(viewName)) {
				viewList.add(viewName);
			}
			break;
		}
	}

	private String findView(String model, String type) {
		MetaView view = MetaView.findByType(type, model);
		if(view != null) {
			return view.getName();
		}
		return null;
	}

	private List<MetaMenu> getMenus(String parent) {
		String filter = "";

		if(Strings.isNullOrEmpty(parent)) {
			filter += "self.parent IS NULL";
		}
		else {
			filter += "self.parent.name = '" + parent+"'";
		}

		return JPA.all(MetaMenu.class).filter(filter).order("-priority").order("id").fetch();
	}
}
