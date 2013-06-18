package com.axelor.meta.service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.axelor.meta.MetaLoader;
import com.axelor.meta.MetaScanner;
import com.axelor.meta.db.MetaAction;
import com.axelor.meta.db.MetaActionMenu;
import com.axelor.meta.db.MetaChart;
import com.axelor.meta.db.MetaChartSeries;
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
import com.axelor.meta.schema.ObjectViews;
import com.axelor.meta.schema.actions.Action;
import com.axelor.meta.schema.actions.ActionCondition;
import com.axelor.meta.schema.actions.ActionCondition.Check;
import com.axelor.meta.schema.actions.ActionValidate;
import com.axelor.meta.schema.actions.ActionValidate.Validator;
import com.axelor.meta.schema.actions.ActionView;
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
import com.axelor.meta.schema.views.Notebook;
import com.axelor.meta.schema.views.Page;
import com.axelor.meta.schema.views.Portal;
import com.axelor.meta.schema.views.Portlet;
import com.axelor.meta.schema.views.Separator;
import com.axelor.meta.schema.views.SimpleContainer;
import com.axelor.meta.schema.views.SimpleWidget;
import com.google.common.base.Charsets;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.io.Files;
import com.google.common.io.Resources;

public class MetaExportTranslation {

	private static final String LOCAL_SCHEMA_DOMAIN = "domain-models_1.0.xsd";
	private static Logger log = LoggerFactory.getLogger(MetaExportTranslation.class);
	private MetaLoader metaLoader = new MetaLoader();

	private final String groupType = "group";
	private final String labelType = "label";
	private final String separatorType = "separator";
	private final String pageType = "page";
	private final String buttonType = "button";
	private final String portletType = "portlet";
	private final String selectType = "select";
	private final String fieldType = "field";
	private final String viewFieldType = "viewField";
	private final String helpType = "help";
	private final String otherType = "other";
	private final String chartType = "chart";
	private final String menuType = "menu";
	private final String actionMenuType = "actionMenu";
	private final String actionType = "action";

	private File exportFile ;
	private String exportLanguage ;
	private String exportPath ;
	private String currentModule ;

	public void exportTranslations(String exportPath, String exportLanguage) {

		exportPath = exportPath.endsWith("/") ? exportPath : exportPath.concat("/");
		this.exportLanguage = exportLanguage;
		
		List<MetaModule> modules = MetaModule.all().filter("self.installed = true").fetch();
		
		//axelor-core
		MetaModule coreModule = new MetaModule();
		coreModule.setName("axelor-core");
		modules.add(coreModule);
		
		for(MetaModule module : modules) {
			this.exportPath = exportPath + module.getName() + "/";
			this.exportFile = new File(this.exportPath + this.exportLanguage + ".csv");
			this.currentModule = module.getName();
			
			log.info("Export {} module to {}.", module.getName(), this.exportFile.getPath());

			this.exportMenus();
			this.exportMenuActions();
			this.exportObjects();
			this.exportSelects();
			this.exportCharts();
			this.exportViews();
			this.exportActions();
			this.exportOther();
		}
	}

	private void exportOther() {
		for (MetaTranslation translation : MetaTranslation.all().filter("self.type = ?1 AND self.language = ?2 AND self.domain = ?3",  this.otherType, this.exportLanguage, this.currentModule).order("key").fetch()) {
			this.appendToFile(translation.getDomain(), translation.getKey(), this.otherType, translation.getKey(), translation.getTranslation());
		}
	}

