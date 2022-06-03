/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2022 Axelor (<http://axelor.com>).
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
package com.axelor.data;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.Collection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Import task configures input sources and provides error handler. */
public abstract class ImportTask implements Closeable {

  private final Multimap<String, Reader> readers = ArrayListMultimap.create();

  private static final Logger logger = LoggerFactory.getLogger(ImportTask.class);

  /**
   * Configure the input sources using the various {@code input} methods.
   *
   * @throws IOException if unable to read configuration
   * @see #input(String, File)
   * @see #input(String, File, Charset)
   * @see #input(String, InputStream)
   * @see #input(String, InputStream, Charset)
   * @see #input(String, Reader)
   */
  public abstract void configure() throws IOException;

  public void init() throws IOException {
    if (readers.isEmpty()) {
      configure();
    }
  }

  /**
   * Provide import error handler.
   *
   * @param e the error cause
   * @return return {@code true} to continue else terminate the task immediately.
   */
  public boolean handle(ImportException e) {
    return false;
  }

  /**
   * Provide {@link IOException} handler.
   *
   * @param e the error cause
   * @return return {@code true} to continue else terminate the task immediately.
   */
  public boolean handle(IOException e) {
    return false;
  }

  /**
   * Provide {@link ClassNotFoundException} handler.
   *
   * @param e the error cause
   * @return return {@code true} to continue else terminate the task immediately.
   */
  public boolean handle(ClassNotFoundException e) {
    return false;
  }

  /**
   * Provide the input source.
   *
   * @param inputName the input name
   * @param source the input source
   * @throws FileNotFoundException if source file doesn't exist
   */
  public void input(String inputName, File source) throws FileNotFoundException {
    input(inputName, source, Charset.defaultCharset());
  }

  /**
   * Provide the input source.
   *
   * @param inputName the input name
   * @param source the input source
   * @param charset the source encoding
   * @throws FileNotFoundException if source file doesn't exist
   */
  public void input(String inputName, File source, Charset charset) throws FileNotFoundException {
    input(inputName, new FileInputStream(source), charset);
  }

  /**
   * Provide the input source.
   *
   * @param inputName the input name
   * @param source the input source
   */
  public void input(String inputName, InputStream source) {
    input(inputName, source, Charset.defaultCharset());
  }

  /**
   * Provide the input source.
   *
   * @param inputName the input name
   * @param source the input source
   * @param charset the source encoding
   */
  public void input(String inputName, InputStream source, Charset charset) {
    input(inputName, new InputStreamReader(source, charset));
  }

  /**
   * Provide the input source.
   *
   * @param inputName the input name
   * @param reader the input source
   */
  public void input(String inputName, Reader reader) {
    readers.put(inputName, reader);
  }

  public Collection<Reader> getReader(String filename) {
    return readers.get(filename);
  }

  @Override
  public void close() {
    for (final Reader reader : readers.values()) {
      try {
        reader.close();
      } catch (IOException e) {
        logger.error("Error while closing reader: " + e.getMessage(), e);
      }
    }

    readers.clear();
  }
}
