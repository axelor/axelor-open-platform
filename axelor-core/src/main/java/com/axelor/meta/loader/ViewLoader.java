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
package com.axelor.meta.loader;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.inject.Singleton;
import javax.xml.bind.JAXBException;

import org.reflections.vfs.Vfs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.axelor.auth.db.Group;
import com.axelor.common.FileUtils;
import com.axelor.common.StringUtils;
import com.axelor.db.JPA;
import com.axelor.db.mapper.Mapper;
import com.axelor.db.mapper.Property;
import com.axelor.meta.MetaScanner;
import com.axelor.meta.db.MetaAction;
import com.axelor.meta.db.MetaActionMenu;
import com.axelor.meta.db.MetaChart;
import com.axelor.meta.db.MetaChartConfig;
import com.axelor.meta.db.MetaChartSeries;
import com.axelor.meta.db.MetaMenu;
import com.axelor.meta.db.MetaSelect;
import com.axelor.meta.db.MetaSelectItem;
import com.axelor.meta.db.MetaView;
import com.axelor.meta.schema.ObjectViews;
import com.axelor.meta.schema.actions.Action;
import com.axelor.meta.schema.views.AbstractView;
import com.axelor.meta.schema.views.AbstractWidget;
import com.axelor.meta.schema.views.ChartView;
import com.axelor.meta.schema.views.Field;
import com.axelor.meta.schema.views.FormView;
import com.axelor.meta.schema.views.GridView;
import com.axelor.meta.schema.views.MenuItem;
import com.axelor.meta.schema.views.Selection;
import com.google.common.base.CaseFormat;
import com.google.common.base.Charsets;
import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.common.io.Files;
import com.google.inject.persist.Transactional;

@Singleton
public class ViewLoader implements Loader {

	protected Logger log = LoggerFactory.getLogger(ViewLoader.class);
	
	@Override
	@Transactional
	public void load(Module module) {
		for (Vfs.File file : MetaScanner.findAll(module.getName(), "views", "(.*?)\\.xml")) {
			log.info("importing: {}", file.getName());
			try {
				process(file.openInputStream(), module);
			} catch (IOException | JAXBException e) {
				throw Throwables.propagate(e);
			}
		}

		// generate default views
		importDefault(module);
	}
	
	private static <T> List<T> getList(List<T> list) {
		if (list == null) {
			return Lists.newArrayList();
		}
		return list;
	}
	
	//TODO: change the view overriding with same name
	//TODO: make name unique and add `overrides="original"` attribute
	private void process(InputStream stream, Module module) throws JAXBException {
		final ObjectViews all = XMLViews.unmarshal(stream);
		
		for (AbstractView view : getList(all.getViews())) {
			importView(view, module);
		}
		
		for (Selection selection : getList(all.getSelections())) {
			importSelection(selection, module);
		}
		
		for (Action action : getList(all.getActions())) {
			importAction(action, module);
		}
		
		for (MenuItem item : getList(all.getMenus())) {
			importMenu(item, module);
		}
		
		for (MenuItem item: getList(all.getActionMenus())) {
			importActionMenu(item, module);
		}
	}
	
	private void importView(AbstractView view, Module module) {

		String name = view.getName();
		String type = view.getType();
		String modelName = view.getModel();
		
		log.info("Loading view: {}", name);

		String xml = XMLViews.toXml(view, true);

		if (type.matches("tree|chart|portal|search")) {
			modelName = null;
		} else if (StringUtils.isBlank(modelName)) {
			throw new IllegalArgumentException("Invalid view, model name missing.");
		}
		
		if (view instanceof ChartView) {
			importChart((ChartView) view, module);
			return;
		}
		
		if (modelName != null) {
			Class<?> model;
			try {
				model = Class.forName(modelName);
			} catch (ClassNotFoundException e) {
				throw new IllegalArgumentException("Invalid view, model not found: " + modelName);
			}
			modelName = model.getName();
		}
		
		MetaView entity = MetaView.findByName(name);
		if (entity == null) {
			entity = new MetaView(name);
		}
		
		entity.setTitle(view.getDefaultTitle());
		entity.setType(type);
		entity.setModel(modelName);
		entity.setModule(module.getName());
		entity.setXml(xml);
		
		entity = entity.save();
	}
	
