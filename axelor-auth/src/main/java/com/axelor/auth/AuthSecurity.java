package com.axelor.auth;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;

import java.util.List;

import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.shiro.authz.UnauthorizedException;

import com.axelor.auth.db.Permission;
import com.axelor.auth.db.User;
import com.axelor.db.JpaSecurity;
import com.axelor.db.Model;
import com.axelor.rpc.filter.Filter;
import com.axelor.rpc.filter.JPQLFilter;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

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

		for(final Permission permission : user.getGroup().getPermissions()) {
			if (!Objects.equal(model.getName(), permission.getObject())) {
				continue;
			}
			Condition condition = this.getCondition(user, permission, type);
			if (condition != null) {
				filters.add(condition.getFilter());
			}
		}

		if (filters.isEmpty()) {
			return null;
		}
		
		if (ids != null && ids.length > 0 && ids[0] != null) {
			filters.add(0, Filter.in("self.id", Lists.newArrayList(ids)));
		}
		return Filter.and(filters);
	}

	@Override
	public void check(AccessType type, Class<? extends Model> model) {
		final User user = getUser();
		if (user == null) {
			return;
		}

		boolean checkRestricted = true;
		
		for(Permission permission : user.getGroup().getPermissions()) {
			if (!Objects.equal(model.getName(), permission.getObject())) {
				continue;
			}
			boolean permitted = hasAccess(permission, type);
			if (!permitted) {
				throw new UnauthorizedException(type.toString());
			}
			checkRestricted = false;
		}

		if (checkRestricted && user.getGroup().getRestricted() == Boolean.TRUE) {
			throw new UnauthorizedException(type.toString());
		}
	}

	@Override
	public void check(AccessType type, Class<? extends Model> model, Long id) {
		User user = getUser();
		if (user == null) {
			return;
		}

		check(type, model);

		Filter filter = this.getFilter(type, model, id);
		if (filter == null) {
			return;
		}

		Model found = filter.build(model).fetchOne();
		if (found == null || !Objects.equal(found.getId(), id)) {
			throw new UnauthorizedException(type.toString());
		}
	}

	@Override
	public void check(AccessType type, Model entity) {
		User user = getUser();
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
