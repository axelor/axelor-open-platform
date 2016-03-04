/**
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2016 Axelor (<http://axelor.com>).
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

import com.axelor.auth.AuthUtils;
import com.axelor.auth.db.User;
import com.axelor.common.StringUtils;
import com.axelor.db.JpaSecurity;
import com.axelor.db.JpaSecurity.AccessType;
import com.axelor.db.Query;
import com.axelor.inject.Beans;
import com.axelor.meta.db.MetaSelectItem;
import com.axelor.meta.loader.ModuleManager;
import com.axelor.meta.loader.XMLViews;
import com.axelor.meta.schema.ObjectViews;
import com.axelor.meta.schema.actions.Action;
import com.axelor.meta.schema.views.AbstractView;
import com.axelor.meta.schema.views.Selection;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;

public class MetaStore {

	private static final ConcurrentMap<String, Object> CACHE = Maps.newConcurrentMap();
	
	private static Object register(String key, Object value) {
		CACHE.put(key, value);
		return value;
	}
	
	/**
	 * Used for unit testing.
	 * 
	 */
	static void resister(ObjectViews views) {
		try {
			for(AbstractView item : views.getViews())
				register(item.getName(), item);
		} catch (NullPointerException e){}
		try {
			for(Action item : views.getActions())
				register(item.getName(), item);
		} catch (NullPointerException e){}
	}
	
	public static AbstractView getView(String name) {
		return getView(name, null);
	}
	
	public static AbstractView getView(String name, String module) {
		// for unit tests
		if (StringUtils.isBlank(module) && CACHE.containsKey(name)) {
			return (AbstractView) CACHE.get(name);
		}
		return StringUtils.isBlank(module) ? XMLViews.findView(null, name, null) : XMLViews.findView(name, module);
	}

	public static Action getAction(String name) {
		Action action = (Action) CACHE.get(name);
		if (action == null) {
			action = XMLViews.findAction(name);
			if (action != null) {
				register(name, action);
			}
		}

		if (action == null) return null;

		final String module = action.getModuleToCheck();
		if (StringUtils.isBlank(module) || ModuleManager.isInstalled(module)) {
			return action;
		}

		return null;
	}
	
	@SuppressWarnings("all")
	public static Map<String, Object> getPermissions(Class<?> model) {
		final User user = AuthUtils.getUser();

		if (user == null || "admin".equals(user.getCode())) return null;
		if (user.getGroup() != null && "admins".equals(user.getGroup().getCode())) return null;

		final Map<String, Object> map = new HashMap<>();
		final JpaSecurity security = Beans.get(JpaSecurity.class);

		map.put("read", security.isPermitted(AccessType.READ, (Class) model));
		map.put("write", security.isPermitted(AccessType.WRITE, (Class) model));
		map.put("create", security.isPermitted(AccessType.CREATE, (Class) model));
		map.put("remove", security.isPermitted(AccessType.REMOVE, (Class) model));
		map.put("export", security.isPermitted(AccessType.EXPORT, (Class) model));

		return map;
	}

	public static List<Selection.Option> getSelectionList(String selection) {
		if (StringUtils.isBlank(selection)) {
			return null;
		}

		final Map<String, Selection.Option> all = buildSelectionMap(selection);
		if(all == null) {
			return null;
		}

		return new ArrayList<>(all.values());
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
				.order("order")
				.order("createdOn")
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

	@SuppressWarnings("unchecked")
	private static Selection.Option getSelectionItem(MetaSelectItem item) {
		Selection.Option option = new Selection.Option();
		option.setValue(item.getValue());
		option.setTitle(item.getTitle());
		option.setIcon(item.getIcon());
		option.setOrder(item.getOrder());
		option.setHidden(item.getHidden());
		ObjectMapper objectMapper = Beans.get(ObjectMapper.class);
		try {
			option.setData(objectMapper.readValue(item.getData(), Map.class));
		} catch (Exception e) {
		}
		return option;
	}

	public static void clear() {
		CACHE.clear();
	}
}
