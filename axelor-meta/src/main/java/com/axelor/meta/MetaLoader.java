package com.axelor.meta;

import java.io.IOException;
import java.io.InputStream;
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

import org.reflections.Reflections;
import org.reflections.scanners.ResourcesScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
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
import com.axelor.meta.db.MetaChartSeries;
import com.axelor.meta.db.MetaMenu;
import com.axelor.meta.db.MetaSelect;
import com.axelor.meta.db.MetaSelectItem;
import com.axelor.meta.db.MetaTranslation;
import com.axelor.meta.db.MetaView;
import com.axelor.meta.service.MetaModelService;
import com.axelor.meta.service.MetaTranslationsService;
import com.axelor.meta.views.AbstractView;
import com.axelor.meta.views.AbstractWidget;
import com.axelor.meta.views.Action;
import com.axelor.meta.views.ActionMenuItem;
import com.axelor.meta.views.ChartView;
import com.axelor.meta.views.Field;
import com.axelor.meta.views.FormView;
import com.axelor.meta.views.GridView;
import com.axelor.meta.views.MenuItem;
import com.axelor.meta.views.ObjectViews;
import com.axelor.meta.views.Selection;
import com.google.common.base.CaseFormat;
import com.google.common.base.Charsets;
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
	private static final String REMOTE_SCHEMA = "object-views_0.9.xsd";
	private static final String REMOTE_SCHEMA_LOCATION = "http://apps.axelor.com/xml/ns/object-views";
	
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
					REMOTE_SCHEMA_LOCATION + " " + REMOTE_SCHEMA_LOCATION + "/" + REMOTE_SCHEMA);
	
			SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
			Schema schema = schemaFactory.newSchema(Resources.getResource(LOCAL_SCHEMA));
	
			unmarshaller.setSchema(schema);
			marshaller.setSchema(schema);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
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
	
	private String toXml(Object obj) {
		return toXml(obj, true);
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
		}
		if (strip)
			return this.strip(writer.toString());
		return writer.toString();
	}
	
	private String prepareXML(String xml) {
		StringBuilder sb = new StringBuilder("<?xml version='1.0' encoding='UTF-8'?>\n");
		sb.append("<object-views")
		  .append(" xmlns='").append(REMOTE_SCHEMA_LOCATION).append("'")
		  .append(" xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'")
		  .append(" xsi:schemaLocation='").append(REMOTE_SCHEMA_LOCATION).append(" ")
		  .append(REMOTE_SCHEMA_LOCATION + "/" + REMOTE_SCHEMA).append("'")
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
	
	private String findName(String filePath) {
		Pattern pattern = Pattern.compile("(.*)\\/views\\/(.*)\\.xml");
		Matcher matcher = pattern.matcher(filePath);
		if (matcher.matches())
			return String.format("com.axelor.%s.db.%s", matcher.group(1), matcher.group(2));
		return null;
	}
	
	private void loadView(AbstractView view, String filePath) {
		
		log.info("Loading view : {}", view.getName());
		
		String xml = toXml(view);
		String type = view.getClass().getSimpleName().replace("View", "").toLowerCase();
		String model = view.getModel();
		
		if (Strings.isNullOrEmpty(model))
			model = findName(filePath);
		
		if (type.matches("chart|portal|search"))
			model = null;

		// import charts
		if (view instanceof ChartView) {
			loadChart((ChartView) view, xml);
			return;
		}

		String name = view.getName();
		String name_ = name + "._";

		MetaView entity = MetaView.all().filter("self.name = ?1", name_).fetchOne();
		if (entity == null) {
			entity = new MetaView();
			if (MetaView.all().filter("self.name = ?1", name).count() > 0) {
				name = name_;
			}
			entity.setName(name);
		}
		
		entity.setTitle(view.getTitle());
		entity.setType(type);
		entity.setModel(model);
		entity.setXml(xml);

		entity = entity.save();
	}
	
	private void loadChart(ChartView view, String xml) {
		String name = view.getName();
		if (MetaChart.all().filter("self.name = ?1", name).count() > 0) {
			return;
		}

		MetaChart chart = new MetaChart();
		
		chart.setName(view.getName());
		chart.setTitle(view.getTitle());
		chart.setStacked(view.getStacked());
		
		String query = stripWhiteSpaces(view.getQuery().getText());
		chart.setQuery(query);
		chart.setQueryType(view.getQuery().getType());

		chart.setCategoryKey(view.getCategory().getKey());
		chart.setCategoryType(view.getCategory().getType());
		chart.setCategoryTitle(view.getCategory().getTitle());

		for(ChartView.ChartSeries series : view.getSeries()) {
			MetaChartSeries item = new MetaChartSeries();
			item.setKey(series.getKey());
			item.setGroupBy(series.getGroupBy());
			item.setType(series.getType());
			item.setSide(series.getSide());
			item.setAggregate(series.getAggregate());
			chart.addChartSeries(item);
		}

		chart.save();
	}

	private void loadSelection(Selection selection) {
		log.info("Loading selection : {}", selection.getName());
		MetaSelect select = new MetaSelect();
		select.setName(selection.getName());
		for(Selection.Option opt : selection.getOptions()) {
			MetaSelectItem item = new MetaSelectItem();
			item.setValue(opt.getValue());
			item.setTitle(opt.getTitle());
			select.addItem(item);
		}
		select.save();
	}

	private void loadAction(Action action) {
		
		log.info("Loading action : {}", action.getName());
		
		Class<?> klass = action.getClass();
		Mapper mapper = Mapper.of(klass);
		
		MetaAction entity = new MetaAction();

		entity.setName(action.getName());
		entity.setXml(toXml(action));
		
		String model = (String) mapper.get(action, "model");
		entity.setModel(model);

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
	
	private void loadMenu(MenuItem menu) {
		
		if (menu instanceof ActionMenuItem) {
			loadMenu2((ActionMenuItem) menu);
			return;
		}
		
		log.info("Loading menu : {}", menu.getName());
		
		MetaMenu m = new MetaMenu();
		m.setName(menu.getName());
		m.setPriority(menu.getPriority());
		m.setTitle(menu.getTitle());
		m.setIcon(menu.getIcon());
		m.setGroups(this.findGroups(menu.getGroups()));
		
		if (!Strings.isNullOrEmpty(menu.getParent())) {
			MetaMenu p = MetaMenu.all().filter("self.name = ?1", menu.getParent()).fetchOne();
			if (p == null) {
				log.info("Unresolved parent : {}", menu.getParent());
				unresolved_menus.put(menu.getParent(), m);
			} else {
				m.setParent(p);
			}
		}
		
		if (!Strings.isNullOrEmpty(menu.getAction())) {
			MetaAction a = MetaAction.all().filter("self.name = ?1", menu.getAction()).fetchOne();
			if (a == null) {
				log.info("Unresolved action: {}", menu.getAction());
				unresolved_actions.put(menu.getAction(), m);
			} else {
				m.setAction(a);
			}
		}
		
		m = m.save();
		
		for (MetaMenu pending : unresolved_menus.get(m.getName())) {
			log.info("Resolved menu : {}", pending.getName());
			pending.setParent(m);
			pending.save();
		}
		
		unresolved_menus.removeAll(m.getName());
	}
	
	private Multimap<String, MetaActionMenu> unresolved_menus2 = HashMultimap.create();
	private Multimap<String, MetaActionMenu> unresolved_actions2 = HashMultimap.create();
	
	private void loadMenu2(ActionMenuItem menu) {
		
		log.info("Loading action menu : {}", menu.getName());
		
		MetaActionMenu m = new MetaActionMenu();
		m.setName(menu.getName());
		m.setTitle(menu.getTitle());
		m.setCategory(menu.getCategory());

		if (!Strings.isNullOrEmpty(menu.getParent())) {
			MetaActionMenu p = MetaActionMenu.all().filter("self.name = ?1", menu.getParent()).fetchOne();
			if (p == null) {
				log.info("Unresolved parent : {}", menu.getParent());
				unresolved_menus2.put(menu.getParent(), m);
			} else {
				m.setParent(p);
			}
		}
		
		if (!Strings.isNullOrEmpty(menu.getAction())) {
			MetaAction a = MetaAction.all().filter("self.name = ?1", menu.getAction()).fetchOne();
			if (a == null) {
				log.info("Unresolved action: {}", menu.getAction());
				unresolved_actions2.put(menu.getAction(), m);
			} else {
				m.setAction(a);
			}
		}
		
		m = m.save();
		
		for (MetaActionMenu pending : unresolved_menus2.get(m.getName())) {
			log.info("Resolved action menu : {}", pending.getName());
			pending.setParent(m);
			pending.save();
		}
		
		unresolved_menus2.removeAll(m.getName());
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
	
	private void process(InputStream stream, String filePath) throws JAXBException {
		
		ObjectViews views = (ObjectViews) unmarshaller.unmarshal(stream);
		
		if (views.getViews() != null)
			for(AbstractView view : views.getViews())
				loadView(view, filePath);

		if (views.getActions() != null)
			for(Action action : views.getActions())
				loadAction(action);
		
		if (views.getMenuItems() != null)
			for (MenuItem menu : views.getMenuItems())
				loadMenu(menu);
		
		if (views.getSelections() != null)
			for(Selection selection : views.getSelections())
				loadSelection(selection);
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
	private String createDefaultViews(Class<?> klass) {
		
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
				field.setNoLabel(true);
			} else
				gridItems.add(field);
			formItems.add(field);
		}
		
		formView.setItems(formItems);
		gridView.setItems(gridItems);
		
		JPA.runInTransaction(new Runnable() {
			
			@Override
			public void run() {
				loadView(formView, null);
				loadView(gridView, null);
			}
		});
		
		return toXml(ImmutableList.of(gridView, formView), false);
	}
	
	private Pattern pattern = Pattern.compile("(\\w+)-(\\w+)");
	
	private String findModuleName(File file) {
		String[] parts = file.getFullPath().split("/");
		for(int i = parts.length - 1 ; i >= 0 ; i--) {
			Matcher matcher = pattern.matcher(parts[i]);
			if (matcher.find())
				return matcher.group(2);
		}
		throw new RuntimeException("Unable to find module name: " + file.getFullPath());
	}
	
	private void loadFile(final File file) {
		
		String path = file.getRelativePath();
		String module = findModuleName(file);
		
		final String filePath = module + "/" + path;
		
		JPA.runInTransaction(new Runnable() {

			@Override
			public void run() {
				try {
					process(file.openInputStream(), filePath);
				} catch (JAXBException e) {
					Throwable ex = e.getLinkedException();
					ex = ex == null ? e : ex;
					log.error("Invalid XML input: {}", filePath, ex);
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
			translationsService.process();
		} catch (Exception e) {
			log.error("Error loading translations.", e);
		}
	}
	
	public void loadViews() throws Exception {
		
		if (MetaView.all().count() > 0) {
			return;
		}

		ModuleResolver resolver = new ModuleResolver();

		for(File file : findResources("module", "module.properties")) {
			Properties cfg = new Properties();
			cfg.load(file.openInputStream());
			String[] deps = cfg.getProperty("depends", "").trim().split("\\s+");
			resolver.add(cfg.getProperty("name"), deps);
		}
		
		List<File> files = findResources("views", ".xml");
		Set<String> imported = Sets.newHashSet();

		for(String module : resolver.all()) {
			String pat = String.format("(/WEB-INF/lib/%s)|(%s/WEB-INF/classes/)", module, module);
			Pattern pattern = Pattern.compile(pat);
			for(File file : files) {
				String path = file.getFullPath();
				if (imported.contains(path)) {
					continue;
				}
				Matcher matcher = pattern.matcher(path);
				if (matcher.find()) {
					loadFile(file);
				}
			}
		}
	}

	public void load(String outputPath) {
		loadModels();
		loadTranslations();
		try {
			loadViews();
		} catch (Exception e){}
		loadDefault(outputPath);
	}
	
	private List<File> findResources(final String prefix, final String suffix) {

		final List<File> files = Lists.newArrayList();

		final ResourcesScanner scanner = new ResourcesScanner() {
			
			@Override
			public boolean acceptsInput(String file) {
				return file.startsWith(prefix) && file.endsWith(suffix);
			}

			@Override
			public void scan(final File file) {
				files.add(file);
			}
		};

		new Reflections(
				new ConfigurationBuilder()
					.setUrls(ClasspathHelper.forPackage("com.axelor"))
					.setScanners(scanner)
				);
		
		return files;
	}

	private ObjectViews unmarshal(String xml) throws JAXBException {
		StringReader reader = new StringReader(prepareXML(xml));
		return (ObjectViews) unmarshaller.unmarshal(reader);
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
		if (name != null) {
			return findView(name);
		}
		MetaView view = null;
		User user = AuthUtils.getUser();
		if (user != null && user.getGroup() != null) {
			view = MetaView.all()
					.filter("self.model = ?1 AND self.type = ?2 AND ?3 MEMBER OF self.groups", model, type, user.getGroup())
					.order("-name").fetchOne();
		}
		if (view == null) {
			view = MetaView.all()
					.filter("self.model = ?1 AND self.type = ?2", model, type)
					.order("-name").fetchOne();
		}
		try {
			return ((ObjectViews) unmarshal(view.getXml())).getViews().get(0);
		} catch (Exception e) {
		}
		return null;
	}
	
	public AbstractView findView(String name) {
		String name_ = name + "._";
		MetaView view = MetaView.all().filter("self.name = ?1 OR self.name = ?2", name_, name).order("-name").fetchOne();
		try {
			return ((ObjectViews) unmarshal(view.getXml())).getViews().get(0);
		} catch (Exception e) {
		}
		return null;
	}
	
	public Action findAction(String name) {
		MetaAction action = MetaAction.all().filter("self.name = ?1", name).fetchOne();
		try {
			return ((ObjectViews) unmarshal(action.getXml())).getActions().get(0);
		} catch (Exception e) {
		}
		return null;
	}
}
