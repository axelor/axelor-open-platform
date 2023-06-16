/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2023 Axelor (<http://axelor.com>).
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.axelor.tools.i18n;

import com.axelor.common.Inflector;
import com.axelor.common.StringUtils;
import com.axelor.common.XMLUtils;
import com.axelor.common.csv.CSVFile;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.common.io.CharStreams;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.parsers.SAXParser;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class I18nExtractor {

  private static Logger log = LoggerFactory.getLogger(I18nExtractor.class);

  private static final Pattern PATTERN_XML =
      Pattern.compile("\\" + File.separator + "(domains|objects|views)\\" + File.separator);
  private static final Pattern PATTERN_I18N =
      Pattern.compile(
          "((_t\\s*\\()|([Ii]18n.get\\s*\\()|(T.apply\\s*\\()|(/\\*\\$\\$\\(\\*/))\\s*");
  private static final Pattern PATTERN_HTML =
      Pattern.compile("((\\{\\{(.*?)\\|\\s*t\\s*\\}\\})|(x-translate.*?\\>(.*?)\\<))");
  private static final Pattern PATTERN_EXCLUDE =
      Pattern.compile("(\\.min\\.)|(main.webapp.lib)|(js.i18n)|(\\.test\\.)");

  private static final Set<String> JS_FILE_EXTENSIONS = Set.of(".js", ".jsx", ".ts", ".tsx");

  private static final Set<String> VIEW_TYPES =
      Sets.newHashSet(
          "form", "grid", "tree", "calendar", "kanban", "cards", "gantt", "chart", "custom");

  private static final Set<String> FIELD_NODES =
      Sets.newHashSet(
          "string",
          "boolean",
          "integer",
          "long",
          "decimal",
          "date",
          "time",
          "datetime",
          "binary",
          "enum",
          "one-to-one",
          "many-to-one",
          "one-to-many",
          "many-to-many");

  private static final Set<String> TEXT_ATTRS =
      Sets.newHashSet(
          "tag",
          "prompt",
          "placeholder",
          "x-true-text",
          "x-false-text",
          "confirm-btn-title",
          "cancel-btn-title");

  private static final Set<String> TEXT_NODES =
      Sets.newHashSet("option", "message", "static", "help");

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

  private abstract static class I18nTextVisitor extends SimpleFileVisitor<Path> {

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

        if (JS_FILE_EXTENSIONS.stream().anyMatch(name::endsWith)
            && !PATTERN_EXCLUDE.matcher(file.toString()).find()) {
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

      log.debug("processing XML for: {}", base.getParent().relativize(file));

      final SAXParser parser = XMLUtils.createSAXParser();
      final DefaultHandler handler =
          new DefaultHandler() {

            private Locator locator;
            private boolean readText = false;
            private StringBuilder readTextLines = new StringBuilder();

            @Override
            public void setDocumentLocator(Locator locator) {
              this.locator = locator;
            }

            @Override
            public void startElement(
                String uri, String localName, String qName, Attributes attributes)
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

              if (StringUtils.isBlank(title) && "attribute".equals(qName) && "title".equals(name)) {
                title = attributes.getValue("value");
              }

              if (name != null
                  && StringUtils.isBlank(title)
                  && (FIELD_NODES.contains(qName)
                      || "item".equals(qName)
                      || ("tree".equals(viewType) && "column".equals(qName)))) {
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

              if (TEXT_NODES.contains(qName)) {
                readText = true;
              }
            }

            @Override
            public void endElement(String uri, String localName, String qName) throws SAXException {
              if (TEXT_NODES.contains(qName)) {
                String text = StringUtils.stripIndent(readTextLines.toString());
                accept(new I18nItem(text, file, locator.getLineNumber()));
                readText = false;
                readTextLines.setLength(0);
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
                readTextLines.append(new String(ch, start, length));
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

      log.debug("processing HTML for: {}", base.getParent().relativize(file));

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
        } else if (text.startsWith("\\\"") && text.endsWith("\\\"")) {
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
          accept(new I18nItem(sb.toString(), file, line));
          sb = new StringBuilder();
        }
      }
      accept(new I18nItem(sb.toString(), file, line));
      return i;
    }

    private void processJava(Path file) throws Exception {

      String source = null;
      try (Reader reader = new FileReader(file.toFile())) {
        source = CharStreams.toString(reader);
      } catch (IOException e) {
        throw e;
      }

      log.debug("processing Java for: {}", base.getParent().relativize(file));

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
    Path src = base.resolve(Paths.get("src", "main"));
    Path dest = src.resolve("resources");
    extract(src, dest, update, withContext);
  }

  public void extract(Path srcPath, Path destPath, boolean update, boolean withContext) {
    if (Files.notExists(srcPath)) {
      return;
    }

    log.info("extracting: {}", "translatable strings...");

    final Multimap<String, String> items = HashMultimap.create();

    final I18nTextVisitor visitor =
        new I18nTextVisitor(srcPath) {

          @Override
          protected void accept(I18nItem item) {
            if (StringUtils.isBlank(item.text)) return;
            String location = null;
            if (item.file != null) {
              location = "" + srcPath.relativize(item.file) + ":" + item.line;
            }
            if (item.text.length() != item.text.trim().length()) {
              log.warn(
                  "Remove leading/trailing white spaces from '{}', of following text: '{}'",
                  location,
                  item.text);
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
      String[] line = {key, "", "", context};
      values.add(line);
    }

    try {
      update(destPath, values, update);
    } catch (IOException e) {
    }
  }

  private void update(Path destPath, final List<String[]> lines, boolean all) throws IOException {

    // first save the template
    Path template = destPath.resolve(Paths.get("i18n", "messages.csv"));

    log.info("generating: " + template);

    save(template, lines);

    if (!all) {
      return;
    }

    // then update all languages
    Files.walkFileTree(
        template.getParent(),
        new SimpleFileVisitor<Path>() {
          @Override
          public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
              throws IOException {
            final String name = file.getFileName().toString();
            final Pattern pattern = Pattern.compile("messages_([a-zA-Z_]+)\\.csv");
            final Matcher matcher = pattern.matcher(name);
            if (matcher.matches()) {
              log.info("updating: " + file);
              update(file, lines);
            }
            return FileVisitResult.CONTINUE;
          }
        });

    // generate initial templates for some languages
    String[] langs = {"en", "fr"};
    for (String lang : langs) {
      Path target = template.resolveSibling("messages_" + lang + ".csv");
      if (!Files.exists(target)) {
        log.info("generating: " + target);
        Files.copy(template, target);
      }
    }
  }

  private void update(Path file, List<String[]> lines) throws IOException {
    if (!file.toFile().exists()) return;

    final Map<String, Map<String, String>> values = new HashMap<>();
    final CSVFile csv = CSVFile.DEFAULT.withFirstRecordAsHeader();

    try (CSVParser csvParser = csv.parse(file.toFile())) {
      if (!csvParser.getHeaderNames().contains("key")) {
        throw new IOException("Invalid language file: " + file);
      }
      for (CSVRecord record : csvParser) {
        if (CSVFile.notEmpty(record)) {
          values.put(record.get("key"), record.toMap());
        }
      }
    } catch (IOException e) {
      throw e;
    }

    final List<String[]> myLines = new ArrayList<>();
    for (String[] line : lines) {
      Map<String, String> current = values.get(line[0]);
      String[] copy = line;
      if (current != null) {
        copy = Arrays.copyOf(line, line.length);
        copy[1] = current.get("message");
        copy[2] = current.get("comment");
      }
      myLines.add(copy);
    }

    save(file, myLines);
  }

  private void save(Path file, List<String[]> values) throws IOException {
    Files.createDirectories(file.getParent());
    try (CSVPrinter printer = CSVFile.DEFAULT.withQuoteAll().write(file.toFile())) {
      printer.printRecord("key", "message", "comment", "context");
      for (String[] line : values) {
        for (int i = 0; i < line.length; i++) {
          if (StringUtils.isBlank(line[i])) {
            line[i] = null;
          }
        }
      }
      printer.printRecords(values);
    } catch (IOException e) {
      throw e;
    }
  }
}
