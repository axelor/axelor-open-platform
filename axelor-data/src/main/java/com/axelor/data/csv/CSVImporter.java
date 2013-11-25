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
package com.axelor.data.csv;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.com.bytecode.opencsv.CSVReader;

import com.axelor.data.ImportException;
import com.axelor.data.ImportTask;
import com.axelor.data.Importer;
import com.axelor.data.Listener;
import com.axelor.data.adapter.DataAdapter;
import com.axelor.db.JPA;
import com.axelor.db.Model;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Injector;

public class CSVImporter implements Importer {

	private Logger LOG = LoggerFactory.getLogger(getClass());

	private File dataDir;

	private Injector injector;

	private CSVConfig config;

	private List<Listener> listeners = Lists.newArrayList();

	private List<String[]> valuesStack = Lists.newArrayList();

	private Map<String, Object> context;

	private CSVLogger loggerManager;

	public void addListener(Listener listener) {
		this.listeners.add(listener);
	}

	public void clearListener() {
		this.listeners.clear();
	}

	public void setContext(Map<String, Object> context) {
		this.context = context;
	}

	public CSVImporter(Injector injector, String configFile) {
		this(injector, configFile, null, null);
	}

	public CSVImporter(Injector injector, String config, String dataDir){
		this(injector, config, dataDir, null);
	}

	@Inject
	public CSVImporter(Injector injector,
			@Named("axelor.data.config") String config,
			@Named("axelor.data.dir") String dataDir,
			@Nullable @Named("axelor.error.dir") String errorDir) {

		File _file = new File(config);

		Preconditions.checkNotNull(_file);
		Preconditions.checkArgument(_file.isFile());

		if (dataDir != null) {
			File _data = new File(dataDir);
			Preconditions.checkNotNull(_data);
			Preconditions.checkArgument(_data.isDirectory());
			this.dataDir = _data;
		}

		this.config = CSVConfig.parse(_file);
		this.injector = injector;
		if(!Strings.isNullOrEmpty(errorDir)) {
			this.loggerManager = new CSVLogger(this.config, errorDir);
		}
	}

	public CSVImporter(Injector injector, CSVConfig config){
		this.config = config;
		this.injector = injector;
	}

	private List<File> getFiles(String... names) {
		List<File> all = Lists.newArrayList();
		for (String name : names)
			all.add(new File(dataDir, name));
		return all;
	}

	private int getBatchSize() {
		try {
			Object val = JPA.em().getEntityManagerFactory().getProperties().get("hibernate.jdbc.batch_size");
			return Integer.parseInt(val.toString());
		} catch (Exception e) {
		}
		return DEFAULT_BATCH_SIZE;
	}

	/**
	 * Run the task from the configured readers
	 * @param task
	 */
	public void runTask(ImportTask task) {
		try {
			if (task.readers.isEmpty()) {
				task.configure();
			}
			for (CSVInput input : config.getInputs()) {
				for(Reader reader : task.readers.get(input.getFileName())) {
					try {
						this.process(input, reader);
					} catch (IOException e) {
						if (LOG.isErrorEnabled()){
							LOG.error("I/O error while accessing {}.", input.getFileName());
						}
						if (!task.handle(e)) {
							break;
						}
					} catch (ClassNotFoundException e) {
						if (LOG.isErrorEnabled()) {
							LOG.error("Error while importing {}.", input.getFileName());
							LOG.error("No such class found {}.", input.getTypeName());
						}
						if (!task.handle(e)) {
							break;
						}
					} catch(Exception e){
						if (!task.handle(new ImportException(e))) {
							break;
						}
					}
				}
			}
		} catch(IOException e) {
			throw new IllegalArgumentException(e);
		} finally {
			task.readers.clear();
		}
	}

	@Override
	public void run(Map<String, String[]> mappings) throws IOException {

		if (mappings == null) {
			mappings = new HashMap<String, String[]>();
		}

		for (CSVInput input : config.getInputs()) {

			String fileName = input.getFileName();

			Pattern pattern = Pattern.compile("\\[([\\w.]+)\\]");
			Matcher matcher = pattern.matcher(fileName);

			List<File> files = matcher.matches() ?
					this.getFiles(mappings.get(matcher.group(1))) :
					this.getFiles(fileName);

			for(File file : files) {
				try {
					this.process(input, file);
				} catch (IOException e) {
					if (LOG.isErrorEnabled())
						LOG.error("Error while accessing {}.", file);
				} catch (ClassNotFoundException e) {
					if (LOG.isErrorEnabled()) {
						LOG.error("Error while importing {}.", file);
						LOG.error("No such class found {}.", input.getTypeName());
					}
				} catch (Exception e) {
					if (LOG.isErrorEnabled()) {
						LOG.error("Error while importing {}.", file);
						LOG.error("Unable to import data.");
						LOG.error("With following exception:", e);
					}
				}
			}
		}
	}

	/**
	 * Check if the String array is empty.
	 * @param line
	 * @return <code>true</code> if line is null or empty, <code>false</code> otherwise
	 */
	private boolean isEmpty(String[] line) {
		if (line == null || line.length == 0)
			return true;
		if (line.length == 1 && (line[0] == null || "".equals(line[0].trim())))
			return true;
		return false;
	}

