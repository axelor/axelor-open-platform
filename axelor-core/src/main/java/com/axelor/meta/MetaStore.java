/**
 * Axelor Business Solutions
 *
 * Copyright (C) 2012-2014 Axelor (<http://axelor.com>).
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

import java.util.concurrent.ConcurrentMap;

import com.axelor.meta.loader.XMLViews;
import com.axelor.meta.schema.ObjectViews;
import com.axelor.meta.schema.actions.Action;
import com.axelor.meta.schema.views.AbstractView;
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
		if (CACHE.containsKey(name)) {
			return (Action) CACHE.get(name);
		}
		Action action = XMLViews.findAction(name);
		if (action != null) {
			register(name, action);
		}
		return action;
	}
	
	public static void clear() {
		CACHE.clear();
	}
}