	private void importChart(ChartView view, Module module) {
		
		MetaChart entity = MetaChart.findByName(view.getName());
		if (entity == null) {
			entity = new MetaChart(view.getName());
		} else {
			entity.clearChartSeries();
			entity.clearChartConfig();
		}
		
		entity.setModule(module.getName());
		entity.setTitle(view.getDefaultTitle());
		entity.setStacked(view.getStacked());
		
		String query = StringUtils.stripIndent(view.getQuery().getText());
		entity.setQuery(query);
		entity.setQueryType(view.getQuery().getType());

		entity.setCategoryKey(view.getCategory().getKey());
		entity.setCategoryType(view.getCategory().getType());
		entity.setCategoryTitle(view.getCategory().getDefaultTitle());
		
		for(ChartView.ChartSeries series : view.getSeries()) {
			MetaChartSeries item = new MetaChartSeries();
			item.setKey(series.getKey());
			item.setGroupBy(series.getGroupBy());
			item.setType(series.getType());
			item.setSide(series.getSide());
			item.setAggregate(series.getAggregate());
			entity.addChartSeries(item);
		}

		if (view.getConfig() != null) {
			for(ChartView.ChartConfig config : view.getConfig()) {
				MetaChartConfig item = new MetaChartConfig();
				item.setName(config.getName());
				item.setValue(config.getValue());
				entity.addChartConfig(item);
			}
		}
		
		entity.save();
	}
	
	private void importSelection(Selection selection, Module module) {
		log.info("Loading selection : {}", selection.getName());
		MetaSelect select = MetaSelect.findByName(selection.getName());
		if (select == null) {
			select = new MetaSelect(selection.getName());
		} else {
			select.clearItems();
		}
		
		select.setModule(module.getName());
		
		int sequence = 0;
		for(Selection.Option opt : selection.getOptions()) {
			MetaSelectItem item = new MetaSelectItem();
			item.setValue(opt.getValue());
			item.setTitle(opt.getDefaultTitle());
			item.setOrder(sequence++);
			select.addItem(item);
		}
		
		select.save();
	}
	
	private Set<Group> findGroups(String groups) {
		if (StringUtils.isBlank(groups)) {
			return null;
		}
		
		Set<Group> all = Sets.newHashSet();
		for(String name : groups.split(",")) {
			Group group = Group.all().filter("self.code = ?1", name).fetchOne();
			if (group == null) {
				log.info("Creating a new user group: {}", name);
				group = new Group();
				group.setCode(name);
				group.setName(name);
				group = group.save();
			}
			all.add(group);
		}

		return all;
	}
	
	private Multimap<String, MetaMenu> unresolved_menus = HashMultimap.create();
	private Multimap<String, MetaMenu> unresolved_actions = HashMultimap.create();
	
	private void importAction(Action action, Module module) {
		log.info("Loading action : {}", action.getName());
		
		Class<?> klass = action.getClass();
		Mapper mapper = Mapper.of(klass);
	
		MetaAction entity = MetaAction.findByName(action.getName());
		if (entity == null) {
			entity = new MetaAction(action.getName());
		}
		
		entity.setXml(XMLViews.toXml(action,  true));
		
		String model = (String) mapper.get(action, "model");
		entity.setModel(model);
		entity.setModule(module.getName());

		String type = CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_HYPHEN, klass.getSimpleName());
		entity.setType(type);

		entity = entity.save();
		
