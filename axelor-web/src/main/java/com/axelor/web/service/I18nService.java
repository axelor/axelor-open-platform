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
package com.axelor.web.service;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.axelor.i18n.I18n;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.servlet.RequestScoped;

@RequestScoped
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Path("/i18n")
public class I18nService extends AbstractService {

	@Inject
	private ObjectMapper mapper;

	@GET
	@Path("messages.js")
	public String getTranslations(@QueryParam("lang") String lang) {

		StringBuilder builder = new StringBuilder();
		ResourceBundle bundle = I18n.getBundle();
		if (bundle == null) {
			return builder.toString();
		}

		builder.append("(function(gettext) {");

		Enumeration<String> keys = bundle.getKeys();
		Map<String, String> messages = new HashMap<>();

		while (keys.hasMoreElements()) {
			String key = keys.nextElement();
			messages.put(key, I18n.get(key));
		}

		try {
			builder.append("gettext.put(").append(mapper.writeValueAsString(messages));
			builder.append(");");
		} catch (JsonProcessingException e) {
		}

		builder.append("}(_t));");

		return builder.toString();
	}
}
