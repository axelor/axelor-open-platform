package com.axelor.meta.service;

import java.util.List;

import org.apache.shiro.authz.AuthorizationException;

import com.axelor.auth.AuthUtils;
import com.axelor.auth.db.User;
import com.axelor.meta.db.MetaFilter;
import com.axelor.meta.db.MetaUser;
import com.google.common.base.Objects;
import com.google.inject.persist.Transactional;

public class MetaUserService {

	@Transactional
	public MetaUser getPreferences() {
		User user = AuthUtils.getUser();
		MetaUser prefs = MetaUser.findByUser(user);
		if (prefs == null) {
			prefs = new MetaUser();
			prefs.setUser(user);
			prefs = prefs.save();
		}
		return prefs;
	}
	
	@Transactional
	public MetaFilter saveFilter(MetaFilter ctx) {
		User user = AuthUtils.getUser();
		String query = "self.name = ?1 AND self.filterView = ?2 AND (self.user.code = ?3 OR self.shared = true)";
		MetaFilter filter = MetaFilter.filter(query, ctx.getName(), ctx.getFilterView(), user.getCode()).fetchOne();
		
		if (filter == null) {
			filter = new MetaFilter();
			filter.setName(ctx.getName());
			filter.setUser(user);
			filter.setFilterView(ctx.getFilterView());
		}

		filter.setTitle(ctx.getTitle());
		filter.setFilters(ctx.getFilters());
		filter.setFilterCustom(ctx.getFilterCustom());
		
		if (Objects.equal(filter.getUser(), user)) {
			filter.setShared(ctx.getShared());
		}
		
		return filter.save();
	}
	
	@Transactional
	public MetaFilter removeFilter(MetaFilter ctx) {
		User user = AuthUtils.getUser();
		String query = "self.name = ?1 AND self.filterView = ?2 AND (self.user.code = ?3 OR self.shared = true)";
		MetaFilter filter = MetaFilter.filter(query, ctx.getName(), ctx.getFilterView(), user.getCode()).fetchOne();
		
		if (!Objects.equal(filter.getUser(), user)) {
			throw new AuthorizationException("You are not allowed to remove this filter");
		}
		
		filter.remove();

		return ctx;
	}
	
	public List<MetaFilter> getFilters(String filterView) {
		User user = AuthUtils.getUser();
		String query = "self.filterView = ?1 AND (self.user.code = ?2 OR self.shared = true)";
		return MetaFilter.filter(query, filterView, user.getCode()).order("id").fetch();
	}
}
