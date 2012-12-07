package com.axelor.data.xml;

import java.util.List;
import java.util.Map;
import java.util.Properties;

import com.axelor.data.adapter.Adapter;
import com.google.common.collect.Lists;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamImplicit;

@XStreamAlias("adapter")
public class XMLAdapter {
	
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
	
	public XMLAdapter() {
	}
	
	public XMLAdapter(String name, Class<?> type, String... options) {
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
