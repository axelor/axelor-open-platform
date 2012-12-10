package com.axelor.data.xml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
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
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.inject.Injector;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.StaxDriver;
import com.thoughtworks.xstream.mapper.MapperWrapper;

/**
 * XML data importer.
 * <br>
 * <br>
 * This class also provides {@link #runTask(ImportTask)} method to import data programatically.
 * <br>
 * <br>
 * For example:
 * <pre> 
 * XMLImporter importer = new XMLImporter(injector, &quot;path/to/xml-config.xml&quot;);
 * 
 * importer.runTask(new ImportTask(){
 * 	
 * 	protected void configure() throws IOException {
 * 		input(&quot;contacts.xml&quot;, new File(&quot;data/xml/contacts.xml&quot;));
 * 		input(&quot;contacts.xml&quot;, new File(&quot;data/xml/contacts2.xml&quot;));
 * 	}
 * 
 * 	protected boolean handle(ImportException e) {
 * 		System.err.println("Import error: " + e);
 * 		return true;
 * 	}
 * }
 * </pre>
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
	
	/**
	 * Import task configures input sources and provides error handler.
	 *
	 */
	public static abstract class ImportTask {
		
		private Multimap<String, Reader> readers =  ArrayListMultimap.create();

		/**
		 * Configure the input sources using the various {@code input} methods.
		 * 
		 * @throws IOException
		 * @see {@link #input(String, File)},
		 *      {@link #input(String, File, Charset)}
		 *      {@link #input(String, InputStream)},
		 *      {@link #input(String, InputStream, Charset)},
		 *      {@link #input(String, Reader)}
		 */
		protected abstract void configure() throws IOException;
		
		/**
		 * Provide import error handler.
		 * 
		 * @return return {@code true} to continue else terminate the task
		 *         immediately.
		 */
		protected boolean handle(ImportException exception) {
			return false;
		}

		/**
		 * Provide the input source.
		 * 
		 * @param inputName
		 *            the input name
		 * @param source
		 *            the input source
		 * @throws FileNotFoundException
		 */
		protected void input(String inputName, File source) throws FileNotFoundException {
			input(inputName, source, Charset.defaultCharset());
		}
		
		/**
		 * Provide the input source.
		 * 
		 * @param inputName
		 *            the input name
		 * @param source
		 *            the input source
		 * @param charset
		 *            the source encoding
		 * @throws FileNotFoundException
		 */
		protected void input(String inputName, File source, Charset charset) throws FileNotFoundException {
			input(inputName, new FileInputStream(source), charset);
		}

		/**
		 * Provide the input source.
		 * 
		 * @param inputName
		 *            the input name
		 * @param source
		 *            the input source
		 */
		protected void input(String inputName, InputStream source) {
			input(inputName, source, Charset.defaultCharset());
		}
		
		/**
		 * Provide the input source.
		 * 
		 * @param inputName
		 *            the input name
		 * @param source
		 *            the input source
		 * @param charset
		 *            the source encoding
		 */
		protected void input(String inputName, InputStream source, Charset charset) {
			input(inputName, new InputStreamReader(source, charset));
		}

		/**
		 * Provide the input source.
		 * 
		 * @param inputName
		 *            the input name
		 * @param reader
		 *            the input source
		 */
		protected void input(String inputName, Reader reader) {
			readers.put(inputName, reader);
		}
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
	
	public void runTask(ImportTask task) {
		try {
			task.configure();
			for (XMLInput input : config.getInputs()) {
				for(Reader reader : task.readers.get(input.getFileName())) {
					try {
						process(input, reader);
					} catch(ImportException e) {
						if (!task.handle(e)) {
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

	/**
	 * Process the given key -> reader multi-mapping to import data from some streams.
	 * 
	 * @param readers multi-value mapping of filename -> reader
	 * @throws ImportException
	 * 
	 * @see {@link #input(String, File)}
	 * @see {@link #input(String, InputStream)}
	 * @see {@link #input(String, Reader)}
	 * @see {@link #consume()}
	 */
	@Deprecated
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
	private void process(XMLInput input, File file) throws ImportException {
		try {
			LOG.info("Importing: {}", file.getName());
			this.process(input, new FileReader(file));
		} catch (IOException e) {
			throw new ImportException(e);
		}
	}
	
	private void process(XMLInput input, Reader reader) throws ImportException {

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