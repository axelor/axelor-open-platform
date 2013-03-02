package com.axelor.auth;

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
import com.google.common.collect.Lists;

@Singleton
class AuthSecurity implements JpaSecurity, Provider<JpaSecurity> {
	
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
	
	@Override
	public Filter getFilter(String action, Class<? extends Model> model, Object... ids) {

		User user = getUser();
		if (user == null) {
			return null;
		}

		Mapper mapper = Mapper.of(Permission.class);
		List<Filter> filters = Lists.newArrayList();

		LocalDate date = new LocalDate();
		LocalDateTime dateTime = new LocalDateTime();

		for(Permission permission : user.getGroup().getPermissions()) {
			if (Strings.isNullOrEmpty(permission.getCondition()) ||
				!Objects.equal(model.getName(), permission.getObjectName())) {
				continue;
			}
			Boolean permitted = (Boolean) mapper.get(permission, action);
			if (permitted != Boolean.TRUE) {
				continue;
			}

			String params = permission.getParams();
			List<Object> args = Lists.newArrayList();
			
			for(String param : (params == null ? "" : params).split(",")) {
				param = param.trim();
				if ("__user__".equals(param)) {
					args.add(user);
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
		User user = getUser();
		if (user == null) {
			return;
		}
		
		Mapper mapper = Mapper.of(Permission.class);
		
		for(Permission permission : user.getGroup().getPermissions()) {
			if (!Strings.isNullOrEmpty(permission.getCondition()) ||
				!Objects.equal(model.getName(), permission.getObjectName())) {
				continue;
			}
			Boolean permitted = (Boolean) mapper.get(permission, action);
			if (permitted == null || permitted == Boolean.FALSE) {
				throw new UnauthorizedException(
						"You are not authorized to access this resource");
			}
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
			throw new UnauthorizedException("You are not authorized to access this resource.");
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
