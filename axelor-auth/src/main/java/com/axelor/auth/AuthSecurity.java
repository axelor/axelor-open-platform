/**
 * Copyright (c) 2012-2014 Axelor. All Rights Reserved.
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
 * Copyright (c) 2012-2014 Axelor. All Rights Reserved.
 */
package com.axelor.auth;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;

import java.util.List;
import java.util.Set;

import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.shiro.authz.UnauthorizedException;

import com.axelor.auth.db.Permission;
import com.axelor.auth.db.User;
import com.axelor.db.JpaSecurity;
import com.axelor.db.Model;
import com.axelor.rpc.filter.Filter;
import com.axelor.rpc.filter.JPQLFilter;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

@Singleton
class AuthSecurity implements JpaSecurity, Provider<JpaSecurity> {

	private static final class Condition {

		private Filter filter;

		public Condition(User user, String condition, String params) {

			final List<Object> args = Lists.newArrayList();

			for(String param : (params == null ? "" : params).split(",")) {
				param = param.trim();
				if ("__user__".equals(param)) {
					args.add(user);
				} else if (param.startsWith("__user__.")) {
					args.add(this.eval(user, "__user__", param));
				} else {
					args.add(param);
				}
			}

			this.filter = new JPQLFilter(condition, args.toArray());
		}

		private Object eval(Object bean, String prefix, String expr) {
			if (bean == null) {
				return null;
			}
			GroovyShell shell = new GroovyShell(new Binding(ImmutableMap.of(prefix, bean)));
			return shell.evaluate(expr);
		}

		public Filter getFilter() {
			return filter;
		}

		@Override
		public String toString() {
			return filter.getQuery();
		}
	}
	
	private AuthResolver authResolver = new AuthResolver();

	private User getUser() {
		User user = AuthUtils.getUser();
		if (user == null || "admin".equals(user.getCode())
				|| user.getGroup() == null
				|| "admins".equals(user.getGroup().getCode())
				|| user.getGroup().getPermissions() == null) {
			return null;
		}
		return user;
	}

	private Condition getCondition(User user, Permission permission, AccessType accessType) {
		final String condition = permission.getCondition();
		final String params = permission.getConditionParams();
		if (condition == null || "".equals(condition.trim())) {
			return null;
		}
		return new Condition(user, condition, params);
	}
	
	@Override
	public boolean hasRole(String name) {
		final User user = getUser();
		if (user == null) {
			return true;
		}
		return AuthUtils.hasRole(user, name);
	}
	
	@Override
	public Set<AccessType> getAccessTypes(Class<? extends Model> model, Long id) {
		final Set<AccessType> types = Sets.newHashSet();
		for (AccessType type : AccessType.values()) {
			if (isPermitted(type, model, id)) {
				types.add(type);
			}
		}
		return types;
	}

	@Override
	public Filter getFilter(AccessType type, Class<? extends Model> model, Long... ids) {
		final User user = getUser();
		if (user == null) {
			return null;
		}

		final List<Filter> filters = Lists.newArrayList();
		final Set<Permission> permissions = authResolver.resolve(user, model.getName(), type);
		if (permissions.isEmpty()) {
			return null;
		}
		
		for (Permission permission : permissions) {
			Condition condition = this.getCondition(user, permission, type);
			if (condition != null) {
				filters.add(condition.getFilter());
			}
		}

		if (filters.isEmpty() && ids.length == 0) {
			return null;
		}
		
		Filter left = filters.isEmpty() ? null : Filter.or(filters);
		Filter right = null;

		if (ids != null && ids.length > 0 && ids[0] != null) {
			right = Filter.in("id", Lists.newArrayList(ids));
		}
		
		if (right == null) return left;
		if (left == null) return right;
		
		return Filter.and(left, right);
	}

	@Override
	public boolean isPermitted(AccessType type, Class<? extends Model> model, Long... ids) {
		final User user = getUser();
		if (user == null) {
			return true;
		}
		
		final Set<Permission> permissions = authResolver.resolve(user, model.getName(), type);
		if (permissions.isEmpty()) {
			return false;
		}
		
		if (ids == null || ids.length == 0) {
			return true;
		}
		
		final Filter filter = this.getFilter(type, model, ids);
		if (filter == null) {
			return true;
		}
		
		return filter.build(model).count() == ids.length;
	}
	
	@Override
	public void check(AccessType type, Class<? extends Model> model, Long... ids) {
		if (isPermitted(type, model, ids)) {
			return;
		}
		throw new UnauthorizedException(type.getMessage());
	}

	@Override
	public JpaSecurity get() {
		return new AuthSecurity();
	}
}
