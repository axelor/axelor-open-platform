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
package com.axelor.data;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

/**
 * Import task configures input sources and provides error handler.
 *
 */
public abstract class ImportTask {
	
	public Multimap<String, Reader> readers =  ArrayListMultimap.create();

	/**
	 * Configure the input sources using the various {@code input} methods.
	 * 
	 * @throws IOException
	 *             if unable to read configuration
	 * @see #input(String, File)
	 * @see #input(String, File, Charset)
	 * @see #input(String, InputStream)
	 * @see #input(String, InputStream, Charset)
	 * @see #input(String, Reader)
	 */
	public abstract void configure() throws IOException;
	
	/**
	 * Provide import error handler.
	 * 
	 * @param e
	 *            the error cause
	 * @return return {@code true} to continue else terminate the task
	 *         immediately.
	 */
	public boolean handle(ImportException e) {
		return false;
	}
	
	/**
	 * Provide {@link IOException} handler.
	 * 
	 * @param e
	 *            the error cause
	 * @return return {@code true} to continue else terminate the task
	 *         immediately.
	 */
	public boolean handle(IOException e) {
		return false;
	}
	
	/**
	 * Provide {@link ClassNotFoundException} handler.
	 * 
	 * @param e
	 *            the error cause
	 * @return return {@code true} to continue else terminate the task
	 *         immediately.
	 */
	public boolean handle(ClassNotFoundException e) {
		return false;
	}

	/**
	 * Provide the input source.
	 * 
	 * @param inputName
	 *            the input name
	 * @param source
	 *            the input source
	 * @throws FileNotFoundException if source file doesn't exist
	 */
	public void input(String inputName, File source) throws FileNotFoundException {
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
	 * @throws FileNotFoundException if source file doesn't exist
	 */
	public void input(String inputName, File source, Charset charset) throws FileNotFoundException {
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
	public void input(String inputName, InputStream source) {
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
	public void input(String inputName, InputStream source, Charset charset) {
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
	public void input(String inputName, Reader reader) {
		readers.put(inputName, reader);
	}
}
