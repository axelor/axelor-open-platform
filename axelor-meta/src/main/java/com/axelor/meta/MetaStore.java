package com.axelor.meta;

import java.util.concurrent.ConcurrentMap;

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
		MetaLoader loader = new MetaLoader();
		AbstractView view = loader.findView(null, name, null);

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
		MetaLoader loader = new MetaLoader();
		AbstractView view = loader.findView(name, module);

		if (view != null) {
			register(key, view);
		}
		return view;
	}

	public static Action getAction(String name) {
		if (CACHE.containsKey(name)) {
			return (Action) CACHE.get(name);
		}
		MetaLoader loader = new MetaLoader();
		Action action = loader.findAction(name);
		if (action != null) {
			register(name, action);
		}
		return action;
	}
	
	public static void clear() {
		CACHE.clear();
	}
}