	private void exportActions() {
		for (MetaAction metaAction : MetaAction.findByModule(this.currentModule).order("name").order("type").fetch()) {
			try {
				ObjectViews views = (ObjectViews) metaLoader.fromXML(metaAction.getXml());
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
			String transalation = this.getTranslation(actionView.getDefaultTitle(), "", null, null);
			this.appendToFile(actionView.getName(), actionView.getName(), this.actionType, actionView.getDefaultTitle(), transalation);
		}
		else if(action instanceof ActionValidate) {
			ActionValidate actionValidate = (ActionValidate) action;
			for(Validator validator : actionValidate.getValidators()) {
				String transalation = this.getTranslation(validator.getMessage(), "", null, null);
				this.appendToFile(actionValidate.getName(), actionValidate.getName(), this.actionType, validator.getMessage(), transalation);
			}
		}
		else if(action instanceof ActionCondition) {
			ActionCondition actionCondition = (ActionCondition) action;
			for(Check check : actionCondition.getConditions()) {
				if(check.getDefaultError() != null) {
					String transalation = this.getTranslation(check.getDefaultError(), "", null, null);
					this.appendToFile(actionCondition.getName(), actionCondition.getName(), this.actionType, check.getDefaultError(), transalation);
				}
			}
		}
	}

	private void exportMenuActions() {
		for (MetaActionMenu actionMenu : MetaActionMenu.findByModule(this.currentModule).order("name").fetch()) {
			String transalation = this.getTranslation(actionMenu.getTitle(), "", null, null);
			this.appendToFile(actionMenu.getName(), actionMenu.getName(), this.actionMenuType, actionMenu.getTitle(), transalation);
		}
	}

	private void exportViews() {
		for (MetaView view : MetaView.findByModule(this.currentModule).order("name").order("type").fetch()) {
			try {
				ObjectViews views = (ObjectViews) metaLoader.fromXML(view.getXml());
				if (view != null && views.getViews() != null)
					for(AbstractView abstractView : views.getViews())
						this.loadView(abstractView);
			}
			catch(Exception ex) {
				log.error("Error while exporting view : {}", view.getName());
				log.error("With following exception : {}", ex);
			}
		}
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
	}

	private void loadButton(AbstractView abstractView, Button button) {
		if(!Strings.isNullOrEmpty(button.getDefaultTitle())) {
			String transalation = this.getTranslation(button.getDefaultTitle(), "", null, null);
			this.appendToFile(abstractView.getName(), button.getName(), this.buttonType, button.getDefaultTitle(), transalation);
		}
		
		if(!Strings.isNullOrEmpty(button.getDefaultPrompt())) {
			String transalation = this.getTranslation(button.getDefaultPrompt(), "", null, null);
			this.appendToFile(abstractView.getName(), button.getName(), this.buttonType, button.getDefaultPrompt(), transalation);
		}
		
		if(!Strings.isNullOrEmpty(button.getDefaultHelp())) {
			String transalation = this.getTranslation(button.getDefaultHelp(), "", null, null);
			this.appendToFile(abstractView.getName(), button.getName(), this.buttonType, button.getDefaultHelp(), transalation);
		}
	}

	private void loadSimpleWidget(AbstractView abstractView, SimpleWidget widget, String type) {
		
		if(!Strings.isNullOrEmpty(widget.getDefaultTitle())) {
			String transalation = this.getTranslation(widget.getDefaultTitle(), "", null, null);
			this.appendToFile(abstractView.getName(), widget.getName(), type, widget.getDefaultTitle(), transalation);
		}
		
		if(!Strings.isNullOrEmpty(widget.getDefaultHelp())) {
			String transalation = this.getTranslation(widget.getDefaultHelp(), "", null, null);
			this.appendToFile(abstractView.getName(), widget.getName(), type, widget.getDefaultHelp(), transalation);
		}
		
	}

	private void loadAbstractView(AbstractView abstractView) {
		if(abstractView.getToolbar() != null) {
			for (Button button : abstractView.getToolbar()) {
				this.loadButton(abstractView,button);
			}
		}
		
		String transalation = this.getTranslation(abstractView.getDefaultTitle(), "", null, null);
		this.appendToFile(abstractView.getModel(), abstractView.getName(), abstractView.getType(), abstractView.getDefaultTitle(), transalation);
	}

	private void loadWidget(AbstractView abstractView, AbstractWidget widget) {
		if(widget instanceof AbstractContainer) {
			this.loadContainer(abstractView, (AbstractContainer) widget);
		}
		else if(widget instanceof Button) {
			this.loadButton(abstractView, (Button) widget);
		}
		else if(widget instanceof Separator) {
			this.loadSimpleWidget(abstractView, (SimpleWidget) widget, this.separatorType);
		}
		else if(widget instanceof Label) {
			this.loadSimpleWidget(abstractView, (SimpleWidget) widget, this.labelType);
		}
		else if(widget instanceof Field) {
			Field field = (Field) widget;
			this.exportField(abstractView,field);
			if(field.getViews() != null) {
				for (AbstractView subAbstractView : field.getViews()) {
					this.loadView(subAbstractView);
				}
			}
		}
	}

	private void exportField(AbstractView abstractView, Field field) {
		if(!Strings.isNullOrEmpty(field.getDefaultTitle()) || !Strings.isNullOrEmpty(field.getDefaultHelp())) {
			String transalation = this.getTranslation(field.getDefaultTitle(), "", abstractView.getModel(), this.fieldType);
			String transalationHelp = this.getTranslation(field.getDefaultHelp(), "", abstractView.getModel(), this.helpType);
			this.appendToFile(abstractView.getModel(), field.getDefaultTitle(), this.viewFieldType, field.getDefaultTitle(), transalation, field.getDefaultHelp(), transalationHelp);
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
			this.loadSimpleWidget(view, (Portlet) container, this.portletType);
		}
		else if(container instanceof SimpleContainer) {
			SimpleContainer simpleContainer = (SimpleContainer) container;
			
			if(simpleContainer.getItems() != null) {
				if(simpleContainer instanceof Group)
					this.loadSimpleWidget(view, (SimpleWidget) container, this.groupType);
				else if(simpleContainer instanceof Page)
					this.loadSimpleWidget(view, (SimpleWidget) container, this.pageType);
				
				for (AbstractWidget widget : simpleContainer.getItems()) {
					this.loadWidget(view, widget) ;
				}
			}
		}
	}

	private void exportCharts() {
		for (MetaChart chart : MetaChart.findByModule(this.currentModule).order("name").fetch()) {
			String transalation = this.getTranslation(chart.getTitle(), "", null, null);
			this.appendToFile(chart.getName(), chart.getName(), this.chartType, chart.getTitle(), transalation);
			
			if(!Strings.isNullOrEmpty(chart.getCategoryTitle())) {
				String transalationTitle = this.getTranslation(chart.getCategoryTitle(), "", null, null);
				this.appendToFile(chart.getName(), chart.getName(), this.chartType, chart.getCategoryTitle(), transalationTitle);
			}
			
			if(chart.getChartSeries() != null) {
				for (MetaChartSeries serie : chart.getChartSeries()) {
					if(!Strings.isNullOrEmpty(serie.getTitle())) {
						String transalationSerie = this.getTranslation(serie.getTitle(), "", null, null);
						this.appendToFile(serie.getTitle(), serie.getTitle(), this.chartType, serie.getTitle(), transalationSerie);
					}
				}
			}
		}
	}

	private void exportObjects() {
		List<org.reflections.vfs.Vfs.File> files = MetaScanner.findAll("domains\\.(.*?)\\.xml");
		
		Collections.sort(files, new Comparator<org.reflections.vfs.Vfs.File>() {
			@Override
			public int compare(org.reflections.vfs.Vfs.File o1, org.reflections.vfs.Vfs.File o2) {
				String a = o1.getName();
				String b = o2.getName();
				return a.compareTo(b);
			}
		});

		String pat = String.format("(/WEB-INF/lib/%s-)|(%s/WEB-INF/classes/)", this.currentModule, this.currentModule);
		Pattern pattern = Pattern.compile(pat);
		for(org.reflections.vfs.Vfs.File file : files) {
			String path = file.toString();
			Matcher matcher = pattern.matcher(path);
			if (matcher.find()) {
				this.exportFile(file);
			}
		}
	}

	private void exportFile(org.reflections.vfs.Vfs.File file) {
		try {
			DomainModels object = this.unmarshalObject(file.openInputStream());
			for (Entity entity : object.getEntities()) {
				this.exportEntity(entity, object.getModule());
			}
		}
		catch(Exception ex) {
			log.error("Error while exporting file : {}", file.getName());
			log.error("With following exception : {}", ex);
		}
	}

	private void exportEntity(Entity entity, Module module) throws IOException {
		String packageName = module.getPackageName()+"."+entity.getName();
		
		for (Property field : entity.getFields()) {
			String transalation = this.getTranslation(field.getName(), "", packageName, this.fieldType);
			String transalationHelp = this.getTranslation(field.getName(), "", packageName, this.helpType);
			this.appendToFile(packageName, field.getName(), this.fieldType, field.getTitle(), transalation, field.getHelp(), transalationHelp);
		}
	}

	private void exportMenus() {
		for (MetaMenu menu : MetaMenu.findByModule(this.currentModule).order("name").fetch()) {
			String transalation = this.getTranslation(menu.getTitle(), "", null, null);
			this.appendToFile(menu.getName(), menu.getTitle(), this.menuType, menu.getTitle(), transalation);
		}
	}

	private void exportSelects() {
		for (MetaSelect select : MetaSelect.findByModule(this.currentModule).order("name").fetch()) {
			for (MetaSelectItem item : select.getItems()) {
				String transalation = this.getTranslation(item.getTitle(), "", null, null);
				this.appendToFile(select.getName(), item.getValue(), this.selectType, item.getTitle(), transalation);
			}
		}
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
		
		if(domain != null) {
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
		if(more == null || more.length != 2){
			sb.append("\"").append("\"").append(",").append("\"").append("\"");
		}
		else {
			sb.append("\"").append(more[0] == null ? "" : more[0]).append("\"").append(",");
			sb.append("\"").append(more[1] == null ? "" : more[1]).append("\"");
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
}
