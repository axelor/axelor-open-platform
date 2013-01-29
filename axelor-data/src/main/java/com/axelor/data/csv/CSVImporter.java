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
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Injector;

public class CSVImporter implements Importer {

	private Logger LOG = LoggerFactory.getLogger(getClass());
	
	private File dataDir;
	
	private Injector injector;
	
	private CSVConfig config;
	
	private List<Listener> listeners = Lists.newArrayList();
	
	private Map<String, Object> context;
	
	public void addListener(Listener listener) {
		this.listeners.add(listener);
	}
	
	public void setContext(Map<String, Object> context) {
		this.context = context;
	}
	
	public CSVImporter(Injector injector, String configFile) {
		this(injector, configFile, null);
	}
	
	@Inject
	public CSVImporter(Injector injector,
			@Named("axelor.data.config") String config,
			@Named("axelor.data.dir") String dataDir) {
		
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
	}
	
	private List<File> getFiles(String... names) {
		List<File> all = Lists.newArrayList();
		for (String name : names)
			all.add(new File(dataDir, name));
		return all;
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
		
		if (LOG.isInfoEnabled()) {
			LOG.info("Importing {} from {}", beanName, csvInput.getFileName());
		}
		
		BufferedReader streamReader = new BufferedReader(reader);
		CSVReader csvReader = new CSVReader(streamReader, csvInput.getSeparator());
		
		String[] fields = csvReader.readNext();
		Class<?> beanClass = Class.forName(beanName);
		
		if (LOG.isDebugEnabled())
			LOG.debug("Header {}", Arrays.asList(fields));
		
		CSVBinder binder = new CSVBinder(beanClass, fields, csvInput);
		String[] values = null;
		int count = 0;
		
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
				
				if (isEmpty(values))
					continue;
				
				if (LOG.isDebugEnabled()) {
					LOG.debug("Record {}", Arrays.asList(values));
				}
				
				try {
					
					Map<String, Object> ctx = Maps.newHashMap(context);
					Object bean = binder.bind(values, ctx);
					
					if (LOG.isTraceEnabled())
						LOG.trace("bean created: {}", bean);
					
					bean = csvInput.call(bean, ctx, injector);
					
					if (bean != null) {
						JPA.manage((Model) bean);
						for(Listener listener : listeners) {
							listener.imported((Model) bean);
						}
					}
					
					LOG.debug("bean saved: {}", bean);
					
				} catch (Exception e) {
					if (LOG.isErrorEnabled()) {
						LOG.error("Error while importing {}.", csvInput.getFileName());
						LOG.error("Unable to import record: {}", Arrays.asList(values));
						LOG.error("With following exception:", e);
					}
					continue;
				}
				
				if (++count % 20 == 0) {
					JPA.flush();
					JPA.em().clear();
				}
			}
			if (JPA.em().getTransaction().isActive())
				JPA.em().getTransaction().commit();
		} catch (Exception e) {
			if (JPA.em().getTransaction().isActive())
				JPA.em().getTransaction().rollback();
			if (LOG.isErrorEnabled())
				LOG.error("Error while importing {}.", csvInput.getFileName());
				LOG.error("Unable to import data.");
				LOG.error("With following exception:", e);
		} finally {
			for(Listener listener : listeners) {
				listener.imported(count);
			}
			csvReader.close();
		}
		
		
	}
}
