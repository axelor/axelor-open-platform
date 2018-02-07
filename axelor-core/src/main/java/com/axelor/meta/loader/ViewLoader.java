/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2018 Axelor (<http://axelor.com>).
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
package com.axelor.meta.loader;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.persistence.PersistenceException;
import javax.persistence.TypedQuery;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;

import com.axelor.auth.db.Group;
import com.axelor.auth.db.repo.GroupRepository;
import com.axelor.common.FileUtils;
import com.axelor.common.Inflector;
import com.axelor.common.ObjectUtils;
import com.axelor.common.StringUtils;
import com.axelor.db.JPA;
import com.axelor.db.mapper.Mapper;
import com.axelor.db.mapper.Property;
import com.axelor.inject.Beans;
import com.axelor.meta.MetaScanner;
import com.axelor.meta.MetaStore;
import com.axelor.meta.db.MetaAction;
import com.axelor.meta.db.MetaActionMenu;
import com.axelor.meta.db.MetaMenu;
import com.axelor.meta.db.MetaSelect;
import com.axelor.meta.db.MetaSelectItem;
import com.axelor.meta.db.MetaView;
import com.axelor.meta.db.repo.MetaActionMenuRepository;
import com.axelor.meta.db.repo.MetaActionRepository;
import com.axelor.meta.db.repo.MetaMenuRepository;
import com.axelor.meta.db.repo.MetaSelectRepository;
import com.axelor.meta.db.repo.MetaViewRepository;
import com.axelor.meta.schema.ObjectViews;
import com.axelor.meta.schema.actions.Action;
import com.axelor.meta.schema.actions.ActionView;
import com.axelor.meta.schema.views.AbstractView;
import com.axelor.meta.schema.views.AbstractWidget;
import com.axelor.meta.schema.views.Field;
import com.axelor.meta.schema.views.FormView;
import com.axelor.meta.schema.views.GridView;
import com.axelor.meta.schema.views.MenuItem;
import com.axelor.meta.schema.views.Panel;
import com.axelor.meta.schema.views.PanelField;
import com.axelor.meta.schema.views.PanelRelated;
import com.axelor.meta.schema.views.Selection;
import com.axelor.meta.service.MetaService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Charsets;
import com.google.common.base.Objects;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.io.Files;
import com.google.inject.persist.Transactional;

public class ViewLoader extends AbstractLoader {
	
	@Inject
	private ObjectMapper objectMapper;
	
	@Inject
	private MetaViewRepository views;

	@Inject
	private MetaSelectRepository selects;
	
	@Inject
	private MetaActionRepository actions;
	
	@Inject
	private MetaMenuRepository menus;
	
	@Inject
	private MetaActionMenuRepository actionMenus;
	
	@Inject
	private GroupRepository groups;

	@Override
	@Transactional
	protected void doLoad(Module module, boolean update) {
		for (URL file : MetaScanner.findAll(module.getName(), "views", "(.*?)\\.xml")) {
			log.debug("importing: {}", file.getFile());
			try {
				process(file.openStream(), module, update);
			} catch (IOException | JAXBException e) {
				throw new RuntimeException(e);
			}
		}
	}

	@Override
	@Transactional
	void doLast(Module module, boolean update) {
		// generate default views
		importDefault(module);
		
		Set<?> unresolved = this.unresolvedKeys();
		if (unresolved.size() > 0) {
			log.error("unresolved items: {}", unresolved);
			throw new PersistenceException("There are some unresolve items, check the log.");
		}
	}

	private static <T> List<T> getList(List<T> list) {
		if (list == null) {
			return Lists.newArrayList();
		}
		return list;
	}

	@Transactional
	void updateFrom(Path file, String moduleName) throws IOException, JAXBException {
		final Module module = ModuleManager.getModule(moduleName);
		try (FileInputStream stream = new FileInputStream(file.toFile())) {
			process(stream, module, true);
		} finally {
			doCleanUp();
		}
	}

	void process(InputStream stream, Module module, boolean update) throws JAXBException {
		final ObjectViews all = XMLViews.unmarshal(stream);

		for (AbstractView view : getList(all.getViews())) {
			importView(view, module, update);
		}

		for (Selection selection : getList(all.getSelections())) {
			importSelection(selection, module, update);
		}

		for (Action action : getList(all.getActions())) {
			importAction(action, module, update);
			MetaStore.invalidate(action.getName());
		}

		for (MenuItem item : getList(all.getMenus())) {
			importMenu(item, module, update);
		}

		for (MenuItem item: getList(all.getActionMenus())) {
			importActionMenu(item, module, update);
		}
	}
	
	private void importView(AbstractView view, Module module, boolean update) {
		importView(view, module, update, -1);
	}
	
	private void importView(AbstractView view, Module module, boolean update, int priority) {

		String xmlId = view.getXmlId();
		String name = view.getName();
		String type = view.getType();
		String modelName = view.getModel();

		if (StringUtils.isBlank(xmlId)) {
			if (isVisited(view.getClass(), name)) {
				log.error("duplicate view without 'id': {}", name);
				return;
			}
		} else if (isVisited(view.getClass(), xmlId)) {
			return;
		}

		log.debug("Loading view: {}", name);
		
		String xml = XMLViews.toXml(view, true);

		if (type.matches("tree|chart|portal|dashboard|search|custom")) {
			modelName = null;
		} else if (StringUtils.isBlank(modelName)) {
			throw new IllegalArgumentException("Invalid view, model name missing.");
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
		
		MetaView entity = views.findByID(xmlId);
		MetaView other = views.findByName(name);
		if (entity == null && StringUtils.isBlank(xmlId)) {
			entity = views.all().filter("self.name = ? AND self.module = ? AND self.xmlId is null", name, module.getName()).fetchOne();
		}
		
		if (entity == null) {
			entity = new MetaView(name);
		}
		
		if (other == entity) {
			other = null;
		}

		// set priority higher to existing view
		if (entity.getId() == null && other != null && !Objects.equal(xmlId, other.getXmlId())
				&& view.getExtension() != Boolean.TRUE) {
			entity.setPriority(other.getPriority() + 1);
		}

		if (entity.getId() != null && !update) {
			return;
		}

		if (priority > -1) {
			entity.setPriority(priority);
		}

		// delete personalized dashboards
		if ("dashboard".equals(type) && !xml.equals(entity.getXml())) {
			int deleted = Beans.get(MetaService.class).removeCustomViews(entity);
			if (deleted > 0) {
				log.info(deleted + " custom views are deleted : " + entity.getName());
			}
		}

		entity.setXmlId(xmlId);
		entity.setTitle(view.getTitle());
		entity.setType(type);
		entity.setModel(modelName);
		entity.setModule(module.getName());
		entity.setXml(xml);
		entity.setGroups(this.findGroups(view.getGroups(), entity.getGroups()));
		entity.setExtension(view.getExtension());
		
		if (entity.getTitle() == null) {
			entity.setTitle(name);
		}

		if (entity.getHelpLink() == null) {
			entity.setHelpLink(view.getHelpLink());
		}

		entity = views.save(entity);
	}

	private void importSelection(Selection selection, Module module, boolean update) {

		String name = selection.getName();
		String xmlId = selection.getXmlId();

		if (StringUtils.isBlank(xmlId)) {
			if (isVisited(Selection.class, name)) {
				log.error("duplicate selection without 'id': {}", name);
				return;
			}
		} else if (isVisited(Selection.class, xmlId)) {
			return;
		}

		log.debug("Loading selection : {}", name);

		MetaSelect entity = selects.findByID(xmlId);
		MetaSelect other = selects.findByName(selection.getName());
		if (entity == null) {
			entity = selects.all().filter("self.name = ? AND self.module = ?", name, module.getName()).fetchOne();
		}

		if (entity == null) {
			entity = new MetaSelect(selection.getName());
			entity.setXmlId(xmlId);
		}
		
		if (other == entity) {
			other = null;
		}

		// set priority higher to existing view
		if (entity.getId() == null && other != null && !Objects.equal(xmlId, other.getXmlId())) {
			entity.setPriority(other.getPriority() + 1);
		}
		
		if (entity.getId() != null && !update) {
			return;
		}

		entity.clearItems();
		entity.setModule(module.getName());

		int sequence = 0;
		for(Selection.Option opt : selection.getOptions()) {

			MetaSelectItem item = new MetaSelectItem();
			Integer seq = sequence++;

			if (opt.getOrder() != null) {
				seq = opt.getOrder();
			}

			item.setValue(opt.getValue());
			item.setTitle(opt.getTitle());
			item.setIcon(opt.getIcon());
			item.setOrder(seq);
			item.setHidden(opt.getHidden());

			entity.addItem(item);
			if (opt.getDataAttributes() == null) {
				continue;
			}

			Map<String, Object> data = Maps.newHashMap();
			for (QName param : opt.getDataAttributes().keySet()) {
				String paramName = param.getLocalPart();
				if (paramName.startsWith("data-")) {
					data.put(paramName.substring(5), opt.getDataAttributes().get(param));
				}
			}
			try {
				item.setData(objectMapper.writeValueAsString(data));
			} catch (JsonProcessingException e) {
			}
		}

		selects.save(entity);
	}

	private Set<Group> findGroups(String names, Set<Group> existing) {
		if (StringUtils.isBlank(names)) {
			return existing;
		}

		Set<Group> all = ObjectUtils.isEmpty(existing) ? new HashSet<Group>() : Sets.newHashSet(existing);
		for(String code : names.split(",")) {
			Group group = groups.all().filter("self.code = ?1", code).fetchOne();
			if (group == null) {
				log.info("Creating a new user group: {}", code);
				group = new Group();
				group.setCode(code);
				group.setName(code);
				group = groups.save(group);
			}
			all.add(group);
		}

		return all;
	}

	private void importAction(Action action, Module module, boolean update) {

		String name = action.getName();
		String xmlId = action.getXmlId();

		if (StringUtils.isBlank(xmlId)) {
			if (isVisited(Action.class, name)) {
				log.error("duplicate action without 'id': {}", name);
				return;
			}
		} else if (isVisited(Action.class, xmlId)) {
			return;
		}

		log.debug("Loading action : {}", name);

		MetaAction entity = actions.findByID(xmlId);
		MetaAction other = actions.findByName(name);
		if (entity == null) {
			entity = actions.all()
					.filter("self.name = ? AND self.module = ?", name, module.getName())
					.fetchOne();
		}

		if (entity == null) {
			entity = new MetaAction(name);
		}

		if (other == entity) {
			other = null;
		}

		// set priority higher to existing menu
		if (entity.getId() == null && other != null && !Objects.equal(xmlId, other.getXmlId())) {
			entity.setPriority(other.getPriority() + 1);
		}

		if (entity.getId() != null && !update) {
			return;
		}

		Class<?> klass = action.getClass();
		Mapper mapper = Mapper.of(klass);

		entity.setXml(XMLViews.toXml(action,  true));

		String model = (String) mapper.get(action, "model");
		entity.setModel(model);
		entity.setModule(module.getName());

		String type = klass.getSimpleName().replaceAll("([a-z\\d])([A-Z]+)", "$1-$2").toLowerCase();
		entity.setType(type);

		if (action instanceof ActionView) {
			ActionView view = (ActionView) action;
			Boolean home = view.getHome();
			if (home == null) {
				for (ActionView.View item : view.getViews()) {
					if ("dashboard".equals(item.getType())) {
						home = Boolean.TRUE;
						break;
					}
				}
			}
			entity.setHome(home);
		}

		entity = actions.save(entity);

		for (MetaMenu pending : this.resolve(MetaMenu.class, entity.getName())) {
			log.debug("Resolved menu: {}", pending.getName());
			pending.setAction(entity);
			pending = menus.save(pending);
		}
	}

	private void importMenu(MenuItem menuItem, Module module, boolean update) {

		String name = menuItem.getName();
		String xmlId = menuItem.getXmlId();

		if (StringUtils.isBlank(xmlId)) {
			if (isVisited(MenuItem.class, name)) {
				log.error("duplicate menu without 'id': {}", name);
				return;
			}
		} else if (isVisited(MenuItem.class, xmlId)) {
			return;
		}

		log.debug("Loading menu : {}", name);

		MetaMenu entity = menus.findByID(xmlId);
		MetaMenu other = menus.findByName(name);
		if (entity == null) {
			entity = menus.all()
					.filter("self.name = ? AND self.module = ?", name, module.getName())
					.fetchOne();
		}

		if (entity == null) {
			entity = new MetaMenu(name);
		}

		if (other == entity) {
			other = null;
		}

		// set priority higher to existing menu
		if (entity.getId() == null && other != null && !Objects.equal(xmlId, other.getXmlId())) {
			entity.setPriority(other.getPriority() + 1);
		}
		
		if (entity.getId() != null && !update) {
			return;
		}

		entity.setTitle(menuItem.getTitle());
		entity.setIcon(menuItem.getIcon());
		entity.setIconBackground(menuItem.getIconBackground());
		entity.setModule(module.getName());
		entity.setTag(menuItem.getTag());
		entity.setTagGet(menuItem.getTagGet());
		entity.setTagCount(menuItem.getTagCount());
		entity.setTagStyle(menuItem.getTagStyle());
		entity.setTop(menuItem.getTop());
		entity.setLeft(menuItem.getLeft() == null ? true : menuItem.getLeft());
		entity.setMobile(menuItem.getMobile());
		entity.setHidden(menuItem.getHidden());
		entity.setGroups(this.findGroups(menuItem.getGroups(), entity.getGroups()));
		
		entity.setConditionToCheck(menuItem.getConditionToCheck());
		entity.setModuleToCheck(menuItem.getModuleToCheck());

		if (menuItem.getOrder() != null) {
			entity.setOrder(menuItem.getOrder());
		}

		if (!Strings.isNullOrEmpty(menuItem.getParent())) {
			MetaMenu parent = menus.findByName(menuItem.getParent());
			if (parent == null) {
				log.debug("Unresolved parent : {}", menuItem.getParent());
				this.setUnresolved(MetaMenu.class, menuItem.getParent(), entity);
			} else {
				entity.setParent(parent);
			}
		}

		if (!StringUtils.isBlank(menuItem.getAction())) {
			MetaAction action = actions.findByName(menuItem.getAction());
			if (action == null) {
				log.debug("Unresolved action: {}", menuItem.getAction());
				setUnresolved(MetaMenu.class, menuItem.getAction(), entity);
			} else {
				entity.setAction(action);
			}
		}

		entity = menus.save(entity);

		for (MetaMenu pending : this.resolve(MetaMenu.class, name)) {
			log.debug("Resolved menu : {}", pending.getName());
			pending.setParent(entity);
			pending = menus.save(pending);
		}
	}

	private void importActionMenu(MenuItem menuItem, Module module, boolean update) {
		String name = menuItem.getName();
		String xmlId = menuItem.getXmlId();

		if (StringUtils.isBlank(xmlId)) {
			if (isVisited(MenuItem.class, name)) {
				log.error("duplicate action menu without 'id': {}", name);
				return;
			}
		} else if (isVisited(MenuItem.class, xmlId)) {
			return;
		}

		log.debug("Loading action menu : {}", name);

		MetaActionMenu entity = actionMenus.findByID(xmlId);
		MetaActionMenu other = actionMenus.findByName(name);
		if (entity == null) {
			entity = actionMenus.all()
					.filter("self.name = ? AND self.module = ?", name, module.getName())
					.fetchOne();
		}

		if (entity == null) {
			entity = new MetaActionMenu(name);
		}

		if (other == entity) {
			other = null;
		}

		// set priority higher to existing menu
		if (entity.getId() == null && other != null && !Objects.equal(xmlId, other.getXmlId())) {
			entity.setPriority(other.getPriority() + 1);
		}

		if (entity.getId() != null && !update) {
			return;
		}

		entity.setTitle(menuItem.getTitle());
		entity.setModule(module.getName());
		entity.setCategory(menuItem.getCategory());

		if (menuItem.getOrder() != null) {
			entity.setOrder(menuItem.getOrder());
		}

		if (!Strings.isNullOrEmpty(menuItem.getParent())) {
			MetaActionMenu parent = actionMenus.findByName(menuItem.getParent());
			if (parent == null) {
				log.debug("Unresolved parent : {}", menuItem.getParent());
				this.setUnresolved(MetaActionMenu.class, menuItem.getParent(), entity);
			} else {
				entity.setParent(parent);
			}
		}

		if (!StringUtils.isBlank(menuItem.getAction())) {
			MetaAction action = actions.findByName(menuItem.getAction());
			if (action == null) {
				log.debug("Unresolved action: {}", menuItem.getAction());
				setUnresolved(MetaActionMenu.class, menuItem.getAction(), entity);
			} else {
				entity.setAction(action);
			}
		}

		entity = actionMenus.save(entity);

		for (MetaActionMenu pending : this.resolve(MetaActionMenu.class, name)) {
			log.debug("Resolved action menu : {}", pending.getName());
			pending.setParent(entity);
			pending = actionMenus.save(pending);
		}
	}

	private static final File outputDir = FileUtils.getFile(System.getProperty("java.io.tmpdir"), "axelor", "generated");

	private void importDefault(Module module) {
		final List<String> names = new ArrayList<>();
		for (String name : ModelLoader.findEntities(module)) {
			final Class<?> klass = JPA.model(name);
			if (klass != null) {
				names.add(klass.getName());
			}
		}
		if (names.isEmpty()) {
			return;
		}

		final TypedQuery<String> query = JPA.em().createQuery("SELECT s.model FROM MetaView s", String.class);
		final List<String> found = query.getResultList();
		for (String name : names) {
			if (found.contains(name)) continue;
			final Class<?> klass = JPA.model(name);
			final File out = FileUtils.getFile(outputDir, "views", klass.getSimpleName() + ".xml");
			final String xml = createDefaults(module, klass);
			try {
				log.debug("Creating default views: {}", out);
				Files.createParentDirs(out);
				Files.asCharSink(out, Charsets.UTF_8).write(xml);
			} catch (IOException e) {
				log.error("Unable to create: {}", out);
			}
		}
	}

	private String createDefaults(Module module, final Class<?> klass) {
		List<AbstractView> views = createDefaults(klass);
		for (AbstractView view : views) {
			importView(view, module, false, 10);
		}
		return XMLViews.toXml(views, false);
	}

	List<AbstractView> createDefaults(final Class<?> klass) {

		final List<AbstractView> all = new ArrayList<>();
		final FormView formView = new FormView();
		final GridView gridView = new GridView();

		final Inflector inflector = Inflector.getInstance();

		String viewName = inflector.underscore(klass.getSimpleName());
		String viewTitle = klass.getSimpleName();

		viewName = inflector.dasherize(viewName);
		viewTitle = inflector.humanize(viewTitle);

		formView.setName(viewName + "-form");
		gridView.setName(viewName + "-grid");

		formView.setModel(klass.getName());
		gridView.setModel(klass.getName());

		formView.setTitle(viewTitle);
		gridView.setTitle(inflector.pluralize(viewTitle));

		List<AbstractWidget> formItems = new ArrayList<>();
		List<AbstractWidget> gridItems = new ArrayList<>();
		List<AbstractWidget> related = new ArrayList<>();

		Mapper mapper = Mapper.of(klass);
		List<String> fields = Lists.reverse(fieldNames(klass));

		for(String n : fields) {
			Property p = mapper.getProperty(n);
			if (p == null || p.isPrimary() || p.isVersion()) {
				continue;
			}
			if (p.isCollection()) {
				if (p.getTargetName() == null) {
					continue;
				}
				PanelRelated panel = new PanelRelated();
				List<AbstractWidget> items = new ArrayList<>();
				Field item = new PanelField();
				item.setName(p.getTargetName());
				items.add(item);
				panel.setName(p.getName());
				panel.setTarget(p.getTarget().getName());
				panel.setItems(items);
				related.add(panel);
			} else {
				Field formItem = new PanelField();
				Field gridItem = new Field();
				formItem.setName(p.getName());
				gridItem.setName(p.getName());
				formItems.add(formItem);
				gridItems.add(gridItem);
			}
		}

		Panel overview = new Panel();
		overview.setTitle("Overview");
		overview.setItems(formItems);

		formItems = new ArrayList<>();
		formItems.add(overview);
		formItems.addAll(related);

		formView.setItems(formItems);
		gridView.setItems(gridItems);
		
		all.add(gridView);
		all.add(formView);

		return all;
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