	/**
	 * Lauch the import for the input and file.
	 * @param input
	 * @param file
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	private void process(CSVInput input, File file) throws IOException, ClassNotFoundException {
		this.process(input, new FileReader(file));
	}

	/**
	 * Lauch the import for the input and reader.
	 * @param csvInput
	 * @param reader
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	private void process(CSVInput csvInput, Reader reader) throws IOException, ClassNotFoundException {

		String beanName = csvInput.getTypeName();

		LOG.info("Importing {} from {}", beanName, csvInput.getFileName());

		BufferedReader streamReader = new BufferedReader(reader);
		CSVReader csvReader = new CSVReader(streamReader, csvInput.getSeparator());

		String[] fields = csvReader.readNext();
		Class<?> beanClass = Class.forName(beanName);
		if(loggerManager != null) {
			loggerManager.prepareInput(csvInput, fields);
		}

		LOG.debug("Header {}", Arrays.asList(fields));

		CSVBinder binder = new CSVBinder(beanClass, fields, csvInput, injector);
		String[] values = null;

		int count = 0;
		int total = 0;
		int batchSize = getBatchSize();

		JPA.em().getTransaction().begin();
		try {

			Map<String, Object> context = Maps.newHashMap();

			//Put global context
			if (this.context != null) {
				context.putAll(this.context);
			}

			csvInput.callPrepareContext(context, injector);

			// register type adapters
			for(DataAdapter adapter : defaultAdapters) {
				binder.registerAdapter(adapter);
			}
			for(DataAdapter adapter : this.config.getAdapters()) {
				binder.registerAdapter(adapter);
			}
			for(DataAdapter adapter : csvInput.getAdapters()) {
				binder.registerAdapter(adapter);
			}

			//Process for each lines
			while((values = csvReader.readNext()) != null) {

				if (isEmpty(values)) {
					continue;
				}
				LOG.trace("Record {}", Arrays.asList(values));

				Object bean = null;
				try {
					bean = this.importRow(values, binder, csvInput, context, false);
					count++;
				} catch (Exception e) {
					LOG.error("Error while importing {}.", csvInput.getFileName());
					LOG.error("Unable to import record: {}", Arrays.asList(values));
					LOG.error("With following exception:", e);

					// Recover the transaction
					if (JPA.em().getTransaction().isActive()) {
						JPA.em().getTransaction().rollback();
					}

					if (!JPA.em().getTransaction().isActive()) {
						JPA.em().getTransaction().begin();
					}

					for(Listener listener : listeners) {
						listener.handle((Model) bean, e);
					}

					//Re-parse previous records
					this.onRollback(values, binder, csvInput, context);
				}

				++total;
				if (valuesStack.size() % batchSize == 0) {
					LOG.trace("Commit {} records", valuesStack.size());

					if (JPA.em().getTransaction().isActive()) {
						JPA.em().getTransaction().commit();
						JPA.em().clear();
						valuesStack.clear();
					}
					if (!JPA.em().getTransaction().isActive()) {
						JPA.em().getTransaction().begin();
					}
				}
			}

			if (JPA.em().getTransaction().isActive()) {
				LOG.trace("Commit {} records", valuesStack.size());

				JPA.em().getTransaction().commit();
				JPA.em().clear();
			}
		} catch (Exception e) {
			if (JPA.em().getTransaction().isActive()) {
				JPA.em().getTransaction().rollback();
			}

			LOG.error("Error while importing {}.", csvInput.getFileName());
			LOG.error("Unable to import data.");
			LOG.error("With following exception:", e);
		} finally {
			for(Listener listener : listeners) {
				listener.imported(total, count);
			}

			valuesStack.clear();
			csvReader.close();
		}
	}

	/**
	 * Import the specific row.
	 * @param values
	 * @param binder
	 * @param csvInput
	 * @param context
	 * @param onRollback
	 * @return the imported object
	 * @throws Exception
	 */
	private Object importRow(String[] values, CSVBinder binder, CSVInput csvInput, Map<String, Object> context, Boolean onRollback) throws Exception {
		Object bean = null;
		Map<String, Object> ctx = Maps.newHashMap(context);

		bean = binder.bind(values, ctx);

		bean = csvInput.call(bean, ctx, injector);
		LOG.trace("bean created: {}", bean);

		if (bean != null) {
			JPA.manage((Model) bean);
			if(!onRollback) {
				valuesStack.add(values);

				for(Listener listener : listeners) {
					listener.imported((Model) bean);
				}
			}

			LOG.trace("bean saved: {}", bean);
		}

		return bean;
	}

	/**
	 * Rollback previous rows stored in valuesStack.
	 * @param values
	 * @param binder
	 * @param csvInput
	 * @param context
	 */
	private void onRollback(String[] values, CSVBinder binder, CSVInput csvInput, Map<String, Object> context) {
		if(loggerManager != null) {
			loggerManager.log(values);
		}

		for (String[] row : valuesStack) {
			LOG.debug("Recover record {}", Arrays.asList(row));

			try {
				this.importRow(row, binder, csvInput, context, true);

				if (JPA.em().getTransaction().isActive()) {
					JPA.em().getTransaction().commit();
				}
			} catch (Exception e) {
				if (JPA.em().getTransaction().isActive()) {
					JPA.em().getTransaction().rollback();
				}
			} finally {
				if (!JPA.em().getTransaction().isActive()) {
					JPA.em().getTransaction().begin();
				}
			}
		}

		valuesStack.clear();
	}
}
