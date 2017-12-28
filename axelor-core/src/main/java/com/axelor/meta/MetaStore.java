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
package com.axelor.meta;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.axelor.auth.AuthUtils;
import com.axelor.auth.db.Role;
import com.axelor.auth.db.User;
import com.axelor.common.Inflector;
import com.axelor.common.StringUtils;
import com.axelor.db.JpaSecurity;
import com.axelor.db.JpaSecurity.AccessType;
import com.axelor.db.Model;
import com.axelor.db.Query;
import com.axelor.db.ValueEnum;
import com.axelor.db.annotations.Widget;
import com.axelor.db.mapper.Mapper;
import com.axelor.db.mapper.Property;
import com.axelor.i18n.I18n;
import com.axelor.inject.Beans;
import com.axelor.meta.db.MetaJsonField;
import com.axelor.meta.db.MetaJsonModel;
import com.axelor.meta.db.MetaJsonRecord;
import com.axelor.meta.db.MetaSelectItem;
import com.axelor.meta.db.repo.MetaJsonModelRepository;
import com.axelor.meta.loader.ModuleManager;
import com.axelor.meta.loader.XMLViews;
import com.axelor.meta.schema.ObjectViews;
import com.axelor.meta.schema.actions.Action;
import com.axelor.meta.schema.views.Selection;
import com.axelor.script.CompositeScriptHelper;
import com.axelor.script.ScriptHelper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Objects;
import com.google.common.base.Splitter;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

public final class MetaStore {

	private static final Logger log = LoggerFactory.getLogger(MetaStore.class);

	private static final Cache<String, Action> ACTIONS = CacheBuilder.newBuilder()
			.maximumSize(1000)
			.weakValues()
			.build();

	private MetaStore() {
	}

	/**
	 * Used for unit testing.
	 * 
	 */
	static void resister(ObjectViews views) {
		try {
			for (Action item : views.getActions()) {
				ACTIONS.put(item.getName(), item);
			}
		} catch (NullPointerException e) {
		}
	}

	public static Action getAction(String name) {
		Action action = ACTIONS.getIfPresent(name);
		if (action == null) {
			action = XMLViews.findAction(name);
			if (action != null) {
				ACTIONS.put(name, action);
			}
		}
		if (action == null) {
			return null;
		}
		final String module = action.getModuleToCheck();
		if (StringUtils.isBlank(module) || ModuleManager.isInstalled(module)) {
			return action;
		}
		return null;
	}

	public static Map<String, Object> getPermissions(Class<?> model) {
		final User user = AuthUtils.getUser();
		if (user == null || AuthUtils.isAdmin(user) || !Model.class.isAssignableFrom(model)) {
			return null;
		}

		@SuppressWarnings("unchecked")
		final Class<? extends Model> klass = (Class<? extends Model>) model;
		final Map<String, Object> map = new HashMap<>();
		final JpaSecurity security = Beans.get(JpaSecurity.class);

		map.put("read", security.isPermitted(AccessType.READ, klass));
		map.put("write", security.isPermitted(AccessType.WRITE, klass));
		map.put("create", security.isPermitted(AccessType.CREATE, klass));
		map.put("remove", security.isPermitted(AccessType.REMOVE, klass));
		map.put("export", security.isPermitted(AccessType.EXPORT, klass));

		return map;
	}

	private static Property findField(final Mapper mapper, String name) {
		final Iterator<String> iter = Splitter.on(".").split(name).iterator();
		Mapper current = mapper;
		Property property = current.getProperty(iter.next());

		if (property == null || (property.isJson() && iter.hasNext())) {
			return null;
		}

		while(property != null && property.getTarget() != null && iter.hasNext()) {
			current = Mapper.of(property.getTarget());
			property = current.getProperty(iter.next());
		}

		return property;
	}
	
