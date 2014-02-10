/**
 * Copyright (c) 2012-2014 Axelor. All Rights Reserved.
 *
 * The contents of this file are subject to the Common Public
 * Attribution License Version 1.0 (the "License"); you may not use
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
 * Software distributed under the License is distributed on an "AS IS"
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
package com.axelor.meta.service;

import java.util.List;

import org.apache.shiro.authz.AuthorizationException;

import com.axelor.auth.AuthUtils;
import com.axelor.auth.db.User;
import com.axelor.meta.db.MetaFilter;
import com.google.common.base.Objects;
import com.google.inject.persist.Transactional;

public class MetaFilterService {

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
