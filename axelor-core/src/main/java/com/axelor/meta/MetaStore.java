/**
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2014 Axelor (<http://axelor.com>).
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

import javax.xml.namespace.QName;

import com.axelor.auth.AuthUtils;
import com.axelor.auth.db.User;
import com.axelor.common.StringUtils;
import com.axelor.db.JpaSecurity;
import com.axelor.db.JpaSecurity.AccessType;
import com.axelor.db.Query;
import com.axelor.inject.Beans;
import com.axelor.meta.db.MetaSelect;
import com.axelor.meta.db.MetaSelectItem;
import com.axelor.meta.db.repo.MetaSelectItemRepository;
import com.axelor.meta.loader.ModuleManager;
import com.axelor.meta.loader.XMLViews;
import com.axelor.meta.schema.ObjectViews;
import com.axelor.meta.schema.actions.Action;
import com.axelor.meta.schema.views.AbstractView;
import com.axelor.meta.schema.views.Selection;
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
		if (CACHE.containsKey(name)) {
			return (AbstractView) CACHE.get(name);
		}
		AbstractView view = XMLViews.findView(null, name, null);

		if (view != null) {
			register(name, view);
		}
		return view;
	}
	
	public static AbstractView getView(String name, String module) {
		if (module == null || "".equals(module.trim())) {
			return getView(name);
		}
		String key = module + ":" + name;
		if (CACHE.containsKey(key)) {
			return (AbstractView) CACHE.get(key);
		}
		AbstractView view = XMLViews.findView(name, module);

		if (view != null) {
			register(key, view);
		}
		return view;
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

		final MetaSelect select = Query.of(MetaSelect.class)
				.filter("self.name = ?", selection)
				.order("-priority")
				.fetchOne();

		if (select == null) {
			return null;
		}

		final List<MetaSelectItem> items = Query
				.of(MetaSelectItem.class)
				.filter("self.select.id = ?", select.getId())
				.order("order")
				.fetch();

		if (items == null || items.isEmpty()) {
			return null;
		}

		final List<Selection.Option> all = new ArrayList<>();
		final Set<String> visited = new HashSet<>();

		for(MetaSelectItem item : items) {
			if (visited.contains(item.getValue())) {
				continue;
			}
			visited.add(item.getValue());
			all.add(getSelectionItem(item));
		}

		return all;
	}

	public static Selection.Option getSelectionItem(String selection, String value) {
		final MetaSelectItem item = Beans.get(MetaSelectItemRepository.class).all()
				.filter("self.select.name = ?1 AND self.value = ?2", selection, value)
				.fetchOne();
		if (item == null) {
			return null;
		}
		return getSelectionItem(item);
	}

	private static Selection.Option getSelectionItem(MetaSelectItem item) {
		Selection.Option option = new Selection.Option();
		option.setValue(item.getValue());
		option.setTitle(item.getTitle());
		String data = item.getData();
		if (data != null) {
			Map<QName, String> attrs = new HashMap<>();
			QName qn = new QName("x-data");
			attrs.put(qn, data);
			option.setData(attrs);
		}
		return option;
	}

	public static void clear() {
		CACHE.clear();
	}
}
