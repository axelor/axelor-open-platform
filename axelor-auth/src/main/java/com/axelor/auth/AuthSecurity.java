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
import javax.persistence.FlushModeType;
import javax.persistence.TypedQuery;

import org.apache.shiro.authz.AuthorizationException;
import org.apache.shiro.authz.UnauthorizedException;

import com.axelor.auth.db.Permission;
import com.axelor.auth.db.User;
import com.axelor.db.JPA;
import com.axelor.db.JpaSecurity;
import com.axelor.db.Model;
import com.axelor.db.QueryBinder;
import com.axelor.rpc.filter.Filter;
import com.axelor.rpc.filter.JPQLFilter;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

@Singleton
class AuthSecurity implements JpaSecurity, Provider<JpaSecurity> {

	private static class Condition {

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
		final String condition;
		final String params;
		switch (accessType) {
		case READ:
			condition = permission.getReadCondition();
			params = permission.getReadParams();
			break;
		case WRITE:
			condition = permission.getWriteCondition();
			params = permission.getWriteParams();
			break;
		case CREATE:
			condition = permission.getCreateCondition();
			params = permission.getCreateParams();
			break;
		case REMOVE:
			condition = permission.getRemoveCondition();
			params = permission.getRemoveParams();
			break;
		default:
			return null;
		}

		if (condition == null || "".equals(condition.trim())) {
			return null;
		}
		return new Condition(user, condition, params);
	}

	private boolean hasAccess(Permission permission, AccessType accessType) {
		switch(accessType) {
		case READ:
			return permission.getCanRead() == Boolean.TRUE;
		case WRITE:
			return permission.getCanWrite() == Boolean.TRUE;
		case CREATE:
			return permission.getCanCreate() == Boolean.TRUE;
		case REMOVE:
			return permission.getCanRemove() == Boolean.TRUE;
		default:
			return false;
		}
	}

	@Override
	public Filter getFilter(AccessType type, Class<? extends Model> model, Object... ids) {

		final User user = getUser();
		if (user == null) {
			return null;
		}

		final List<Filter> filters = Lists.newArrayList();
		final Permission permission = getPermission(model, user);

		if (permission == null) {
			return null;
		}

		Condition condition = this.getCondition(user, permission, type);
		if (condition == null) {
			return null;
		}

		filters.add(condition.getFilter());

		if (ids != null && ids.length > 0 && ids[0] != null) {
			filters.add(0, Filter.in("self.id", Lists.newArrayList(ids)));
		}
		return Filter.and(filters);
	}

	private Permission getPermission(Class<? extends Model> model, User user) {
		if (model == null || user == null) {
			return null;
		}
		final TypedQuery<Permission> q = JPA.em().createQuery(
				"SELECT p FROM User u " +
				"LEFT JOIN u.group AS g " +
				"LEFT JOIN g.permissions AS p " +
				"WHERE u.code = :code AND p.object = :object", Permission.class);

		q.setMaxResults(1);
		q.setFlushMode(FlushModeType.COMMIT);

		QueryBinder.of(q)
			.bind("code", user.getCode())
			.bind("object", model.getName())
			.setCacheable();

		try {
			return q.getResultList().get(0);
		} catch (IndexOutOfBoundsException e){
			return null;
		}
	}

	@Override
	public Set<AccessType> perms(Class<? extends Model> model) {
		final User user = getUser();
		if (user == null) {
			return null;
		}

		final Set<AccessType> perms = Sets.newHashSet();
		final Permission permission = getPermission(model, user);

		if (permission == null) {
			return null;
		}

		if (hasAccess(permission, CAN_READ)) perms.add(CAN_READ);
		if (hasAccess(permission, CAN_WRITE)) perms.add(CAN_WRITE);
		if (hasAccess(permission, CAN_CREATE)) perms.add(CAN_CREATE);
		if (hasAccess(permission, CAN_REMOVE)) perms.add(CAN_REMOVE);

		return perms;
	}

	@Override
	public Set<AccessType> perms(Class<? extends Model> model, Long id) {
		final User user = getUser();
		if (user == null) {
			return null;
		}

		final Set<AccessType> perms = Sets.newHashSet();
		try {
			check(CAN_READ, model, id);
			perms.add(CAN_READ);
		} catch (AuthorizationException e) {
		}
		try {
			check(CAN_WRITE, model, id);
			perms.add(CAN_WRITE);
		} catch (AuthorizationException e) {
		}
		try {
			check(CAN_CREATE, model, id);
			perms.add(CAN_CREATE);
		} catch (AuthorizationException e) {
		}
		try {
			check(CAN_REMOVE, model, id);
			perms.add(CAN_REMOVE);
		} catch (AuthorizationException e) {
		}

		return perms;
	}

	@Override
	public Set<AccessType> perms(Model entity) {
		return perms(entity.getClass(), entity.getId());
	}

	@Override
	public void check(AccessType type, Class<? extends Model> model) {
		final User user = getUser();
		if (user == null) {
			return;
		}

		final Permission permission = getPermission(model, user);
		final boolean permitted = permission != null && hasAccess(permission, type);
		final boolean checkRestricted = permission == null;

		if (permission != null && !permitted) {
			throw new UnauthorizedException(type.toString());
		}

		if (checkRestricted && user.getGroup().getRestricted() == Boolean.TRUE) {
			throw new UnauthorizedException(type.toString());
		}
	}

	@Override
	public void check(AccessType type, Class<? extends Model> model, Long id) {
		final User user = getUser();
		if (user == null) {
			return;
		}

		check(type, model);

		final Filter filter = this.getFilter(type, model, id);
		if (filter == null) {
			return;
		}

		final Model found = filter.build(model).fetchOne();
		if (found == null || !Objects.equal(found.getId(), id)) {
			throw new UnauthorizedException(type.toString());
		}
	}

	@Override
	public void check(AccessType type, Model entity) {
		final User user = getUser();
		if (user == null || entity == null) {
			return;
		}
		check(type, entity.getClass(), entity.getId());
	}

	@Override
	public JpaSecurity get() {
		return new AuthSecurity();
	}
}
