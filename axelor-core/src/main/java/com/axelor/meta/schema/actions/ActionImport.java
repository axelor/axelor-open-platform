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
package com.axelor.meta.schema.actions;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import com.axelor.data.ImportException;
import com.axelor.data.ImportTask;
import com.axelor.data.Listener;
import com.axelor.data.xml.XMLImporter;
import com.axelor.db.Model;
import com.axelor.meta.ActionHandler;
import com.axelor.meta.MetaStore;
import com.google.common.base.Objects;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

@XmlType
public class ActionImport extends Action {

	@XmlAttribute
	private String config;

	@XmlElement(name = "import")
	private List<Import> imports;

	public String getConfig() {
		return config;
	}

	public List<Import> getImports() {
		return imports;
	}

	private List<Model> doImport(XMLImporter importer, final String fileName, Object data) {

		if (!(data instanceof String)) {
			log.debug("stream type not supported: " + data.getClass());
			return null;
		}

		log.info("action-import: " + fileName);

		final StringReader reader = new StringReader((String) data);
		final HashMultimap<String, Reader> mapping = HashMultimap.create();
		final List<Model> records = Lists.newArrayList();

		mapping.put(fileName, reader);

		importer.addListener(new Listener() {
			@Override
			public void imported(Model bean) {
				log.info("action-import (record): {}(id={})",
						bean.getClass().getSimpleName(),
						bean.getId());
				records.add(bean);
			}

			@Override
			public void imported(Integer total, Integer success) {
				// TODO Auto-generated method stub

			}

			@Override
			public void handle(Model bean, Exception e) {
				// TODO Auto-generated method stub

			}
		});

		importer.runTask(new ImportTask() {

			@Override
			public void configure() throws IOException {
				input(fileName, reader);
			}

			@Override
			public boolean handle(ImportException e) {
				log.error("error:" + e);
				e.printStackTrace();
				return true;
			}
		});

		log.info("action-import (count): " + records.size());
		return records;
	}

	@Override
	public Object evaluate(ActionHandler handler) {
		Map<String, Object> result = Maps.newHashMap();

		Object configName = handler.evaluate(config);
		if(configName == null) {
			log.debug("No such config file: " + config);
			return result;
		}

		log.info("action-import (config): " + configName.toString());
		XMLImporter importer = new XMLImporter(handler.getInjector(), configName.toString());
		importer.setContext(handler.getContext());

		int count = 0;
		for(Import stream : getImports()) {
			log.info("action-import (stream, provider): " + stream.getFile() + ", " + stream.getProvider());
			Action action = MetaStore.getAction(stream.getProvider());
			if (action == null) {
				log.debug("No such action: " + stream.getProvider());
				continue;
			}

			List<Model> records = Lists.newArrayList();
			Object data = action.evaluate(handler);

			if (data instanceof Collection) {
				for(Object item : (Collection<?>) data) {
					if (item instanceof String) {
						log.info("action-import (xml stream)");
						List<Model> imported = doImport(importer, stream.getFile(), item);
						if (imported != null) {
							records.addAll(imported);
						}
					}
				}
			} else {
				log.info("action-import (object stream)");
				List<Model> imported = doImport(importer, stream.getFile(), data);
				if (imported != null) {
					records.addAll(imported);
				}
			}
			count += records.size();
			result.put(stream.name == null ? stream.getFile() : stream.name, records);
		}
		log.info("action-import (total): " + count);
		return result;
	}

	@Override
	public Object wrap(ActionHandler handler) {
		Object records = evaluate(handler);
		return ImmutableMap.of("values", records);
	}

	@Override
	public String toString() {
		return Objects.toStringHelper(getClass()).add("name", getName()).toString();
	}

	@XmlType
	public static class Import {

		@XmlAttribute
		private String file;

		@XmlAttribute
		private String provider;

		@XmlAttribute
		private String name;

		public String getFile() {
			return file;
		}

		public String getProvider() {
			return provider;
		}

		public String getName() {
			return name;
		}

		@Override
		public String toString() {
			return Objects.toStringHelper(getClass())
					.add("file", file)
					.add("provider", provider)
					.toString();
		}
	}
}
