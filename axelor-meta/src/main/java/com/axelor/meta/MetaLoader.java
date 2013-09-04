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
package com.axelor.meta;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.reflections.vfs.Vfs.File;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.axelor.auth.AuthUtils;
import com.axelor.auth.db.Group;
import com.axelor.auth.db.User;
import com.axelor.db.JPA;
import com.axelor.db.mapper.Mapper;
import com.axelor.db.mapper.Property;
import com.axelor.meta.db.MetaAction;
import com.axelor.meta.db.MetaActionMenu;
import com.axelor.meta.db.MetaChart;
import com.axelor.meta.db.MetaChartConfig;
import com.axelor.meta.db.MetaChartSeries;
import com.axelor.meta.db.MetaMenu;
import com.axelor.meta.db.MetaModule;
import com.axelor.meta.db.MetaSelect;
import com.axelor.meta.db.MetaSelectItem;
import com.axelor.meta.db.MetaTranslation;
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
import com.axelor.meta.service.MetaModelService;
import com.axelor.meta.service.MetaTranslationsService;
import com.google.common.base.CaseFormat;
import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.common.io.Files;
import com.google.common.io.Resources;
import com.google.inject.Inject;

public class MetaLoader {

	@Inject
	private MetaTranslationsService translationsService;

	private static final String LOCAL_SCHEMA = "object-views_1.0.xsd";
	private static final String REMOTE_SCHEMA = "object-views_"+ ObjectViews.VERSION +".xsd";

	private Logger log = LoggerFactory.getLogger(getClass());

	private Marshaller marshaller;
	private Unmarshaller unmarshaller;

