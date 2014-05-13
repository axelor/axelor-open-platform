/**
 * Axelor Business Solutions
 *
 * Copyright (C) 2012-2014 Axelor (<http://axelor.com>).
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;

import com.axelor.common.Inflector;
import com.axelor.common.StringUtils;
import com.google.common.collect.Sets;
import com.google.common.io.CharStreams;

public class I18nExtractor {
	
	private static Logger log = LoggerFactory.getLogger(I18nExtractor.class);

	private static final DocumentBuilderFactory DOC_FACTORY = DocumentBuilderFactory.newInstance();
	private static final XPathFactory XPATH_FACTORY = XPathFactory.newInstance();
	
	private static final Pattern PATTERN_XML = Pattern.compile("/(domains|objects|views)/");
	private static final Pattern PATTERN_I18N = Pattern.compile("((I18n.get\\s*\\()|(/\\*\\$\\$\\(\\*/))\\s*");

	private static final Set<String> ROOT_NODES = Sets.newHashSet(
			"domain-models", "object-views");
	
	private static final Set<String> FIELD_NODES = Sets.newHashSet(
			"string", "boolean", "integer", "long", "decimal", "date", "time", "datetime", "binary",
			"one-to-one", "many-to-one", "one-to-many", "many-to-many");
	
	private static abstract class I18nTextVisitor extends SimpleFileVisitor<Path> {
		
		private Path base;
		
		public I18nTextVisitor(Path base) {
			this.base = base;
		}
		
		@Override
		public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
			String name = file.getFileName().toString();
			try {
				if (name.endsWith(".xml"))processXml(file);
				if (name.endsWith(".java"))processJava(file);
				if (name.endsWith(".groovy"))processJava(file);
			} catch (Exception e) {
				log.error(e.getMessage());
			}
			return FileVisitResult.CONTINUE;
		}
		
		protected abstract void accept(String text);
		
		private void processXml(Path file) throws Exception {
			
			String path = file.toString();
			Matcher match = PATTERN_XML.matcher(path);
			
			if (!match.find()) {
				return;
			}
			
			DocumentBuilder builder = DOC_FACTORY.newDocumentBuilder();
			Document doc = builder.parse(file.toFile());
			
			if (!ROOT_NODES.contains(doc.getDocumentElement().getNodeName())) {
				return;
			}
			
			log.debug("processing: {}", base.getParent().relativize(file));
			
			XPath xpath = XPATH_FACTORY.newXPath();
			XPathExpression xpr = xpath.compile("//*");
			
			NodeList nodes = (NodeList) xpr.evaluate(doc, XPathConstants.NODESET);
			
			for (int i = 0; i < nodes.getLength(); i++) {
				Element node = (Element) nodes.item(i);
				String title = node.getAttribute("title");
				if (StringUtils.isBlank(title) && FIELD_NODES.contains(node.getNodeName())) {
					title = node.getAttribute("name");
					title = Inflector.getInstance().humanize(title);
				}
				accept(title);
				accept(node.getAttribute("help"));
				accept(node.getAttribute("prompt"));
				accept(node.getAttribute("placeholder"));
			}
		}

		private int consume(String source) {
			
			char first = source.charAt(0);
			if (first != '"') {
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
				if (!isString && next == '"') {
					isString = true;
					continue;
				}
				if (next == '"') {
					isString = source.charAt(i - 2) == '\\';
				}
				if (isString) {
					sb.append(next);
				} else if (next == ',') { // next argument
					accept(sb.toString().trim());
					sb = new StringBuilder();
				}
			}
			accept(sb.toString().trim());
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
				int end = consume(source.substring(matcher.end()));
				matcher.region(matcher.end() + end, source.length());
			}
		}
		
		public void walk() {
			try {
				Files.walkFileTree(base, this);
			} catch (IOException e) {
			}
		}
	}
	
	public void extract(Path base) {
		
		final Path srcPath = base.resolve("src");
		if (!Files.exists(srcPath)) {
			return;
		}

		log.info("extracting: {}", "translatable strings...");
		
		final Set<String> items = new HashSet<>();
		final I18nTextVisitor visitor = new I18nTextVisitor(srcPath) {
			
			@Override
			protected void accept(String text) {
				if (StringUtils.isBlank(text)) return;
				items.add(text);
			}
		};
		
		visitor.walk();
		
		List<String> keys = new ArrayList<>(items);
		List<String[]> values = new ArrayList<>();
		
		Collections.sort(keys);
		
		for (String key : keys) {
			String[] line = { key, "", "" };
			values.add(line);
		}
		
		try {
			updateAll(base, values);
		} catch (IOException e) {
		}
	}
	
	private void updateAll(final Path base, final List<String[]> lines) throws IOException {

		// first save the template
		Path template = base.resolve("src/main/resources/i18n/messages.csv");
		
		log.info("generating: " + base.relativize(template));
		
		save(template, lines);

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
		try(final CSVReader reader = new CSVReader(new FileReader(file.toFile()))) {
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
			csv.writeNext(new String[] { "key", "message", "comment" });
			for (String[] line : values) {
				csv.writeNext(line);
			}
		} catch (IOException e) {
			throw e;
		}
	}
}
