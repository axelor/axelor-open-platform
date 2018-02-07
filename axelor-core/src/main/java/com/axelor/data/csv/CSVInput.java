/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2018 Axelor (<http://axelor.com>).
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
package com.axelor.data.csv;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import com.axelor.data.ImportException;
import com.axelor.data.adapter.DataAdapter;
import com.axelor.inject.Beans;
import com.google.common.base.MoreObjects;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamImplicit;

@XStreamAlias("input")
public class CSVInput {

	private static transient final char DEFAULT_SEPARATOR = ',';

	@XStreamAlias("file")
	@XStreamAsAttribute
	private String fileName;
	
	@XStreamAsAttribute
	private String header;

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

	@XStreamAlias("search-call")
	@XStreamAsAttribute
	private String searchCall;

	@XStreamImplicit(itemFieldName = "bind")
	private List<CSVBind> bindings = Lists.newArrayList();

	@XStreamImplicit(itemFieldName = "adapter")
	private List<DataAdapter> adapters = Lists.newArrayList();

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}
	
	public String getHeader() {
		return header;
	}
	
	public void setHeader(String header) {
		this.header = header;
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

	public void setSeparator(char separator) {
		this.separator = Character.toString(separator);
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

	public void setSearchCall(String searchCall) {
		this.searchCall = searchCall;
	}

	public String getSearchCall() {
		return searchCall;
	}

	public List<CSVBind> getBindings() {
		return bindings;
	}

	public void setBindings(List<CSVBind> bindings) {
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
	public <T> T call(T object, Map<String, Object> context) throws Exception {

		if (Strings.isNullOrEmpty(callable))
			return object;

		if (callObject == null) {

			String className = callable.split("\\:")[0];
			String method = callable.split("\\:")[1];

			Class<?> klass = Class.forName(className);

			callMethod = klass.getMethod(method, Object.class, Map.class);
			callObject = Beans.get(klass);
		}

		try {
			return (T) callMethod.invoke(callObject, new Object[]{ object, context });
		} catch (Exception e) {
			System.err.println("EEE: " + e);
			throw new ImportException(e);
		}
	}

	public Map<String, Object> callPrepareContext(Map<String, Object> context) throws Exception {

		if (Strings.isNullOrEmpty(prepareContext))
			return context;

		if (contextObject == null) {

			String className = prepareContext.split("\\:")[0];
			String method = prepareContext.split("\\:")[1];

			Class<?> klass = Class.forName(className);

			contextMethod = klass.getMethod(method, Map.class);
			contextObject = Beans.get(klass);
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
		return MoreObjects.toStringHelper(this)
				.add("file", fileName)
				.add("type", typeName)
				.add("bindings", bindings)
				.omitNullValues().toString();
	}
}
