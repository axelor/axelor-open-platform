package com.axelor.meta;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.axelor.meta.db.MetaTranslation;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;

public class ImportTranslations {
	
	private final Logger LOG = LoggerFactory.getLogger(ActionHandler.class);

	@SuppressWarnings("rawtypes")
	public Object loadTranslation(Object bean, Map values) {

		MetaTranslation meta = (MetaTranslation) bean;
		MetaTranslation foundedTranslation = null;
		String help = values.get("help").toString();
		String help_t = values.get("help_t").toString();
		
		if(!Strings.isNullOrEmpty(help) && !Strings.isNullOrEmpty(help_t) && "viewField".equals(meta.getType())) {
			MetaTranslation helpT = new MetaTranslation();
			helpT.setDomain(meta.getDomain());
			helpT.setKey(help);
			helpT.setLanguage(meta.getLanguage());
			helpT.setTranslation(help_t);
			helpT.setType("help");
			helpT.save();
		}
		else if(!Strings.isNullOrEmpty(help) && !Strings.isNullOrEmpty(help_t) && "field".equals(meta.getType()) && meta.getKey() != null) {
			MetaTranslation helpT = new MetaTranslation();
			helpT.setDomain(meta.getDomain());
			helpT.setKey(meta.getKey());
			helpT.setLanguage(meta.getLanguage());
			helpT.setTranslation(help_t);
			helpT.setType("help");
			helpT.save();
		}
		
		if(meta.getKey() == null) {
			return null;
		}
		
		if("viewField".equals(meta.getType())) {
			meta.setType("field");
		}
		
		foundedTranslation = searchMetaTranslation(meta);

		if(foundedTranslation != null) {
			LOG.trace("Found translation : " + foundedTranslation);
			foundedTranslation.setTranslation(meta.getTranslation());
			foundedTranslation.save();
			return null;
		}

		return meta;
	}
	
	private MetaTranslation searchMetaTranslation(MetaTranslation meta) {
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

		return MetaTranslation.all().filter(query, params.toArray()).fetchOne();
	}
	
}
