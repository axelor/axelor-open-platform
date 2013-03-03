package com.axelor.auth;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;

import java.util.List;

import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.shiro.authz.UnauthorizedException;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;

import com.axelor.auth.db.Permission;
import com.axelor.auth.db.User;
import com.axelor.db.JpaSecurity;
import com.axelor.db.Model;
import com.axelor.db.mapper.Mapper;
import com.axelor.rpc.filter.Filter;
import com.axelor.rpc.filter.JPQLFilter;
import com.google.common.base.Objects;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

@Singleton
class AuthSecurity implements JpaSecurity, Provider<JpaSecurity> {

	private static final String MESSAGE = "You are not authorized to access this resource.";

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
	
	private Object eval(Object bean, String prefix, String expr) {
		if (bean == null) {
			return null;
		}
		GroovyShell shell = new GroovyShell(new Binding(ImmutableMap.of(prefix, bean)));
		return shell.evaluate(expr);
	}

	@Override
	public Filter getFilter(String action, Class<? extends Model> model, Object... ids) {

		final User user = getUser();
		if (user == null) {
			return null;
		}

		final Mapper mapper = Mapper.of(Permission.class);
		final List<Filter> filters = Lists.newArrayList();

		final LocalDate date = new LocalDate();
		final LocalDateTime dateTime = new LocalDateTime();

		for(final Permission permission : user.getGroup().getPermissions()) {
			if (!Objects.equal(model.getName(), permission.getObjectName())
					|| Strings.isNullOrEmpty(permission.getCondition())) {
				continue;
			}
			final Boolean permitted = (Boolean) mapper.get(permission, action);
			if (permitted != Boolean.TRUE) {
				continue;
			}

			final String params = permission.getParams();
			final List<Object> args = Lists.newArrayList();

			for(String param : (params == null ? "" : params).split(",")) {
				param = param.trim();
				if ("__user__".equals(param)) {
					args.add(user);
				} else if (param.startsWith("__user__.")) {
					args.add(this.eval(user, "__user__", param));
				} else if ("__date__".equals(param)) {
					args.add(date);
				} else if ("__time__".equals(param)) {
					args.add(dateTime);
				} else {
					args.add(param);
				}
			}
			filters.add(new JPQLFilter(permission.getCondition(), args.toArray()));
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
	public void check(String action, Class<? extends Model> model) {
		final User user = getUser();
		if (user == null) {
			return;
		}

		final Mapper mapper = Mapper.of(Permission.class);
		boolean checkRestricted = true;

		for(Permission permission : user.getGroup().getPermissions()) {
			if (!Objects.equal(model.getName(), permission.getObjectName())
					|| !Strings.isNullOrEmpty(permission.getCondition())) {
				continue;
			}
			Boolean permitted = (Boolean) mapper.get(permission, action);
			if (permitted == null || permitted == Boolean.FALSE) {
				throw new UnauthorizedException(MESSAGE);
			}
			checkRestricted = false;
		}

		if (checkRestricted && user.getGroup().getRestricted() == Boolean.TRUE) {
			throw new UnauthorizedException(MESSAGE);
		}
	}

	@Override
	public void check(String action, Class<? extends Model> model, Long id) {
		User user = getUser();
		if (user == null) {
			return;
		}
		
		check(action, model);

		Filter filter = this.getFilter(action, model, id);
		if (filter == null) {
			return;
		}
		
		Model found = filter.build(model).fetchOne();
		if (found == null || !Objects.equal(found.getId(), id)) {
			throw new UnauthorizedException(MESSAGE);
		}
	}

	@Override
	public void check(String action, Model entity) {
		User user = getUser();
		if (user == null || entity == null) {
			return;
		}
		check(action, entity.getClass(), entity.getId());
	}

	@Override
	public JpaSecurity get() {
		return new AuthSecurity();
	}
}