	public static Map<String, Object> findFields(final Class<?> modelClass, final Collection<String> names) {
		final Map<String, Object> data = new HashMap<>();
		final Mapper mapper = Mapper.of(modelClass);
		final Map<String, Property> fieldsMap = new LinkedHashMap<>();
		final List<Object> fields = new ArrayList<>();

		boolean massUpdate = false;
		Object bean = null;
		try {
			bean = modelClass.newInstance();
		} catch (Exception e) {}

		for(final String name : names) {
			final Property property = findField(mapper, name);
			if (property == null) continue;
			final Map<String, Object> map = property.toMap();
			map.put("name", name);
			if (property.getSelection() != null && !"".equals(property.getSelection().trim())) {
				map.put("selection", property.getSelection());
				map.put("selectionList", getSelectionList(property.getSelection()));
			}
			if (property.isEnum()) {
				map.put("selectionList", getSelectionList(property.getEnumType()));
			}
			if (property.getTarget() != null) {
				map.put("perms", getPermissions(property.getTarget()));
			}
			if (property.isMassUpdate()) {
				massUpdate = true;
			}
			// find the default value
			if (!property.isTransient() && !property.isVirtual()) {
				Object obj = null;
				if (name.contains(".")) {
					try {
						obj = property.getEntity().newInstance();
					} catch (Exception e) {}
				} else {
					obj = bean;
				}
				if (obj != null) {
					Object defaultValue = property.get(obj);
					if (defaultValue != null) {
						map.put("defaultValue", defaultValue);
					}
				}
			}
			if (name.contains(".")) {
				map.put("readonly", true);
			}
			fieldsMap.put(name, property);
			fields.add(map);
		}

		Map<String, Object> perms = getPermissions(modelClass);
		if (massUpdate) {
			if (perms == null) {
				perms = new HashMap<>();
			}
			perms.put("massUpdate", massUpdate);
		}

		// find dotted json fields
		final Map<String, Map<String, Object>> jsonFields = new HashMap<>();
		for (String name : names) {
			if (fieldsMap.containsKey(name) || name.indexOf('.') == -1) { continue; }
			final String first = name.substring(0, name.indexOf('.'));
			final String field = name.substring(name.indexOf('.') + 1);
			final Property property = findField(mapper, first);
			if (property == null || !property.isJson()) { continue; }
			if (!jsonFields.containsKey(first)) {
				jsonFields.put(first, findJsonFields(modelClass.getName(), first));
			}
			final Map<String, Object> jsonField = jsonFields.get(first);
			if (jsonField != null && jsonField.containsKey(field)) {
				@SuppressWarnings("all")
				final Map<String, Object> attrs = new HashMap<>((Map) jsonField.get(field));
				if (attrs != null) {
					attrs.put("name", name);
					fields.add(attrs);
				}
			}
		}

		data.put("perms", perms);
		data.put("fields", fields);

		return data;
	}

	public static Map<String, Object> findJsonFields(String modelName, String fieldName) {
		try {
			if (!Mapper.of(Class.forName(modelName)).getProperty(fieldName).isJson()) {
				return null;
			}
		} catch (Exception e) {
			return null;
		}
		final List<MetaJsonField> fields = Query.of(MetaJsonField.class)
				.filter("self.model = :model AND self.modelField = :field")
				.bind("model", modelName)
				.bind("field", fieldName)
				.order("id").fetch(); 
		return updateJsonFields(fields, fieldName);
	}

	public static Map<String, Object> findJsonFields(String jsonModel) {
		final MetaJsonModelRepository forms = Beans.get(MetaJsonModelRepository.class);
		final MetaJsonModel found = forms.findByName(jsonModel);
		return found == null ? null : updateJsonFields(found.getFields(), "attrs");
	}

