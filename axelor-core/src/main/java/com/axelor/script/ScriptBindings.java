/**
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
package com.axelor.script;

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
import com.google.common.collect.Sets;
import com.google.common.primitives.Longs;

public class ScriptBindings extends SimpleBindings {

	private static final String MODEL_KEY = "_model";

	private static final Set<String> META_VARS = ImmutableSet.of(
			"__id__",
			"__ids__",
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

	public ScriptBindings(Map<String, Object> variables) {
		this.variables = this.tryContext(variables);
	}
	
	private Map<String, Object> tryContext(Map<String, Object> variables) {
		if (variables instanceof Context) {
			return variables;
		}
		Class<?> klass = null;
		try {
			klass = Class.forName((String) variables.get(MODEL_KEY));
		} catch (NullPointerException | ClassNotFoundException e) {
			return variables;
		}
		return Context.create(variables, klass);
	}

	@SuppressWarnings("all")
	private Object getSpecial(String name) throws Exception {
		switch (name) {
		case "__id__":
			if (variables.containsKey("id")) return Longs.tryParse(variables.get("id").toString());
			if (variables.containsKey("_id")) return Longs.tryParse(variables.get("_id").toString());
			return ((Context) variables).asType(Model.class).getId();
		case "__ids__":
			return variables.get("_ids");
		case "__this__":
			return ((Context) variables).asType(Model.class);
		case "__parent__":
			return ((Context) variables).getParentContext();
		case "__date__":
			return new LocalDate();
		case "__time__":
			return new LocalDateTime();
		case "__datetime__":
			return new DateTime();
		case "__user__":
			return AuthUtils.getUser();
		case "__self__":
			Model bean = ((Context) variables).asType(Model.class);
			if (bean == null || bean.getId() == null) return null;
			return JPA.em().getReference(bean.getClass(), bean.getId());
		case "__ref__":
			Map values = (Map) variables.get("_ref");
			Class<?> klass = Class.forName((String) values.get("_model"));
			return JPA.em().getReference(klass, Long.parseLong(values.get("id").toString()));
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
	public Set<String> keySet() {
		Set<String> keys = Sets.newHashSet(super.keySet());
		keys.addAll(META_VARS);
		keys.addAll(variables.keySet());
		return keys;
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
			String name = key.toString();
			Object value = null;
			try {
				value = getSpecial(name);
			} catch (Exception e){}
			super.put(name, value);
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
