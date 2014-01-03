/**
 * Copyright (c) 2012-2014 Axelor. All Rights Reserved.
 *
 * The contents of this file are subject to the Common Public
 * Attribution License Version 1.0 (the “License”); you may not use
 * this file except in compliance with the License. You may obtain a
 * copy of the License at:
 *
 * http://license.axelor.com/.
 *
 * The License is based on the Mozilla Public License Version 1.1 but
 * Sections 14 and 15 have been added to cover use of software over a
 * computer network and provide for limited attribution for the
 * Original Developer. In addition, Exhibit A has been modified to be
 * consistent with Exhibit B.
 *
 * Software distributed under the License is distributed on an “AS IS”
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 *
 * The Original Code is part of "Axelor Business Suite", developed by
 * Axelor exclusively.
 *
 * The Original Developer is the Initial Developer. The Initial Developer of
 * the Original Code is Axelor.
 *
 * All portions of the code written by Axelor are
 * Copyright (c) 2012-2014 Axelor. All Rights Reserved.
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
	 * @see {@link #input(String, File)},
	 *      {@link #input(String, File, Charset)}
	 *      {@link #input(String, InputStream)},
	 *      {@link #input(String, InputStream, Charset)},
	 *      {@link #input(String, Reader)}
	 */
	public abstract void configure() throws IOException;
	
	/**
	 * Provide import error handler.
	 * 
	 * @return return {@code true} to continue else terminate the task
	 *         immediately.
	 */
	public boolean handle(ImportException e) {
		return false;
	}
	
	/**
	 * Provide io exception handler.
	 * 
	 * @return return {@code true} to continue else terminate the task
	 *         immediately.
	 */
	public boolean handle(IOException e) {
		return false;
	}
	
	/**
	 * Provide class exception handler.
	 * 
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
	 * @throws FileNotFoundException
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
	 * @throws FileNotFoundException
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
