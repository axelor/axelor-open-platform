/**
 * Copyright (c) 2012-2013 Axelor. All Rights Reserved.
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
 * Copyright (c) 2012-2013 Axelor. All Rights Reserved.
 */
package com.axelor.data.csv;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import com.axelor.data.ImportException;
import com.axelor.data.adapter.DataAdapter;
import com.google.common.base.Objects;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamImplicit;

@XStreamAlias("input")
public class CSVInput {

	private static transient final char DEFAULT_SEPARATOR = ',';

	@Inject
	Injector injector;

	@XStreamAlias("file")
	@XStreamAsAttribute
	private String fileName;

	@XStreamAlias("type")
	@XStreamAsAttribute
	private String typeName;

	@XStreamAsAttribute
	private String separator;

	@XStreamAsAttribute
	private String search;

	@XStreamAsAttribute
	private boolean update;

	@XStreamAlias("call")
	@XStreamAsAttribute
	private String callable;

	@XStreamAlias("prepare-context")
	@XStreamAsAttribute
	private String prepareContext;

	@XStreamImplicit(itemFieldName = "bind")
	private List<CSVBinding> bindings = Lists.newArrayList();

	@XStreamImplicit(itemFieldName = "adapter")
	private List<DataAdapter> adapters = Lists.newArrayList();

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public String getTypeName() {
		return typeName;
	}

	public void setTypeName(String typeName) {
		this.typeName = typeName;
	}

	public char getSeparator() {

		if (Strings.isNullOrEmpty(separator))
			return DEFAULT_SEPARATOR;

		if ("\\t".equals(separator))
			return '\t';

		return separator.charAt(0);
	}

	public String getSearch() {
		return search;
	}

	public void setSearch(String search) {
		this.search = search;
	}

	public boolean isUpdate() {
		return update;
	}

	public String getCallable() {
		return callable;
	}

	public void setCallable(String callable) {
		this.callable = callable;
	}

	public String getPrepareContext() {
		return prepareContext;
	}

	public List<CSVBinding> getBindings() {
		return bindings;
	}

	public void setBindings(List<CSVBinding> bindings) {
		this.bindings = bindings;
	}

	public List<DataAdapter> getAdapters() {
		if (adapters == null) {
			adapters = Lists.newArrayList();
		}
		return adapters;
	}

	private Object callObject;
	private Method callMethod;

	private Object contextObject;
	private Method contextMethod;

	@SuppressWarnings("unchecked")
	public <T> T call(T object, Map<String, Object> context, Injector injector) throws Exception {

		if (Strings.isNullOrEmpty(callable))
			return object;

		if (callObject == null) {

			String className = callable.split("\\:")[0];
			String method = callable.split("\\:")[1];

			Class<?> klass = Class.forName(className);

			callMethod = klass.getMethod(method, Object.class, Map.class);
			callObject = injector.getInstance(klass);
		}

		try {
			return (T) callMethod.invoke(callObject, new Object[]{ object, context });
		} catch (Exception e) {
			System.err.println("EEE: " + e);
			throw new ImportException(e);
		}
	}

	public Map<String, Object> callPrepareContext(Map<String, Object> context, Injector injector) throws Exception {

		if (Strings.isNullOrEmpty(prepareContext))
			return context;

		if (contextObject == null) {

			String className = prepareContext.split("\\:")[0];
			String method = prepareContext.split("\\:")[1];

			Class<?> klass = Class.forName(className);

			contextMethod = klass.getMethod(method, Map.class);
			contextObject = injector.getInstance(klass);
		}

		try {
			contextMethod.invoke(contextObject, context);
			return context;
		} catch (Exception e) {
			System.err.println("EEE: " + e);
			throw new ImportException(e);
		}
	}

	@Override
	public String toString() {
		return Objects.toStringHelper(this)
				.add("file", fileName)
				.add("type", typeName)
				.add("bindings", bindings).toString();
	}
}
