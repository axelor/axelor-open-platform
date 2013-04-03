package com.axelor.meta.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.reflections.Reflections;
import org.reflections.scanners.ResourcesScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.vfs.Vfs.File;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.axelor.data.ImportException;
import com.axelor.data.ImportTask;
import com.axelor.data.csv.CSVBinding;
import com.axelor.data.csv.CSVConfig;
import com.axelor.data.csv.CSVImporter;
import com.axelor.data.csv.CSVInput;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.Injector;

public class MetaTranslationsService {
	
	private Logger log = LoggerFactory.getLogger(getClass());
	
	private final String CSV_INPUT_FILE_NAME =  "[file.name]";
	private final String CSV_INPUT_TYPE_NAME =  "com.axelor.meta.db.MetaTranslation";
	private final String LANGUAGE_FIELD =  "language";
	private final String CURRENT_LANGUAGE =  "current_language";
	private final String CALLABLE = "com.axelor.meta.ImportTranslations:loadTranslation";
	
	@Inject
	Injector injector;
	
	public void process(){
		
		CSVConfig config = getCSVConfig();
		
		List<File> allFileList = findResources();
		
		for (File file : filterFiles(allFileList,false)) {
			try {
				process(file, config);
			}
			catch(IOException e){
				log.error("Unable to import file: {}", file.getName());
			}
		}
		
		for (File file : filterFiles(allFileList,true)) {
			try {
				process(file, config);
			}
			catch(IOException e){
				log.error("Unable to import file: {}", file.getName());
			}
		}
		
	}
	
	private void process(final File file, CSVConfig config) throws IOException{
		final InputStream stream = file.openInputStream();
		CSVImporter importer = new CSVImporter(injector, config);
		
		//Get language name from the file name
		String languageName = "";
		Pattern pattern = Pattern.compile("(\\w+)\\.(.*?)");
		Matcher matcher = pattern.matcher(file.getName());
		if(matcher.matches()){
			languageName = matcher.group(1);
		}
		
		//Prepare context
		Map<String, Object> context = Maps.newHashMap();
		context.put(CURRENT_LANGUAGE, languageName);
		importer.setContext(context);
		
		//Run task
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
	
	private List<File> filterFiles(List<File> files, boolean matched){
		
		List<File> lists = Lists.newArrayList();
		Pattern pattern = Pattern.compile("/WEB-INF/classes/translations/");
		
		for (File file : files) {
			Matcher matcher = pattern.matcher(file.getFullPath());
			if(matcher.find() == matched){
				lists.add(file);
			}
		}
		
		return lists;
	}
	
	private List<File> findResources() {
		
		FileScanner scanner = new FileScanner();
		
		Reflections reflections = new Reflections(
				new ConfigurationBuilder()
					.setUrls(ClasspathHelper.forPackage("com.axelor"))
					.setScanners(scanner)
				);
		
		reflections.getStore().get(FileScanner.class).keySet();
		return scanner.files;
	}
	
	private static class FileScanner extends ResourcesScanner {
		
		private List<File> files = Lists.newArrayList();
		
		@Override
		public boolean acceptsInput(String file) {
			return file.startsWith("translations.") && file.endsWith(".csv");
		}
		
		@Override
		public void scan(final File file) {
			files.add(file);
			String path = file.getRelativePath();
			getStore().put(path, path);
		}
	}
	
	private CSVConfig getCSVConfig() {
		
		CSVConfig csvConfig = new CSVConfig();
		
		CSVInput input = new CSVInput();
		input.setFileName(CSV_INPUT_FILE_NAME);
		input.setTypeName(CSV_INPUT_TYPE_NAME);
		input.setCallable(CALLABLE);
		
		CSVBinding binding = new CSVBinding();
		binding.setField(LANGUAGE_FIELD);
		binding.setExpression(CURRENT_LANGUAGE);
		
		input.setBindings(Lists.newArrayList(binding));
		csvConfig.setInputs(Lists.newArrayList(input));
		
		return csvConfig;
	}
	
}
