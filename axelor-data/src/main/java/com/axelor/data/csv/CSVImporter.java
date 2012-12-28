package com.axelor.data.csv;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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

import com.axelor.data.Importer;
import com.axelor.db.JPA;
import com.axelor.db.Model;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Injector;

public class CSVImporter implements Importer {

	private Logger LOG = LoggerFactory.getLogger(getClass());
	
	private File cfgFile;
	
	private File dataDir;
	
	private Injector injector;
	
	@Inject
	public CSVImporter(Injector injector,
			@Named("axelor.data.config") String config,
			@Named("axelor.data.dir") String dataDir) {
		
		File _file = new File(config);
		File _data = new File(dataDir);
		
		Preconditions.checkNotNull(_file);
		Preconditions.checkNotNull(_data);
		Preconditions.checkArgument(_file.isFile());
		Preconditions.checkArgument(_data.isDirectory());
		
		this.cfgFile = _file;
		this.dataDir = _data;
		this.injector = injector;
	}
	
	private List<File> getFiles(String... names) {
		List<File> all = Lists.newArrayList();
		for (String name : names)
			all.add(new File(dataDir, name));
		return all;
	}
	
	@Override
	public void run(Map<String, String[]> mappings) throws IOException {
		
		CSVConfig config = CSVConfig.parse(this.cfgFile);
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
					this.process(file, input);
				} catch (IOException e) {
					if (LOG.isErrorEnabled())
						LOG.error("I/O error while accessing {}.", file);
				} catch (ClassNotFoundException e) {
					if (LOG.isErrorEnabled()) {
						LOG.error("Error while importing {}.", file);
						LOG.error("No such class found {}.", input.getTypeName());
					}
				}
			}
		}
	}

	private boolean isEmpty(String[] line) {
		if (line == null || line.length == 0)
			return true;
		if (line.length == 1 && (line[0] == null || "".equals(line[0].trim())))
			return true;
		return false;
	}
	
	private void process(File input, CSVInput csvInput) throws IOException, ClassNotFoundException {
		
		String beanName = csvInput.getTypeName();
		
		if (LOG.isInfoEnabled()) {
			LOG.info("Importing {} from {}", beanName, input);
		}
		
		InputStream is = new FileInputStream(input);
		BufferedReader streamReader = new BufferedReader(new InputStreamReader(is));
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
			csvInput.callPrepareContext(context, injector);
			
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
					}
					LOG.debug("bean saved: {}", bean);
				} catch (Exception e) {
					if (LOG.isErrorEnabled()) {
						LOG.error("Error while importing {}.", input.getName());
						LOG.error("Unable to import record: {}", Arrays.asList(values));
						LOG.error("With following exception:", e);
					}
					continue;
				}
				
				if (++count % 100 == 0) {
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
				LOG.error("Error while importing {}.", input.getName());
				LOG.error("Unable to import data.");
				LOG.error("With following exception:", e);
		}
		
		csvReader.close();
	}
}
