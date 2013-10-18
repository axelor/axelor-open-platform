/**
 * Copyright (c) 2012-2013 Axelor. All Rights Reserved.
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
 * Copyright (c) 2012-2013 Axelor. All Rights Reserved.
 */
package com.axelor.meta.service;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Singleton;

import org.reflections.vfs.Vfs.File;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.axelor.data.ImportException;
import com.axelor.data.ImportTask;
import com.axelor.data.csv.CSVBinding;
import com.axelor.data.csv.CSVConfig;
import com.axelor.data.csv.CSVImporter;
import com.axelor.data.csv.CSVInput;
import com.axelor.meta.MetaScanner;
import com.axelor.meta.ModuleResolver;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.Injector;

@Singleton
public class MetaTranslationsService {

	private Logger log = LoggerFactory.getLogger(getClass());

	private static final String CSV_INPUT_FILE_NAME =  "[file.name]";
	private static final String CSV_INPUT_TYPE_NAME =  "com.axelor.meta.db.MetaTranslation";
	private static final String LANGUAGE_FIELD =  "language";
	private static final String NAME_COLUMN =  "name";
	private static final String DOMAIN_COLUMN =  "domain";
	private static final String TITLE_COLUMN =  "title";
	private static final String TRANSLATION_COLUMN =  "title_t";
	private static final String CURRENT_MODULE =  "_currentModule";
	private static final String TYPE_COLUMN = "type";
	private static final String CURRENT_LANGUAGE =  "_currentLanguage";
	private static final String CALLABLE = "com.axelor.meta.ImportTranslations:loadTranslation";

	@Inject
	Injector injector;

	public void process(ModuleResolver moduleResolver, String importPath) {
		if(Strings.isNullOrEmpty(importPath)){
			loadFromSources(moduleResolver);
		}
		else {
			loadFromDir(moduleResolver, importPath);
		}
	}

	private void loadFromDir(ModuleResolver moduleResolver, String importPath) {
		importPath = importPath.endsWith("/") ? importPath : importPath.concat("/");

		//Import by module resolver order
		for(String module : moduleResolver.all()) {
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

	private void loadFromSources(ModuleResolver moduleResolver) {
		List<File> files = MetaScanner.findAll("i18n\\.(.*?)\\.csv");

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

		//Import by module resolver order
		for(String module : moduleResolver.all()) {
			String pat = String.format("(/WEB-INF/lib/%s-)|(%s/WEB-INF/classes/)", module, module);
			Pattern pattern = Pattern.compile(pat);
			for(org.reflections.vfs.Vfs.File file : files) {
				String path = file.toString();
				Matcher matcher = pattern.matcher(path);
				if (matcher.find()) {
					try {
						process(file.openInputStream(), file.getName(), module);
					} catch (IOException e) {
						log.error("Unable to import file: {}", file.getName());
					}
				}
			}
		}
	}

	private void process(final InputStream stream, String fileName, String module) throws IOException {
		log.debug("Load translation for {} module", module);

		CSVConfig config = getCSVConfig();
		CSVImporter importer = new CSVImporter(injector, config);

		// Get language name from the file name
		String languageName = "";
		Pattern pattern = Pattern.compile("(\\w+)\\.(.*?)");
		Matcher matcher = pattern.matcher(fileName);
		if (matcher.matches()) {
			languageName = matcher.group(1);
		}

		// Prepare context
		Map<String, Object> context = Maps.newHashMap();
		context.put(CURRENT_LANGUAGE, languageName);
		context.put(CURRENT_MODULE, module);
		importer.setContext(context);

		// Run task
		importer.runTask(new ImportTask() {

			@Override
			public void configure() throws IOException {
				input(CSV_INPUT_FILE_NAME, stream);
			}

			@Override
			public boolean handle(ImportException e) {
				log.error("Error with folling exception : {}", e);
				return true;
			}
		});
	}

	private CSVConfig getCSVConfig() {

		CSVConfig csvConfig = new CSVConfig();
		List<CSVBinding> bindings = Lists.newArrayList();

		CSVInput input = new CSVInput();
		input.setFileName(CSV_INPUT_FILE_NAME);
		input.setTypeName(CSV_INPUT_TYPE_NAME);
		input.setCallable(CALLABLE);

		CSVBinding bindingLanguage = new CSVBinding();
		bindingLanguage.setField(LANGUAGE_FIELD);
		bindingLanguage.setExpression(CURRENT_LANGUAGE);
		bindings.add(bindingLanguage);

		CSVBinding bindingName = new CSVBinding();
		bindingName.setField("key");
		bindingName.setColumn(NAME_COLUMN);
		bindingName.setCondition("type in ['field', 'documentation']");
		bindings.add(bindingName);

		CSVBinding bindingTitle = new CSVBinding();
		bindingTitle.setField("key");
		bindingTitle.setColumn(TITLE_COLUMN);
		bindingTitle.setCondition("!(type in ['field', 'documentation'])");
		bindings.add(bindingTitle);

		CSVBinding bindingTranslation = new CSVBinding();
		bindingTranslation.setField("translation");
		bindingTranslation.setColumn(TRANSLATION_COLUMN);
		bindings.add(bindingTranslation);

		CSVBinding bindingType = new CSVBinding();
		bindingType.setField("type");
		bindingType.setColumn(TYPE_COLUMN);
		bindings.add(bindingType);

		CSVBinding bindingDomain = new CSVBinding();
		bindingDomain.setField("domain");
		bindingDomain.setColumn(DOMAIN_COLUMN);
		bindingDomain.setCondition("!(type in ['menu', 'actionMenu', 'tree', 'chart', 'search', 'action', 'select'])");
		bindings.add(bindingDomain);

		input.setBindings(bindings);
		csvConfig.setInputs(Lists.newArrayList(input));

		return csvConfig;
	}

}
