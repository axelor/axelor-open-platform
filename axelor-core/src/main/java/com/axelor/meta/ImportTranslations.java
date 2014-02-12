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
package com.axelor.meta;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.axelor.meta.db.MetaTranslation;
import com.axelor.meta.db.MetaView;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;

public class ImportTranslations {

	private final Logger LOG = LoggerFactory.getLogger(ActionHandler.class);

	@SuppressWarnings("rawtypes")
	public void loadTranslation(Object bean, Map values) {

		MetaTranslation meta = (MetaTranslation) bean;

		String help = values.get("help").toString();
		String help_t = values.get("help_t").toString();

		if(!Strings.isNullOrEmpty(help) && !Strings.isNullOrEmpty(help_t) && "viewField".equals(meta.getType())) {
			this.createOrUpdateTranslation(help, "help", meta.getModule(), meta.getDomain(), help_t, meta.getLanguage());
		}
		else if(!Strings.isNullOrEmpty(help_t) && "field".equals(meta.getType()) && meta.getKey() != null) {
			this.createOrUpdateTranslation(meta.getKey(), "help", meta.getModule(), meta.getDomain(), help_t, meta.getLanguage());
		}
		else if("documentation".equals(meta.getType())) {
			if(!Strings.isNullOrEmpty(help_t)) {
				MetaView view = MetaView.all().filter("self.name = ?1 AND self.module = ?2", meta.getDomain(), meta.getModule()).fetchOne();
				if(view != null && view.getModel() != null) {
					this.createOrUpdateTranslation(meta.getKey(), "help", meta.getModule(), view.getModel(), help_t, meta.getLanguage());
				}
			}
			if(!Strings.isNullOrEmpty(values.get("title").toString())) {
				this.createOrUpdateTranslation(meta.getKey(), "documentation", meta.getModule(), meta.getDomain(), values.get("title").toString(), meta.getLanguage());
			}
		}

		if(meta.getKey() == null || ("documentation".equals(meta.getType()) && Strings.isNullOrEmpty(meta.getTranslation()))) {
			return ;
		}

		//`viewFiled` type are considered as `field` type
		if("viewField".equals(meta.getType())) {
			meta.setType("field");
		}

		this.createOrUpdateTranslation(meta);
	}

	private MetaTranslation searchMetaTranslation(MetaTranslation meta) {
		List<Object> params = Lists.newArrayList();
		String query = "self.module = ?1 AND self.key = ?2 AND self.language = ?3";
		params.add(meta.getModule());
		params.add(meta.getKey());
		params.add(meta.getLanguage());

		if(meta.getDomain() != null) {
			query += " AND self.domain = ?" + (params.size() + 1);
			params.add(meta.getDomain());
		}
		else {
			query += " AND self.domain IS NULL";
		}

		if(meta.getType() != null) {
			query += " AND self.type = ?" + (params.size() + 1);
			params.add(meta.getType());
		}
		else {
			query += " AND self.type IS NULL";
		}

		return MetaTranslation.all().filter(query, params.toArray()).fetchOne();
	}

	private void createOrUpdateTranslation(MetaTranslation meta) {

		MetaTranslation foundedTranslation = searchMetaTranslation(meta);

		if(foundedTranslation != null) {
			LOG.trace("Found translation : " + foundedTranslation);
			foundedTranslation.setTranslation(meta.getTranslation());
			foundedTranslation.save();
			return ;
		}

		meta.save();
	}

	private void createOrUpdateTranslation(String key, String type, String module, String domain, String translation, String language) {

		MetaTranslation founded = MetaTranslation.findByAll(key, language, domain, type, module);

		if(founded == null) {
			founded = new MetaTranslation(key, language, translation, domain, type, module);
		}
		else {
			founded.setTranslation(translation);
		}

		founded.save();
	}

}
