package com.axelor.meta;

import java.util.Map;

import com.axelor.meta.db.MetaTranslation;

public class ImportTranslations {

	@SuppressWarnings("rawtypes")
	public Object loadTranslation(Object bean, Map values) {
		
		MetaTranslation meta = (MetaTranslation) bean;
		MetaTranslation foundedTranslation = null;
				
		if(meta.getDomain() == null){
			foundedTranslation = MetaTranslation.all().filter("" +
					"self.key = ?1 AND self.language = ?2 AND self.domain IS NULL", 
					meta.getKey(), meta.getLanguage()).fetchOne();
		}
		else {
			foundedTranslation = MetaTranslation.all().filter("" +
					"self.key = ?1 AND self.language = ?2 and self.domain = ?3", 
					meta.getKey(), meta.getLanguage(), meta.getDomain()).fetchOne();
		}
		
		if(foundedTranslation != null){
			System.out.println("Duplicate founded : " + foundedTranslation);
			foundedTranslation.setTranslation(meta.getTranslation());
			foundedTranslation.save();
			return null;
		}
		
		return meta;
	}
	
}
