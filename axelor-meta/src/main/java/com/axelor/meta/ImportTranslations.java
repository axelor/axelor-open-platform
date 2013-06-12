package com.axelor.meta;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.axelor.meta.db.MetaTranslation;
import com.google.common.collect.Lists;

public class ImportTranslations {
	
	private final Logger LOG = LoggerFactory.getLogger(ActionHandler.class);

	@SuppressWarnings("rawtypes")
	public Object loadTranslation(Object bean, Map values) {

		MetaTranslation meta = (MetaTranslation) bean;
		MetaTranslation foundedTranslation = null;

		if(meta.getKey() == null) {
			return null;
		}

		List<Object> params = Lists.newArrayList();
		String query = "self.key = ?1 AND self.language = ?2";
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

		foundedTranslation = MetaTranslation.all().filter(query, params.toArray()).fetchOne();

		if(foundedTranslation != null) {
			LOG.trace("Found translation : " + foundedTranslation);
			foundedTranslation.setTranslation(meta.getTranslation());
			foundedTranslation.save();
			return null;
		}

		return meta;
	}
	
}
