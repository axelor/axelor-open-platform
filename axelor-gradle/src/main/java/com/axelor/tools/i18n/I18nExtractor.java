/**
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2016 Axelor (<http://axelor.com>).
 *
 * This program is free software: you can redistribute it and/or  modify
 * it under the terms of the GNU Affero General Public License, version 3,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.axelor.tools.i18n;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.axelor.common.Inflector;
import com.axelor.common.StringUtils;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.common.io.CharStreams;

import au.com.bytecode.opencsv.CSVParser;
import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;

public class I18nExtractor {
	
	private static Logger log = LoggerFactory.getLogger(I18nExtractor.class);

	private static final Pattern PATTERN_XML = Pattern.compile("/(domains|objects|views)/");
	private static final Pattern PATTERN_I18N = Pattern.compile("((_t\\s*\\()|(I18n.get\\s*\\()|(/\\*\\$\\$\\(\\*/))\\s*");
	private static final Pattern PATTERN_HTML = Pattern.compile("((\\{\\{(.*?)\\|\\s*t\\s*\\}\\})|(x-translate.*?\\>(.*?)\\<))");
	private static final Pattern PATTERN_EXCLUDE = Pattern.compile("(\\.min\\.)|(main.webapp.lib)|(js.i18n)");

	private static final Set<String> VIEW_TYPES = Sets.newHashSet(
			"form", "grid", "tree", "calendar", "kanban", "cards", "gantt", "chart", "custom");

	private static final Set<String> FIELD_NODES = Sets.newHashSet(
			"string", "boolean", "integer", "long", "decimal", "date", "time", "datetime", "binary",
			"one-to-one", "many-to-one", "one-to-many", "many-to-many");

	private static final Set<String> TEXT_ATTRS = Sets.newHashSet("tag", "prompt", "placeholder", "x-true-text",
			"x-false-text");

	private static final String[] CSV_HEADER = {"key", "message", "comment", "context" };

	private static class I18nItem {
		
		private String text;
		private Path file;
		private int line;
		
		public I18nItem(String text, Path file, int line) {
			this.text = text;
			this.file = file;
			this.line = line;
		}
	}

	private static abstract class I18nTextVisitor extends SimpleFileVisitor<Path> {
		
		private Path base;
		
		private String entityName;

		private String viewType;

		public I18nTextVisitor(Path base) {
			this.base = base;
		}
		
		@Override
		public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
			String name = file.getFileName().toString();
			try {
				if (name.endsWith(".xml")) processXml(file);
				if (name.endsWith(".html")) processHtml(file);
				if (name.endsWith(".jsp")) processHtml(file);
				if (name.endsWith(".jsp")) processJava(file);
				if (name.endsWith(".java")) processJava(file);
				if (name.endsWith(".groovy")) processJava(file);

				if (name.endsWith(".js") && !PATTERN_EXCLUDE.matcher(file.toString()).find()) {
					processJava(file);
					processHtml(file);
				}

			} catch (Exception e) {
				log.error(e.getMessage());
			}
			return FileVisitResult.CONTINUE;
		}
		
		protected abstract void accept(I18nItem item);
		
		private void processXml(final Path file) throws Exception {
			
			boolean isView = file.toString().indexOf("views") > -1;
			if (isView) {
				processHtml(file);
				processJava(file);
			}

			if (!PATTERN_XML.matcher(file.toString()).find()) {
				return;
			}
			
			if (!isView) {
				log.debug("processing: {}", base.getParent().relativize(file));
			}
			
			final SAXParserFactory factory = SAXParserFactory.newInstance();
			final SAXParser parser = factory.newSAXParser();
			final DefaultHandler handler = new DefaultHandler() {
				
				private Locator locator;
				private boolean readText = false;
				
				@Override
				public void setDocumentLocator(Locator locator) {
					this.locator = locator;
				}
				
				@Override
				public void startElement(String uri, String localName,
						String qName, Attributes attributes)
						throws SAXException {

					String name = attributes.getValue("name");
					String title = attributes.getValue("title");
					String help = attributes.getValue("help");
					String message = attributes.getValue("message");
					String error = attributes.getValue("error");
					
					if ("entity".equals(qName)) {
						entityName = name;
					}
					if (VIEW_TYPES.contains(qName)) {
						viewType = qName;
					}

					if (StringUtils.isBlank(title) && name != null
							&& (FIELD_NODES.contains(qName) || ("tree".equals(viewType) && "column".equals(qName)))) {
						title = Inflector.getInstance().humanize(name);
					}

					if ("true".equals(help) && entityName != null && name != null) {
						help = "help:" + entityName + "." + name;
					}

					accept(new I18nItem(title, file, locator.getLineNumber()));
					accept(new I18nItem(help, file, locator.getLineNumber()));
					accept(new I18nItem(message, file, locator.getLineNumber()));
					accept(new I18nItem(error, file, locator.getLineNumber()));

					for (String attr : TEXT_ATTRS) {
						accept(new I18nItem(attributes.getValue(attr), file, locator.getLineNumber()));
					}

					if ("option".equals(qName) || "message".equals(qName)) {
						readText = true;
					}
				}
				
				@Override
				public void endElement(String uri, String localName, String qName) throws SAXException {
					if ("option".equals(qName) || "message".equals(qName)) {
						readText = false;
					}
					if ("entity".equals(qName)) {
						entityName = null;
					}
					if (VIEW_TYPES.contains(qName)) {
						viewType = null;
					}
				}
				
				@Override
				public void characters(char[] ch, int start, int length) throws SAXException {
					if (readText) {
						String text = new String(ch, start, length);
						I18nItem item = new I18nItem(text, file, locator.getLineNumber());
						accept(item);
					}
				}
			};
			
			parser.parse(file.toFile(), handler);
		}

		private void processHtml(Path file) throws Exception {

			String source = null;
			try (Reader reader = new FileReader(file.toFile())) {
				source = CharStreams.toString(reader);
			} catch (IOException e) {
				throw e;
			}

			log.debug("processing: {}", base.getParent().relativize(file));

			Matcher matcher = PATTERN_HTML.matcher(source);
			while (matcher.find()) {
				int line = getLine(source, matcher.end());
				String text = matcher.group(3);
				if (text == null) {
					text = matcher.group(5);
				}
				text = text.trim();
				if (text.startsWith("\\'") && text.endsWith("\\'")) {
					text = text.substring(2, text.length() - 2);
				} else if(text.startsWith("\\\"") && text.endsWith("\\\"")) {
					text = text.substring(2, text.length() - 2);
				}
				consumePlain(text, file, line);
				matcher.region(matcher.end(), source.length());
			}
		}

		private int consumePlain(String text, Path file, int line) {
			String str = text.trim();
			if (str.indexOf('\'') == 0 || str.indexOf('"') == 0) {
				str = str.substring(1, str.length() - 1);
			}
			accept(new I18nItem(str, file, line));
			return text.length();
		}

		private int consume(String source, Path file, int line) {
			
			char first = source.charAt(0);
			if (first != '"' && first != '\'') {
				return 0;
			}

			StringBuilder sb = new StringBuilder();
			
			int i = 1;
			boolean isString = true;
			
			while (true) {
				char next = source.charAt(i++);
				if (!isString && next == ')') {
					break;
				}
				if (!isString && next == first) {
					isString = true;
					continue;
				}
				if (next == first) {
					isString = source.charAt(i - 2) == '\\';
				}
				if (isString) {
					sb.append(next);
				} else if (next == ',') { // next argument
					accept(new I18nItem(sb.toString().trim(), file, line));
					sb = new StringBuilder();
				}
			}
			accept(new I18nItem(sb.toString().trim(), file, line));
			return i;
		}
		
		private void processJava(Path file) throws Exception {
			
			String source = null;
			try (Reader reader = new FileReader(file.toFile())) {
				source = CharStreams.toString(reader);
			} catch (IOException e) {
				throw e;
			}
			
			log.debug("processing: {}", base.getParent().relativize(file));
			
			Matcher matcher = PATTERN_I18N.matcher(source);
			while (matcher.find()) {
				int line = getLine(source, matcher.end());
				int end = consume(source.substring(matcher.end()), file, line);
				matcher.region(matcher.end() + end, source.length());
			}
		}
		
		private int getLine(String source, int index) {
			String sub = source.substring(0, index);
			return Splitter.on('\n').splitToList(sub).size();
		}
		
		public void walk() {
			try {
				Files.walkFileTree(base, this);
			} catch (IOException e) {
			}
		}
	}
	
	public void extract(final Path base, boolean update, boolean withContext) {
		
		final Path srcPath = base.resolve("src/main");
		if (!Files.exists(srcPath)) {
			return;
		}

		log.info("extracting: {}", "translatable strings...");
		
		final Multimap<String, String> items =  HashMultimap.create();
		
		final I18nTextVisitor visitor = new I18nTextVisitor(srcPath) {
			
			@Override
			protected void accept(I18nItem item) {
				if (StringUtils.isBlank(item.text)) return;
				String location = null;
				if (item.file != null) {
					location = "" + srcPath.relativize(item.file) + ":" + item.line;
				}
				items.put(item.text.trim(), location);
			}
		};
		
		visitor.walk();

		// don't generate empty templates
		if (items.isEmpty()) {
			return;
		}
		
		List<String> keys = new ArrayList<>(items.keySet());
		List<String[]> values = new ArrayList<>();
		
		Collections.sort(keys);

		for (String key : keys) {
			String context = "";
			if (withContext) {
				List<String> locations = new ArrayList<>(items.get(key));
				Collections.sort(locations);
				context = Joiner.on('\n').join(locations).trim();
			}
			String[] line = { key, "", "", context };
			values.add(line);
		}
		
		try {
			update(base, values, update);
		} catch (IOException e) {
		}
	}

	private void update(final Path base, final List<String[]> lines, boolean all) throws IOException {

		// first save the template
		Path template = base.resolve("src/main/resources/i18n/messages.csv");
		
		log.info("generating: " + base.relativize(template));
		
		save(template, lines);

		if (!all) {
			return;
		}

		// then update all languages
		Files.walkFileTree(template.getParent(), new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				final String name = file.getFileName().toString();
				final Pattern pattern = Pattern.compile("messages_([a-zA-Z_]+)\\.csv");
				final Matcher matcher = pattern.matcher(name);
				if (matcher.matches()) {
					log.info("updating: " + base.relativize(file));
					update(file, lines);
				}
				return FileVisitResult.CONTINUE;
			}
		});
		
		// generate initial templates for some languages
		String[] langs = { "en", "fr" };
		for (String lang : langs) {
			Path target = template.resolveSibling("messages_" + lang + ".csv");
			if (!Files.exists(target)) {
				log.info("generating: " + base.relativize(target));
				Files.copy(template, target);
			}
		}
	}
	
	private boolean isEmpty(String[] items) {
		if (items == null || items.length == 0)
			return true;
		if (items.length == 1 && (items[0] == null || "".equals(items[0].trim())))
			return true;
		return false;
	}
	
	private void update(Path file, List<String[]> lines) throws IOException {
		if (!file.toFile().exists()) return;
		final Map<String, Map<String, String>> values = new HashMap<>();
		try(final CSVReader reader = new CSVReader(new FileReader(file.toFile()),
				CSVParser.DEFAULT_SEPARATOR,
				CSVParser.DEFAULT_QUOTE_CHARACTER, '\0')) {
			String[] headers = reader.readNext();
			if (headers.length < 2) {
				throw new IOException("Invalid language file: " + file);
			}
			String[] items = null;
			while((items = reader.readNext()) != null) {
				if (items.length != headers.length || isEmpty(items)) continue;
				Map<String, String> value = new HashMap<>();
				for (int i = 0; i < headers.length; i++) {
					value.put(headers[i], items[i]);
				}
				values.put(value.get("key"), value);
			}
		} catch (IOException e) {
			throw e;
		}
		
		for (String[] line : lines) {
			Map<String, String> current = values.get(line[0]);
			if (current != null) {
				line[1] = current.get("message");
				line[2] = current.get("comment");
			}
		}
		
		save(file, lines);
	}

	private void save(Path file, List<String[]> values) throws IOException {
		Files.createDirectories(file.getParent());
		FileWriter writer = new FileWriter(file.toFile());
		try (CSVWriter csv = new CSVWriter(writer)) {
			csv.writeNext(CSV_HEADER);
			for (String[] line : values) {
				for (int i = 0; i < line.length; i++) {
					if (StringUtils.isBlank(line[i])) {
						line[i] = null;
					}
				}
				csv.writeNext(line);
			}
		} catch (IOException e) {
			throw e;
		}
	}
}
