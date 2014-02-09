/**
 * Copyright (c) 2012-2014 Axelor. All Rights Reserved.
 *
 * The contents of this file are subject to the Common Public
 * Attribution License Version 1.0 (the “License”); you may not use
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
 * Software distributed under the License is distributed on an “AS IS”
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
package com.axelor.meta.loader;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.reflections.vfs.Vfs;
import org.reflections.vfs.Vfs.File;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.com.bytecode.opencsv.CSVReader;

import com.axelor.meta.ImportTranslations;
import com.axelor.meta.MetaScanner;
import com.axelor.meta.db.MetaTranslation;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.inject.persist.Transactional;

@Singleton
public class I18nLoader implements Loader {

	private Logger log = LoggerFactory.getLogger(I18nLoader.class);

	private static final String NAME_COLUMN =  "name";
	private static final String DOMAIN_COLUMN =  "domain";
	private static final String TITLE_COLUMN =  "title";
	private static final String TRANSLATION_COLUMN =  "title_t";
	private static final String CURRENT_MODULE =  "_currentModule";
	private static final String TYPE_COLUMN = "type";
	private static final String CURRENT_LANGUAGE =  "_currentLanguage";
	
	@Inject
	private ImportTranslations importTranslations;
	
	@Override
	public void load(Module module) {
		
		List<Vfs.File> files = MetaScanner.findAll(module.getName(), "i18n", "(.*?)\\.csv");
		
		Collections.sort(files, new Comparator<File>() {
			@Override
			public int compare(File o1, File o2) {
				String a = o1.toString();
				String b = o2.toString();
				if (a.contains("/classes/") && b.contains("/classes/")) return 0;
				if (a.contains("/classes/")) return 1;
				if (b.contains("/classes/")) return -1;
				return 0;
			}
		});

		for(Vfs.File file : files) {
			try {
				log.debug("Load translation: {}", file);
				process(file.openInputStream(), file.getName(), module.getName());
			} catch (IOException e) {
				log.error("Unable to import file: {}", file.getName());
			}
		}
	}

	@Transactional
	public void load(String importPath) {

		importPath = importPath.endsWith("/") ? importPath : importPath.concat("/");

		//Import by module resolver order
		for(String module : ModuleManager.getResolution()) {
			java.io.File moduleDir = new java.io.File(importPath + module);
			if(!moduleDir.exists() || !moduleDir.isDirectory() || moduleDir.listFiles() == null) {
				continue;
			}

			for(java.io.File file : moduleDir.listFiles()) {
				try {
					process(new FileInputStream(file), file.getName(), module);
				} catch (IOException e) {
					log.error("Unable to import file: {}", file.getName());
				}
			}
		}
	}
	
	private static Set<String> typeSet = Sets.newHashSet("field", "documentation");
	private static Set<String> domainSet = Sets.newHashSet("menu", "actionMenu", "tree", "chart", "search", "action", "select");
	
	private void process(final InputStream stream, String fileName, String moduleName) throws IOException {

		// Get language name from the file name
		String languageName = "";
		Pattern pattern = Pattern.compile("(\\w+)\\.(.*?)");
		Matcher matcher = pattern.matcher(fileName);
		if (matcher.matches()) {
			languageName = matcher.group(1);
		}
		
		Reader reader = new InputStreamReader(stream);
		CSVReader csvReader = new CSVReader(reader);

		try {
			String[] fields = csvReader.readNext();
			String[] values = null;
					
			while((values = csvReader.readNext()) != null) {
				if (isEmpty(values)) {
					continue;
				}
				Map<String, String> map = toMap(fields, values);
				
				map.put(CURRENT_LANGUAGE, languageName);
				map.put(CURRENT_MODULE, moduleName);
				
				String type = map.get(TYPE_COLUMN);
				
				MetaTranslation entity = new MetaTranslation();
				entity.setLanguage(languageName);
				entity.setTranslation(map.get(TRANSLATION_COLUMN));
				entity.setType(type);
				
				if (typeSet.contains(type)) {
					entity.setKey(map.get(NAME_COLUMN));
				} else {
					entity.setKey(map.get(TITLE_COLUMN));
				}
				if (!domainSet.contains(type)) {
					entity.setDomain(map.get(DOMAIN_COLUMN));
				}
	
				entity = (MetaTranslation) importTranslations.loadTranslation(entity, map);
			}
		} finally {
			csvReader.close();
		}
	}
	
	private Map<String, String> toMap(String[] fields, String[] values) {
		Map<String, String> map = Maps.newHashMap();
		for (int i = 0; i < fields.length; i++) {
			map.put(fields[i], values[i]);
		}
		return map;
	}
	
	private boolean isEmpty(String[] line) {
		if (line == null || line.length == 0)
			return true;
		if (line.length == 1 && (line[0] == null || "".equals(line[0].trim())))
			return true;
		return false;
	}
}