		for (MetaMenu pending : unresolved_actions.get(entity.getName())) {
			log.info("Resolved menu: {}", pending.getName());
			pending.setAction(entity);
			pending.save();
		}
		unresolved_actions.removeAll(entity.getName());
	}
	
	private void importMenu(MenuItem menuItem, Module module) {
		
		log.info("Loading menu : {}", menuItem.getName());

		MetaMenu menu = MetaMenu.findByName(menuItem.getName());
		if (menu == null) {
			menu = new MetaMenu(menuItem.getName());
		}
		
		menu.setPriority(menuItem.getPriority());
		menu.setTitle(menuItem.getDefaultTitle());
		menu.setIcon(menuItem.getIcon());
		menu.setModule(module.getName());
		menu.setTop(menuItem.getTop());
		menu.setLeft(menuItem.getLeft() == null ? true : menuItem.getLeft());
		menu.setMobile(menuItem.getMobile());
		
		if (menu.getId() == null) {
			menu.setGroups(this.findGroups(menuItem.getGroups()));
		}
		
		if (!Strings.isNullOrEmpty(menuItem.getParent())) {
			MetaMenu parent = MetaMenu.findByName(menuItem.getParent());
			if (parent == null) {
				log.info("Unresolved parent : {}", menuItem.getParent());
				unresolved_menus.put(menuItem.getParent(), menu);
			} else {
				menu.setParent(parent);
			}
		}
		
		if (!StringUtils.isBlank(menuItem.getAction())) {
			MetaAction action = MetaAction.findByName(menuItem.getAction());
			if (action == null) {
				log.info("Unresolved action: {}", menuItem.getAction());
				unresolved_actions.put(menuItem.getAction(), menu);
			} else {
				menu.setAction(action);
			}
		}
		
		menu = menu.save();

		for (MetaMenu pending : unresolved_menus.get(menu.getName())) {
			log.info("Resolved menu : {}", pending.getName());
			pending.setParent(menu);
			pending.save();
		}

		unresolved_menus.removeAll(menu.getName());
	}
	
	private Multimap<String, MetaActionMenu> unresolved_action_menus = HashMultimap.create();
	private Multimap<String, MetaActionMenu> unresolved_action_actions = HashMultimap.create();

	private void importActionMenu(MenuItem menuItem, Module module) {
		log.info("Loading action menu : {}", menuItem.getName());

		MetaActionMenu menu = MetaActionMenu.findByName(menuItem.getName());
		if (menu == null) {
			menu = new MetaActionMenu(menuItem.getName());
		}

		menu.setTitle(menuItem.getDefaultTitle());
		menu.setModule(module.getName());
		menu.setCategory(menuItem.getCategory());
		
		if (!StringUtils.isBlank(menuItem.getParent())) {
			MetaActionMenu parent = MetaActionMenu.findByName(menuItem.getParent());
			if (parent == null) {
				log.info("Unresolved parent : {}", menuItem.getParent());
				unresolved_action_menus.put(menuItem.getParent(), menu);
			} else {
				menu.setParent(parent);
			}
		}

		if (!Strings.isNullOrEmpty(menuItem.getAction())) {
			MetaAction action = MetaAction.findByName(menuItem.getAction());
			if (action == null) {
				log.info("Unresolved action: {}", menuItem.getAction());
				unresolved_action_actions.put(menuItem.getAction(), menu);
			} else {
				menu.setAction(action);
			}
		}

		menu = menu.save();

		for (MetaActionMenu pending : unresolved_action_menus.get(menu.getName())) {
			log.info("Resolved action menu : {}", pending.getName());
			pending.setParent(menu);
			pending.save();
		}

		unresolved_action_actions.removeAll(menu.getName());
	}

	private static final File outputDir = FileUtils.getFile(System.getProperty("java.io.tmpdir"), "axelor", "generated");

	private void importDefault(Module module) {
		for (Class<?> klass: JPA.models()) {
			if (module.hasEntity(klass) && MetaView.all().filter("self.model = ?1", klass.getName()).count() == 0) {
				File out = FileUtils.getFile(outputDir, "views", klass.getSimpleName() + ".xml");
				String xml = createDefaultViews(module, klass);
				try {
					log.info("Creating default views: {}", out);
					Files.createParentDirs(out);
					Files.write(xml, out, Charsets.UTF_8);
				} catch (IOException e) {
					log.error("Unable to create: {}", out);
				}
			}
		}
	}
	
	@SuppressWarnings("all")
	private String createDefaultViews(Module module, final Class<?> klass) {

		final FormView formView = new FormView();
		final GridView gridView = new GridView();

		String name = CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_HYPHEN, klass.getSimpleName());
		String title = klass.getSimpleName();

		formView.setName(name + "-form");
		gridView.setName(name + "-grid");

		formView.setModel(klass.getName());
		gridView.setModel(klass.getName());

		formView.setTitle(title);
		gridView.setTitle(title);

		List<AbstractWidget> formItems = Lists.newArrayList();
		List<AbstractWidget> gridItems = Lists.newArrayList();

		Mapper mapper = Mapper.of(klass);
		List<String> fields = Lists.reverse(fieldNames(klass));

		for(String n : fields) {

			Property p = mapper.getProperty(n);

			if (p == null || p.isPrimary() || p.isVersion())
				continue;

			Field field = new Field();
			field.setName(p.getName());

			if (p.isCollection()) {
				field.setColSpan(4);
				field.setShowTitle(false);
			} else {
				gridItems.add(field);
			}
			formItems.add(field);
		}

		formView.setItems(formItems);
		gridView.setItems(gridItems);


		importView(formView, module);
		importView(gridView, module);

		return XMLViews.toXml(ImmutableList.of(gridView, formView), false);
	}
	
	// Fields names are not in ordered but some JVM implementation can.
	private List<String> fieldNames(Class<?> klass) {
		List<String> result = new ArrayList<String>();
		for(java.lang.reflect.Field field : klass.getDeclaredFields()) {
			if (!field.getName().matches("id|version|selected|created(By|On)|updated(By|On)")) {
				result.add(field.getName());
			}
		}
		if (klass.getSuperclass() != Object.class) {
			result.addAll(fieldNames(klass.getSuperclass()));
		}
		return Lists.reverse(result);
	}
}
