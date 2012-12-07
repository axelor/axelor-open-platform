package com.axelor.data.xml;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.inject.Named;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.axelor.data.ImportException;
import com.axelor.data.Importer;
import com.axelor.data.adapter.BooleanAdapter;
import com.axelor.data.adapter.JodaAdapter;
import com.axelor.data.adapter.NumberAdapter;
import com.axelor.db.JPA;
import com.axelor.db.Model;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.inject.Injector;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.StaxDriver;
import com.thoughtworks.xstream.mapper.MapperWrapper;

/**
 * XML data importer.
 *
 */
public class XMLImporter implements Importer {

	private Logger LOG = LoggerFactory.getLogger(getClass());

	private Injector injector;
	
	private File dataDir;
	
	private XMLConfig config;
	
	private Map<String, Object> context;

	private List<Listener> listeners = Lists.newArrayList();
	
	private static XMLAdapter[] defaultAdapters = {
		new XMLAdapter("LocalDate", JodaAdapter.class, "type", "LocalDate", "format", "dd/MM/yyyy"),
		new XMLAdapter("LocalTime", JodaAdapter.class, "type", "LocalTime", "format", "HH:mm"),
		new XMLAdapter("LocalDateTime", JodaAdapter.class, "type", "LocalDateTime", "format", "dd/MM/yyyy HH:mm"),
		new XMLAdapter("DateTime", JodaAdapter.class, "type", "DateTime", "format", "dd/MM/yyyy HH:mm"),
		new XMLAdapter("Boolean", BooleanAdapter.class, "falsePattern", "(0|f|n|false|no)"),
		new XMLAdapter("Number", NumberAdapter.class, "decimalSeparator", ".", "thousandSeparator", ",")
	};

	public static interface Listener {
		
		void imported(Model bean);
	}

	@Inject
	public XMLImporter(Injector injector,
			@Named("axelor.data.config") String configFile,
			@Named("axelor.data.dir") String dataDir) {

		Preconditions.checkNotNull(injector);
		Preconditions.checkNotNull(configFile);

		File _file = new File(configFile);

		Preconditions.checkArgument(_file.isFile(), "No such file: " + configFile);
		
		if (dataDir != null) {
			File _data = new File(dataDir);
			Preconditions.checkArgument(_data.isDirectory());
			this.dataDir = _data;
		}

		this.injector = injector;
		this.config = XMLConfig.parse(_file);
	}

	public XMLImporter(Injector injector, String configFile) {
		this(injector, configFile, null);
	}
	
	public void setContext(Map<String, Object> context) {
		this.context = context;
	}

	private List<File> getFiles(String... names) {
		List<File> all = Lists.newArrayList();
		for (String name : names)
			all.add(dataDir != null ? new File(dataDir, name) : new File(name));
		return all;
	}
	
	public void addListener(Listener listener) {
		this.listeners.add(listener);
	}

	@Override
	public void run(Map<String, String[]> mappings) {
		
		if (mappings == null) {
			mappings = new HashMap<String, String[]>();
		}
		
		for (XMLInput input : config.getInputs()) {
			
			String fileName = input.getFileName();
			
			Pattern pattern = Pattern.compile("\\[([\\w.]+)\\]");
			Matcher matcher = pattern.matcher(fileName);
			
			List<File> files = matcher.matches() ?
					this.getFiles(mappings.get(matcher.group(1))) :
					this.getFiles(fileName);
			
			for(File file : files) {
				try {
					this.process(input, file);
				} catch (Exception e) {
					LOG.error("Error while importing {}.", file, e);
				}
			}
		}
	}
	
	/**
	 * Process the given key -> reader multi-mapping to import data from some streams.
	 * 
	 * @param readers multi-value mapping of filename -> reader
	 * @throws ImportException
	 */
	public void process(Multimap<String, Reader> readers) throws ImportException {
		
		Preconditions.checkNotNull(config);
		Preconditions.checkNotNull(readers);

		for (XMLInput input : config.getInputs()) {
			for(Reader reader : readers.get(input.getFileName()))
				this.process(input, reader);
		}
	}
	
	/**
	 * Process the data file with the given input binding.
	 * 
	 * @param input input binding configuration
	 * @param file data file
	 * @throws ImportException
	 */
	public void process(XMLInput input, File file) throws ImportException {
		try {
			this.process(input, new FileReader(file));
		} catch (IOException e) {
			throw new ImportException(e);
		}
	}
	
	public void process(XMLInput input, Reader reader) throws ImportException {

		XStream stream = new XStream(new StaxDriver()) {

			private String root = null;

			@Override
			@SuppressWarnings("all")
			protected MapperWrapper wrapMapper(MapperWrapper next) {
				
				return new MapperWrapper(next) {
					
					@Override
					public Class realClass(String elementName) {
						if (root == null) {
							root = elementName;
							return Document.class;
						}
						return Element.class;
					}
				};
			}
		};
		
		XMLBinder binder = new XMLBinder(input, context) {
			
			int count = 0;
			
			@Override
			protected void handle(Object bean, XMLBind binding) {
				if (bean == null) {
					return;
				}
				try {
					bean = binding.call(bean, context, injector);
					if (bean != null) {
						bean = JPA.manage((Model) bean);
						count++;
						for(Listener listener : listeners) {
							listener.imported((Model) bean);
						}
					}
				} catch (Exception e) {
					LOG.error("Unable to import object {}.", bean);
					LOG.error("With binding {}.", binding);
					LOG.error("With exception:", e);
				}

				if (count % 100 == 0) {
					JPA.flush();
					JPA.em().clear();
				}
			}
		};

		// register type adapters
		for(XMLAdapter adapter : defaultAdapters) {
			binder.registerAdapter(adapter);
		}
		for(XMLAdapter adapter : config.getAdapters()) {
			binder.registerAdapter(adapter);
		}
		for(XMLAdapter adapter : input.getAdapters()) {
			binder.registerAdapter(adapter);
		}

		stream.setMode(XStream.NO_REFERENCES);
		stream.registerConverter(new ElementConverter(binder));
		
		JPA.em().getTransaction().begin();
		try {
			stream.fromXML(reader);
			if (JPA.em().getTransaction().isActive())
				JPA.em().getTransaction().commit();
		} catch (Exception e) {
			if (JPA.em().getTransaction().isActive())
				JPA.em().getTransaction().rollback();
			throw new ImportException(e);
		}
	}
}