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
package com.axelor.common.csv;

import com.axelor.common.StringUtils;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.csv.QuoteMode;
import org.apache.commons.io.input.BOMInputStream;

/**
 * This class provides api to work with csv files using {@link CSVParser} and {@link CSVPrinter}.
 */
public final class CSVFile {

  public static final CSVFile DEFAULT = new CSVFile(CSVFormat.DEFAULT);

  public static final CSVFile EXCEL = new CSVFile(CSVFormat.EXCEL);

  private final CSVFormat format;

  private CSVFile(CSVFormat format) {
    this.format = format;
  }

  public CSVFile withDelimiter(char delimiter) {
    return new CSVFile(format.builder().setDelimiter(delimiter).build());
  }

  public CSVFile withEscape(char escape) {
    return new CSVFile(format.builder().setEscape(escape).build());
  }

  public CSVFile withQuoteAll() {
    return new CSVFile(format.builder().setQuoteMode(QuoteMode.ALL).build());
  }

  public CSVFile withFirstRecordAsHeader() {
    return new CSVFile(format.builder().setHeader().setSkipHeaderRecord(true).build());
  }

  public CSVFile withHeader(String... header) {
    return new CSVFile(format.builder().setHeader(header).build());
  }

  public CSVParser parse(InputStream in) throws IOException {
    return parse(in, StandardCharsets.UTF_8);
  }

  public CSVParser parse(InputStream in, Charset charset) throws IOException {
    return parse(new InputStreamReader(BOMInputStream.builder().setInputStream(in).get(), charset));
  }

  public CSVParser parse(Reader in) throws IOException {
    return CSVParser.parse(in, format);
  }

  public CSVParser parse(File in) throws IOException {
    return parse(in, StandardCharsets.UTF_8);
  }

  public CSVParser parse(File in, Charset charset) throws IOException {
    return parse(new FileInputStream(in), charset);
  }

  public CSVPrinter write(Writer out) throws IOException {
    return new CSVPrinter(out, format);
  }

  public CSVPrinter write(OutputStream out, Charset charset) throws IOException {
    return write(new OutputStreamWriter(out, charset));
  }

  public CSVPrinter write(File out, Charset charset) throws IOException {
    return write(new FileOutputStream(out), charset);
  }

  public CSVPrinter write(File out) throws IOException {
    return write(out, StandardCharsets.UTF_8);
  }

  public void parse(File in, Consumer<CSVParser> task) throws IOException {
    parse(
        new InputStreamReader(BOMInputStream.builder().setInputStream(new FileInputStream(in)).get(), StandardCharsets.UTF_8),
        task);
  }

  public void parse(Reader in, Consumer<CSVParser> task) throws IOException {
    try (CSVParser parser = parse(in)) {
      task.accept(parser);
    }
  }

  /**
   * Return a stream of {@link CSVRecord} by filtering out empty records.
   *
   * @param parser the {@link CSVParser}
   * @return stream of {@link CSVRecord}
   */
  public static Stream<CSVRecord> stream(CSVParser parser) {
    return StreamSupport.stream(parser.spliterator(), false).filter(CSVFile::notEmpty);
  }

  /**
   * Return header as array.
   *
   * @param parser the {@link CSVParser}
   * @return array of header names
   */
  public static String[] header(CSVParser parser) {
    return parser.getHeaderNames().stream().toArray(String[]::new);
  }

  /**
   * Return values as array.
   *
   * @param record the {@link CSVRecord}
   * @return array of values
   */
  public static String[] values(CSVRecord record) {
    return StreamSupport.stream(record.spliterator(), false).toArray(String[]::new);
  }

  /**
   * Check whether the record is empty.
   *
   * @param record the {@link CSVRecord}
   * @return true if record is empty
   */
  public static boolean isEmpty(CSVRecord record) {
    return record.toMap().values().stream().allMatch(StringUtils::isBlank);
  }

  /**
   * Check whether the record is not empty.
   *
   * @param record the {@link CSVRecord}
   * @return true if record is not empty
   */
  public static boolean notEmpty(CSVRecord record) {
    return !isEmpty(record);
  }
}