	private static Map<String, Object> updateJsonFields(List<MetaJsonField> records, String fieldName) {
		final java.lang.reflect.Field[] declaredFields = MetaJsonField.class.getDeclaredFields();
		final Mapper mapper = Mapper.of(MetaJsonField.class);
		final Map<String, Object> fields = new LinkedHashMap<>();
		final List<MetaJsonField> jsonFields = new ArrayList<>(records);
		final User user = AuthUtils.getUser();

		ScriptHelper scriptHelper = null;

		jsonFields.sort((a, b) -> {
			int x = a.getSequence() == null ? 0 : a.getSequence();
			int y = b.getSequence() == null ? 0 : b.getSequence();
			return Integer.compare(x, y);
		});

		for (MetaJsonField record : jsonFields) {
			final Map<String, Object> attrs = new HashMap<>();
			final String name = record.getName();
			
			// check permissions
			if (record.getRoles() != null && !record.getRoles().isEmpty()) {
				final Set<Role> roles = new HashSet<>();
				if (user.getRoles() != null) {
					roles.addAll(user.getRoles());
				}
				if (user.getGroup() != null && user.getGroup().getRoles() != null) {
					roles.addAll(user.getGroup().getRoles());
				}
				if (Collections.disjoint(roles, record.getRoles())) {
					continue;
				}
			}

			// check server condition
			if (StringUtils.notBlank(record.getIncludeIf())) {
				if (scriptHelper == null) {
					scriptHelper = new CompositeScriptHelper(null);
				}
				if (scriptHelper == null || !scriptHelper.test(record.getIncludeIf())) {
					continue;
				}
			}

			for (java.lang.reflect.Field field : declaredFields) {
				final Property prop = mapper.getProperty(field.getName());
				if (prop == null || prop.isPrimary() || prop.isReference() || prop.isCollection()) {
					continue;
				}
				final Object value = prop.get(record);
				if (value == null || value == Boolean.FALSE) continue;
				attrs.put(prop.getName(), value);
			}

			String title = record.getTitle();

			// localized title
			attrs.put("title", I18n.get(title));

			// auto title
			if (StringUtils.isBlank(title)) {
				String last = name.substring(name.lastIndexOf('.') + 1);
				title = I18n.get(Inflector.getInstance().humanize(last));
				attrs.put("autoTitle", title);
			}

			String type = record.getType() == null ? "" : record.getType();
			int min = record.getMinSize() == null ? 0 : record.getMinSize();
			int max = record.getMaxSize() == null ? 0 : record.getMaxSize();
			if (max <= min) {
				attrs.remove("maxSize");
			}
			if ((min == 0 && max == 0) || type.matches("date|time|datetime|boolean")) {
				attrs.remove("maxSize");
				attrs.remove("minSize");
			}

			if ("ref-select".equalsIgnoreCase(record.getType()) ||
				"ref-select".equalsIgnoreCase(record.getWidget()) ||
				"RefSelect".equalsIgnoreCase(record.getWidget())) {
				attrs.put("widget", "json-ref-select");
			}
			
			if (!StringUtils.isBlank(record.getTargetModel())) {
				attrs.put("target", record.getTargetModel());
				attrs.remove("targetModel");
				try {
					attrs.put("targetName", Mapper.of(Class.forName(record.getTargetModel())).getNameField().getName());
				} catch (ClassNotFoundException e) {
				}
			}

			if (type.startsWith("json-")) {
				type = type.substring(5);
				attrs.put("type", type);
				attrs.put("target", MetaJsonRecord.class.getName());
				if (record.getTargetJsonModel() != null) {
					final MetaJsonModel targetModel = record.getTargetJsonModel();
					String domain = String.format("self.jsonModel = '%s'", targetModel.getName());
					if (!StringUtils.isBlank(record.getDomain())) {
						domain = String.format("(%s) AND (%s)", domain, record.getDomain()); 
					}
					attrs.put("domain", domain);
					attrs.put("gridView", targetModel.getGridView().getName());
					attrs.put("formView", targetModel.getFormView().getName());
					attrs.put("targetName", "name");
					attrs.put("jsonTarget", targetModel.getName());
				}
			}

			if (StringUtils.notBlank(record.getSelection())) {
				attrs.put("selectionList", getSelectionList(record.getSelection()));
			}
			
			if (StringUtils.notBlank(record.getEnumType())) {
				try {
					attrs.put("selectionList", getSelectionList(Class.forName(record.getEnumType())));
				} catch (ClassNotFoundException e) {
					log.error("No such enum type found: {}", record.getEnumType());
				}
			}

			attrs.put("jsonField", fieldName);
			attrs.put("jsonPath", record.getName());
			if (type.matches("integer|decimal|boolean")) {
				attrs.put("jsonType", type);
			}

			fields.put(record.getName(), attrs);
		}
		return fields;
	}
	