	public MetaLoader() {
		try {
			JAXBContext context = JAXBContext.newInstance(ObjectViews.class);
			unmarshaller = context.createUnmarshaller();
			marshaller = context.createMarshaller();
			marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
			marshaller.setProperty(Marshaller.JAXB_SCHEMA_LOCATION,
					ObjectViews.NAMESPACE + " " + ObjectViews.NAMESPACE + "/" + REMOTE_SCHEMA);

			SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
			Schema schema = schemaFactory.newSchema(Resources.getResource(LOCAL_SCHEMA));

			unmarshaller.setSchema(schema);
			marshaller.setSchema(schema);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private ObjectViews unmarshal(String xml) throws JAXBException {
		StringReader reader = new StringReader(prepareXML(xml));
		return (ObjectViews) unmarshaller.unmarshal(reader);
	}

	private String stripWhiteSpaces(String text) {
		String string = text.replaceAll("\\t", "    ");
		StringBuilder builder = new StringBuilder();
		int leading = 0;
		for(String line : string.split("\\n")) {
			if (line.trim().length() == 0) continue;
			int n = 0;
			while(n++ < line.length()) {
				if (!Character.isWhitespace(line.charAt(n))) break;
			}
			if (leading == 0 || n < leading) {
				leading = n;
			}
			if (n >= leading) {
				builder.append(line.substring(leading)).append("\n");
			}
		}
		return builder.toString();
	}

	private String strip(String xml) {
		String[] lines = xml.split("\n");
		StringBuilder sb = new StringBuilder();
		for(int i = 2 ; i < lines.length - 1 ; i ++) {
			sb.append(lines[i] + "\n");
		}
		sb.deleteCharAt(sb.length()-1);

		Pattern p = Pattern.compile("^(\\t|\\s{4})", Pattern.MULTILINE);
		return p.matcher(sb).replaceAll("");
	}

	@SuppressWarnings("all")
	private String toXml(Object obj, boolean strip) {

		ObjectViews views = new ObjectViews();
		StringWriter writer = new StringWriter();

		if (obj instanceof Action) {
			views.setActions(ImmutableList.of((Action) obj));
		}
		if (obj instanceof AbstractView) {
			views.setViews(ImmutableList.of((AbstractView) obj));
		}
		if (obj instanceof List) {
			views.setViews((List)obj);
		}
		try {
			marshaller.marshal(views, writer);
		} catch (JAXBException e) {
			e.printStackTrace();
		}
		if (strip)
			return this.strip(writer.toString());
		return writer.toString();
	}

	private String prepareXML(String xml) {
		StringBuilder sb = new StringBuilder("<?xml version='1.0' encoding='UTF-8'?>\n");
		sb.append("<object-views")
		  .append(" xmlns='").append(ObjectViews.NAMESPACE).append("'")
		  .append(" xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'")
		  .append(" xsi:schemaLocation='").append(ObjectViews.NAMESPACE).append(" ")
		  .append(ObjectViews.NAMESPACE + "/" + REMOTE_SCHEMA).append("'")
		  .append(">\n")
		  .append(xml)
		  .append("\n</object-views>");
		return sb.toString();
	}

	public ObjectViews fromXML(String xml) throws JAXBException {
		if (Strings.isNullOrEmpty(xml))
			return null;

		if (!xml.trim().startsWith("<?xml"))
			xml = prepareXML(xml);

		StringReader reader = new StringReader(xml);
		return (ObjectViews) unmarshaller.unmarshal(reader);
	}

	private void loadView(AbstractView view, String module, String file) {

		log.info("Loading view : {}", view.getName());

		String name = view.getName();
		String type = view.getClass().getSimpleName().replace("View", "").toLowerCase();
		String model = view.getModel();

		String xml = toXml(view, true);

		if (type.matches("chart|portal|search")) {
			model = null;
		} else if (Strings.isNullOrEmpty(model)) {
			try {
				model = Files.getNameWithoutExtension(file);
				model = JPA.model(model).getName();
			} catch (Exception e) {
				throw new IllegalArgumentException("Invalid view: " + name);
			}
		}

		// import charts
		if (view instanceof ChartView) {
			loadChart((ChartView) view, xml, module);
			return;
		}

		MetaView entity = new MetaView();
		entity.setName(name);
		entity.setTitle(view.getDefaultTitle());
		entity.setType(type);
		entity.setModel(model);
		entity.setModule(module);
		entity.setXml(xml);

		// if a view with same name exists, set higher priority then that
		MetaView existing = model == null ? MetaView.findByName(name) : MetaView.findByName(name, model);
		if (existing != null) {
			entity.setPriority(existing.getPriority() + 1);
		}

		entity = entity.save();
	}

	private void loadChart(ChartView view, String xml, String module) {
		String name = view.getName();
		if (MetaChart.all().filter("self.name = ?1", name).count() > 0) {
			return;
		}

		MetaChart chart = new MetaChart();
		chart.setName(view.getName());
		chart.setModule(module);
		chart.setTitle(view.getDefaultTitle());
		chart.setStacked(view.getStacked());

		String query = stripWhiteSpaces(view.getQuery().getText());
		chart.setQuery(query);
		chart.setQueryType(view.getQuery().getType());

		chart.setCategoryKey(view.getCategory().getKey());
		chart.setCategoryType(view.getCategory().getType());
		chart.setCategoryTitle(view.getCategory().getDefaultTitle());

		for(ChartView.ChartSeries series : view.getSeries()) {
			MetaChartSeries item = new MetaChartSeries();
			item.setKey(series.getKey());
			item.setGroupBy(series.getGroupBy());
			item.setType(series.getType());
			item.setSide(series.getSide());
			item.setAggregate(series.getAggregate());
			chart.addChartSeries(item);
		}

		if (view.getConfig() != null) {
			for(ChartView.ChartConfig config : view.getConfig()) {
				MetaChartConfig item = new MetaChartConfig();
				item.setName(config.getName());
				item.setValue(config.getValue());
				chart.addChartConfig(item);
			}
		}

		chart.save();
	}

	private void loadSelection(Selection selection, String module) {
		log.info("Loading selection : {}", selection.getName());
		MetaSelect select = new MetaSelect();
		select.setName(selection.getName());
		select.setModule(module);
		int sequence = 0;
		for(Selection.Option opt : selection.getOptions()) {
			MetaSelectItem item = new MetaSelectItem();
			item.setValue(opt.getValue());
			item.setTitle(opt.getDefaultTitle());
			item.setOrder(sequence++);
			select.addItem(item);
		}

		MetaSelect existing = MetaSelect.findByName(select.getName());
		if (existing != null) {
			select.setPriority(existing.getPriority() +  1);
		}
		select.save();
	}

	private void loadAction(Action action, String module) {

		log.info("Loading action : {}", action.getName());

		Class<?> klass = action.getClass();
		Mapper mapper = Mapper.of(klass);

		MetaAction entity = new MetaAction();

		entity.setName(action.getName());
		entity.setXml(toXml(action,  true));

		String model = (String) mapper.get(action, "model");
		entity.setModel(model);
		entity.setModule(module);

		String type = CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_HYPHEN, klass.getSimpleName());
		entity.setType(type);

		entity = entity.save();

		for (MetaMenu pending : unresolved_actions.get(entity.getName())) {
			log.info("Resolved menu: {}", pending.getName());
			pending.setAction(entity);
			pending.save();
		}
		unresolved_actions.removeAll(entity.getName());

		for (MetaActionMenu pending : unresolved_actions2.get(entity.getName())) {
			log.info("Resolved action menu: {}", pending.getName());
			pending.setAction(entity);
			pending.save();
		}
		unresolved_actions2.removeAll(entity.getName());
	}

	private Multimap<String, MetaMenu> unresolved_menus = HashMultimap.create();
	private Multimap<String, MetaMenu> unresolved_actions = HashMultimap.create();

	private void loadMenu(MenuItem menuItem, String module) {

		log.info("Loading menu : {}", menuItem.getName());

		MetaMenu menu = new MetaMenu();
		menu.setName(menuItem.getName());
		menu.setPriority(menuItem.getPriority());
		menu.setTitle(menuItem.getDefaultTitle());
		menu.setIcon(menuItem.getIcon());
		menu.setModule(module);
		menu.setGroups(this.findGroups(menuItem.getGroups()));
		menu.setTop(menuItem.getTop());

		if (!Strings.isNullOrEmpty(menuItem.getParent())) {
			MetaMenu parent = MetaMenu.findByName(menuItem.getParent());
			if (parent == null) {
				log.info("Unresolved parent : {}", menuItem.getParent());
				unresolved_menus.put(menuItem.getParent(), menu);
			} else {
				menu.setParent(parent);
			}
		}

		if (!Strings.isNullOrEmpty(menuItem.getAction())) {
			MetaAction a = MetaAction.findByName(menuItem.getAction());
			if (a == null) {
				log.info("Unresolved action: {}", menuItem.getAction());
				unresolved_actions.put(menuItem.getAction(), menu);
			} else {
				menu.setAction(a);
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

	private Multimap<String, MetaActionMenu> unresolved_menus2 = HashMultimap.create();
	private Multimap<String, MetaActionMenu> unresolved_actions2 = HashMultimap.create();

	private void loadActionMenu(MenuItem menuItem, String module) {

		log.info("Loading action menu : {}", menuItem.getName());

		MetaActionMenu menu = new MetaActionMenu();
		menu.setName(menuItem.getName());
		menu.setTitle(menuItem.getDefaultTitle());
		menu.setModule(module);
		menu.setCategory(menuItem.getCategory());

		if (!Strings.isNullOrEmpty(menuItem.getParent())) {
			MetaActionMenu parent = MetaActionMenu.findByName(menuItem.getParent());
			if (parent == null) {
				log.info("Unresolved parent : {}", menuItem.getParent());
				unresolved_menus2.put(menuItem.getParent(), menu);
			} else {
				menu.setParent(parent);
			}
		}

		if (!Strings.isNullOrEmpty(menuItem.getAction())) {
			MetaAction action = MetaAction.findByName(menuItem.getAction());
			if (action == null) {
				log.info("Unresolved action: {}", menuItem.getAction());
				unresolved_actions2.put(menuItem.getAction(), menu);
			} else {
				menu.setAction(action);
			}
		}

		menu = menu.save();

		for (MetaActionMenu pending : unresolved_menus2.get(menu.getName())) {
			log.info("Resolved action menu : {}", pending.getName());
			pending.setParent(menu);
			pending.save();
		}

		unresolved_menus2.removeAll(menu.getName());
	}

	private Set<Group> findGroups(String groups) {

		if (Strings.isNullOrEmpty(groups))
			return null;

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

	private void process(File file, String module) throws JAXBException, IOException {

		ObjectViews views = (ObjectViews) unmarshaller.unmarshal(file.openInputStream());

		if (views.getViews() != null)
			for(AbstractView view : views.getViews())
				loadView(view, module, file.getRelativePath());

		if (views.getActions() != null)
			for(Action action : views.getActions())
				loadAction(action, module);

		if (views.getMenus() != null)
			for (MenuItem menu : views.getMenus())
				loadMenu(menu, module);

		if (views.getActionMenus() != null)
			for (MenuItem menu : views.getActionMenus())
				loadActionMenu(menu, module);

		if (views.getSelections() != null)
			for(Selection selection : views.getSelections())
				loadSelection(selection, module);
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

	@SuppressWarnings("all")
	private String createDefaultViews(final Class<?> klass) {

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
			} else
				gridItems.add(field);
			formItems.add(field);
		}

		formView.setItems(formItems);
		gridView.setItems(gridItems);

		JPA.runInTransaction(new Runnable() {

			@Override
			public void run() {

				String module = null;

				Pattern pattern = Pattern.compile("(.*?)\\.([^.]+)\\.db\\.(.*)");
				Matcher matcher = pattern.matcher(klass.getName());
				if (matcher.matches()) {
					module = "axelor-" + matcher.group(2);
				}

				loadView(formView, module, null);
				loadView(gridView, module, null);
			}
		});

		return toXml(ImmutableList.of(gridView, formView), false);
	}

	private void loadFile(final File file, final String module) {

		JPA.runInTransaction(new Runnable() {

			@Override
			public void run() {
				try {
					process(file, module);
				} catch (JAXBException e) {
					Throwable ex = e.getLinkedException();
					ex = ex == null ? e : ex;
					log.error("Invalid XML input: {} -> {}", module, file.getRelativePath(), ex);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	private void loadDefault(String outputPath) {

		java.io.File output = null;
		if (!Strings.isNullOrEmpty(outputPath))
			output = new java.io.File(outputPath);

		for(Class<?> klass : JPA.models()) {
			String model = klass.getName();
			Long found = MetaView.all().filter("self.model = ?1", model).count();
			if (found == 0) {
				String xml = createDefaultViews(klass);
				if (output != null && output.exists()) {
					java.io.File out = new java.io.File(output + "/views/" + klass.getSimpleName() + ".xml");
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
	}

	/**
	 * Load all JPA models.
	 *
	 */
	public void loadModels() {

		final MetaModelService service = new MetaModelService();

		JPA.runInTransaction(new Runnable() {

			@Override
			public void run() {
				log.info("Load entities...");
				try {
					service.process();
				} catch (Exception e) {
					log.error("Error loading entities.", e);
				}
			}
		});
	}

	/**
	 * Load all Translations.
	 *
	 */
	public void loadTranslations() {
		if (translationsService == null || MetaTranslation.all().count() > 0) {
			return;
		}
		log.info("Load translations...");
		try {
			translationsService.process(moduleResolver);
		} catch (Exception e) {
			log.error("Error loading translations.", e);
		}
	}

	public void loadViews() throws Exception {

		if (MetaView.all().count() > 0 || moduleResolver == null) {
			return;
		}

		List<File> files = MetaScanner.findAll("views\\.(.*?)\\.xml");
		Set<String> imported = Sets.newHashSet();

		for(String module : moduleResolver.all()) {
			String pat = String.format("(/WEB-INF/lib/%s-)|(%s/WEB-INF/classes/)", module, module);
			Pattern pattern = Pattern.compile(pat);
			MetaModule m = MetaModule.findByName(module);
			if (m == null || m.getInstalled() == Boolean.FALSE) {
				continue;
			}
			for(File file : files) {
				String path = file.toString();
				if (imported.contains(path)) {
					continue;
				}
				Matcher matcher = pattern.matcher(path);
				if (matcher.find()) {
					loadFile(file, module);
				}
			}
		}
	}

	private static ModuleResolver moduleResolver = new ModuleResolver();

	private void _loadModuleInfo() throws IOException {

		for(File file : MetaScanner.findAll("module\\.properties")) {

			Properties cfg = new Properties();
			cfg.load(file.openInputStream());

			String name = cfg.getProperty("name");
			String[] deps = cfg.getProperty("depends", "").trim().split("\\s+");

			moduleResolver.add(name, deps);

			MetaModule module = MetaModule.findByName(name);
			if (module == null) {
				module = new MetaModule();
				module.setName(name);
				module.setDepends(Joiner.on(",").join(deps));
			}

			module.setModuleVersion(cfg.getProperty("version"));
			module.setTitle(cfg.getProperty("title"));
			module.setDescription(cfg.getProperty("description"));

			boolean removable = "true".equals(cfg.getProperty("removable"));
			module.setRemovable(removable);

			if (!removable) {
				module.setInstalled(true);
			}

			module = module.save();
		}
	}

	private void loadModuleInfo() throws IOException {
		JPA.runInTransaction(new Runnable() {
			@Override
			public void run() {
				try {
				_loadModuleInfo();
				} catch (Exception e){
					e.printStackTrace();
				}
			}
		});
	}

	public void load(String outputPath) {
		try {
			loadModuleInfo();
		} catch (Exception e) {
			e.printStackTrace();
		}

		loadModels();

		try {
			loadViews();
		} catch (Exception e){}

		loadDefault(outputPath);
		loadTranslations();
	}

	public void loadModule(MetaModule module) {
		// load all the resources of the given module if it's installed
		if (module.getInstalled() == Boolean.FALSE) {
			return;
		}

		// check whether all the dependencies are installed
		for(String name : moduleResolver.resolve(module.getName())) {
			if (name.equals(module.getName()) || "axelor-core".equals(name)) {
				continue;
			}
			MetaModule m = MetaModule.findByName(name);
			if (m == null) {
				throw new IllegalArgumentException("dependency not found: " + name);
			}
			if (m.getInstalled() == Boolean.FALSE) {
				throw new IllegalArgumentException("dependency not installed: " + name);
			}
		}

		String name = module.getName();
		Pattern pattern = Pattern.compile(String.format("(/WEB-INF/lib/%s-)|(%s/WEB-INF/classes/)", name, name));

		for(File file : MetaScanner.findAll("views\\.(.*?)\\.xml")) {
			Matcher matcher = pattern.matcher(file.toString());
			if (matcher.find()) {
				loadFile(file, name);
			}
		}
	}

	public Map<String, Object> findViews(String model, Map<String, String> views) {
		final Map<String, Object> result = Maps.newHashMap();
		if (views == null || views.isEmpty()) {
			views = ImmutableMap.of("grid", "", "form", "");
		}
		for(String type : views.keySet()) {
			final String name = views.get(type);
			final AbstractView view = findView(model, name, type);
			try {
				result.put(type, view);
			} catch (Exception e) {
			}
		}
		return result;
	}

	public AbstractView findView(String model, String name, String type) {
		MetaView view = null;
		User user = AuthUtils.getUser();
		Long group = user != null && user.getGroup() != null ? user.getGroup().getId() : null;

		if (name != null) {
			view = MetaView.findByName(name, model, group);
			if (view == null) {
				view = MetaView.findByName(name, model);
				if (view == null) {
					view = MetaView.findByName(name);
				}
			}
		}

		if (view == null) {
			view = MetaView.findByType(type, model, group);
			if (view == null) {
				view = MetaView.findByType(type, model);
			}
		}

		try {
			return ((ObjectViews) unmarshal(view.getXml())).getViews().get(0);
		} catch (Exception e) {
		}
		return null;
	}

	public AbstractView findView(String name, String module) {
		MetaView view = MetaView.all()
				.filter("self.name = :name AND self.module = :module")
				.bind("name", name)
				.bind("module", module)
				.order("-priority")
				.fetchOne();
		try {
			return ((ObjectViews) unmarshal(view.getXml())).getViews().get(0);
		} catch (Exception e) {
		}
		return null;
	}

	public Action findAction(String name) {
		MetaAction action = MetaAction.findByName(name);
		try {
			return ((ObjectViews) unmarshal(action.getXml())).getActions().get(0);
		} catch (Exception e) {
		}
		return null;
	}
}
