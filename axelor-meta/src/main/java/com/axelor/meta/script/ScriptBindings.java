package com.axelor.meta.script;

import java.util.Map;
import java.util.Set;

import javax.script.SimpleBindings;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;

import com.axelor.auth.AuthUtils;
import com.axelor.db.JPA;
import com.axelor.db.Model;
import com.axelor.rpc.Context;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;

public class ScriptBindings extends SimpleBindings {

	private static final Set<String> META_VARS = ImmutableSet.of(
			"__this__",
			"__self__",
			"__user__",
			"__ref__",
			"__parent__",
			"__date__",
			"__time__",
			"__datetime__"
			);

	private Map<String, Object> variables;

	public ScriptBindings(Context context) {
		this.variables = context;
	}

	public ScriptBindings(Map<String, Object> variables) {
		this.variables = variables;
	}

	@SuppressWarnings("all")
	private Object getSpecial(String name) throws Exception {
		Context context = (Context) variables;
		switch (name) {
		case "__this__":
			return context.asType(Model.class);
		case "__parent__":
			return context.getParentContext();
		case "__date__":
			return new LocalDate();
		case "__time__":
			return new LocalDateTime();
		case "__datetime__":
			return new DateTime();
		case "__user__":
			return AuthUtils.getUser();
		case "__self__":
			Model bean = context.asType(Model.class);
			if (bean == null || bean.getId() == null) return null;
			return JPA.find(bean.getClass(), bean.getId());
		case "__ref__":
			Map values = (Map) context.get("_ref");
			Class<?> klass = Class.forName((String) values.get("_model"));
			return JPA.em().find(klass, Long.parseLong(values.get("id").toString()));
		}
		return null;
	}

	public <T> T asType(Class<T> type) {
		Preconditions.checkState(variables instanceof Context,
				"Invalid binding, only Context bindings can be converted.");
		return ((Context) variables).asType(type);
	}

	@Override
	public boolean containsKey(Object key) {
		if (META_VARS.contains(key) || super.containsKey(key)) {
			return true;
		}
		return variables.containsKey(key);
	}

	@Override
	public Object get(Object key) {

		if (super.containsKey(key)){
			return super.get(key);
		}

		if (variables.containsKey(key)) {
			return variables.get(key);
		}

		if (META_VARS.contains(key)) {
			Object value = null;
			try {
				value = getSpecial((String) key);
			} catch (Exception e){}
			super.put((String) key, value);
			return value;
		}

		return null;
	}

	@Override
	public Object put(String name, Object value) {
		if (META_VARS.contains(name) || name.startsWith("$") || name.startsWith("__")) {
			return super.put(name, value);
		}
		return variables.put(name, value);
	}

	@Override
	public void putAll(Map<? extends String, ? extends Object> toMerge) {
		final Map<String, Object> values = Maps.newHashMap();
		for(String name : toMerge.keySet()) {
			Object value = toMerge.get(name);
			if (META_VARS.contains(name)) {
				this.put(name, value);
			} else {
				values.put(name, values);
			}
		}
		variables.putAll(values);
	}
}
