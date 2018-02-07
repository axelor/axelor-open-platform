/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2018 Axelor (<http://axelor.com>).
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
package com.axelor.auth;

import com.axelor.db.JpaSecurity.AccessType;

public class AuthSecurityException extends RuntimeException {

	private static final long serialVersionUID = -794508889899422879L;

	private AccessType type;

	private Class<?> model;

	private Long[] ids;
	
	public AuthSecurityException(AccessType type) {
		this(type, null);
	}

	public AuthSecurityException(AccessType type, Class<?> model, Long... ids) {
		this.type = type;
		this.model = model;
		this.ids = ids;
	}

	public AccessType getType() {
		return type;
	}

	public Class<?> getModel() {
		return model;
	}

	public Long[] getIds() {
		return ids;
	}
	
	@Override
	public String getMessage() {
		if (model == null || AuthUtils.getUser() == null || !AuthUtils.isTechnicalStaff(AuthUtils.getUser())) {
			return type.getMessage();
		}
		final StringBuilder builder = new StringBuilder(type.getMessage()).append("[").append(model.getName());
		if (ids.length > 0) {
			builder.append("#");
			for (int i = 0, n = Math.min(5, ids.length); i < n; i++) {
				builder.append(ids[i]);
				if (i < n - 1) {
					builder.append(",");
				}
			}
			if (ids.length > 5) {
				builder.append(",...");
			}
		}
		builder.append("]");
		return builder.toString();
	}
}