	public static List<Selection.Option> getSelectionList(Class<?> enumType) {
		if (enumType == null || !enumType.isEnum()) {
			return null;
		}
		final List<Selection.Option> all = new ArrayList<>();
		for (Enum<?> item : enumType.asSubclass(Enum.class).getEnumConstants()) {
			final Selection.Option option = new Selection.Option();
			final String name = item.name();
			
			option.setValue(name);

			if (item instanceof ValueEnum<?>) {
				Object value = ((ValueEnum<?>) item).getValue();
				if (!Objects.equal(name, value)) {
					Map<String, Object> data = new HashMap<>();
					data.put("value", value);
					option.setData(data);
				}
			}

			try {
				final Field field = enumType.getDeclaredField(name);
				final Widget widget = field.getAnnotation(Widget.class);
				if (StringUtils.notBlank(widget.title())) {
					option.setTitle(widget.title());;
				}
			} catch (Exception e) {
			}

			if (option.getTitle() == null) {
				option.setTitle(Inflector.getInstance().humanize(name));
			}

			all.add(option);
		}
		return all;
	}

	public static List<Selection.Option> getSelectionList(String selection) {
		if (StringUtils.isBlank(selection)) {
			return null;
		}

		final Map<String, Selection.Option> all = buildSelectionMap(selection);
		if(all == null) {
			return null;
		}

		final List<Selection.Option> values = new ArrayList<>(all.values());
		Collections.sort(values, new Comparator<Selection.Option>() {
			@Override
			public int compare(Selection.Option o1, Selection.Option o2) {
				Integer n = o1.getOrder();
				Integer m = o2.getOrder();

				if (n == null) n = 0;
				if (m == null) m = 0;

				return Integer.compare(n, m);
			}
		});

		return values;
	}

	public static Selection.Option getSelectionItem(String selection, String value) {
		if (StringUtils.isBlank(selection)) {
			return null;
		}

		final Map<String, Selection.Option> all = buildSelectionMap(selection);
		if(all == null) {
			return null;
		}

		return all.get(value);
	}

	private static Map<String, Selection.Option> buildSelectionMap(String selection) {
		final List<MetaSelectItem> items = Query.of(MetaSelectItem.class)
				.filter("self.select.name = ?", selection)
				.order("select.priority")
				.order("order")
				.fetch();

		if (items.isEmpty()) {
			return null;
		}

		final Map<String, Selection.Option> all = new LinkedHashMap<>();

		for (MetaSelectItem item : items) {
			if (item.getHidden() == Boolean.TRUE) {
				all.remove(item.getValue());
			} else {
				all.put(item.getValue(), getSelectionItem(item));
			}
		}

		return all;
	}

	private static Selection.Option getSelectionItem(MetaSelectItem item) {
		final ObjectMapper objectMapper = Beans.get(ObjectMapper.class);
		final Selection.Option option = new Selection.Option();
		option.setValue(item.getValue());
		option.setTitle(item.getTitle());
		option.setIcon(item.getIcon());
		option.setOrder(item.getOrder());
		option.setHidden(item.getHidden());
		try {
			option.setData(objectMapper.readValue(item.getData(), new TypeReference<Map<String,Object>>() {}));
		} catch (Exception e) {
			// this should never happen, ignore
		}
		return option;
	}

	public static void clear() {
		ACTIONS.invalidateAll();
	}

	public static void invalidate(String name) {
		ACTIONS.invalidate(name);
	}
}
