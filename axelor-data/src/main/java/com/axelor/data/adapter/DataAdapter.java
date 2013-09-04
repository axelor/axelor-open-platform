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
package com.axelor.data.adapter;

import java.util.List;
import java.util.Map;
import java.util.Properties;

import com.google.common.collect.Lists;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamImplicit;

@XStreamAlias("adapter")
public class DataAdapter {
	
	static class Option {
		
		@XStreamAsAttribute
		String name;
		
		@XStreamAsAttribute
		String value;
		
		@Override
		public String toString() {
			return "{ " + name + " : " + value + " }";
		}
	}

	@XStreamAsAttribute
	private String name;
	
	@XStreamAlias("type")
	@XStreamAsAttribute
	private String klass;

	@XStreamImplicit
	@XStreamAlias("option")
	private List<Option> options;

	private Adapter adapter;
	
	public DataAdapter() {
	}
	
	public DataAdapter(String name, Class<?> type, String... options) {
		this.name = name;
		this.klass = type.getName();
		this.options = Lists.newArrayList();
		if (options.length % 2 == 0) {
			for(int i = 0 ; i < options.length ; i += 2) {
				String key = options[i];
				String val = options[i+1];
				Option opt = new Option();
				
				opt.name = key;
				opt.value = val;
				this.options.add(opt);
			}
		}
	}

	public String getName() {
		return name;
	}

	public Class<?> getType() {
		try {
			return Class.forName(klass);
		} catch (ClassNotFoundException e) {
			throw new IllegalArgumentException("No such adapter: " + klass);
		}
	}
	
	private Adapter create() {
		Class<?> type = getType();
		try {
			return (Adapter) type.newInstance();
		} catch (Exception e) {
			throw new IllegalArgumentException("Invalid adapter: " + klass);
		}
	}

	public Object adapt(Object value, Map<String, Object> context) {
		
		if (adapter == null) {
			adapter = create();
			if (options != null) {
				Properties p = new Properties();
				for(Option o : options) {
					p.setProperty(o.name, o.value);
				}
				adapter.setOptions(p);
			}
		}
		
		return adapter.adapt(value, context);
	}

}
