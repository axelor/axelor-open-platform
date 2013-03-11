package com.axelor.data.csv;

import groovy.lang.Binding;
import groovy.lang.GroovyCodeSource;
import groovy.lang.GroovyShell;
import groovy.lang.MissingPropertyException;
import groovy.lang.Script;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.codehaus.groovy.control.CompilerConfiguration;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.google.common.base.Objects.ToStringHelper;
import com.google.common.base.Strings;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamImplicit;

@XStreamAlias("bind")
public class CSVBinding {

	@XStreamAsAttribute
	private String column;

	@XStreamAlias("to")
	@XStreamAsAttribute
	private String field;
	
	@XStreamAsAttribute
	private String type;

	@XStreamAsAttribute
	private String search;
	
	@XStreamAsAttribute
	private boolean update;
	
	@XStreamAlias("eval")
	@XStreamAsAttribute
	private String expression;
	
	@XStreamAlias("if")
	@XStreamAsAttribute
	private String condition;
	
	@XStreamImplicit(itemFieldName = "bind")
	private List<CSVBinding> bindings;
	
	@XStreamAsAttribute
	private String adapter;

	public String getColumn() {
		return column;
	}

	public String getField() {
		return field;
	}
	
	public String getType() {
		return type;
	}

	public String getSearch() {
		return search;
	}
	
	public boolean isUpdate() {
		return update;
	}
	
	public String getExpression() {
		return expression;
	}
	
	public String getCondition() {
		return condition;
	}
	
	public List<CSVBinding> getBindings() {
		return bindings;
	}
	
	public String getAdapter() {
		return adapter;
	}
	
	public static CSVBinding getBinding(final String column, final String field, Set<String> cols) {
		CSVBinding cb = new CSVBinding();
		cb.field = field;
		cb.column = column;
		
		if (cols == null || cols.isEmpty()) {
			if (cb.column == null)
				cb.column = field;
			return cb;
		}
		
		for(String col : cols) {
			if (cb.bindings == null)
				cb.bindings = Lists.newArrayList();
			cb.bindings.add(CSVBinding.getBinding(field + "." + col, col, null));
		}
		
		cb.update = true;
		cb.search = Joiner.on(" AND ").join(Collections2.transform(cols, new Function<String, String>(){
			
			@Override
			public String apply(String input) {
				return String.format("self.%s = :%s_%s_", input, field, input);
			}
		}));
		
		return cb;
	}
	
	private Script scriptIf;
	private Script scriptEval;

	private Script newScript(final String expr) {
		GroovyCodeSource gcs = AccessController.doPrivileged(new PrivilegedAction<GroovyCodeSource>() {
            public GroovyCodeSource run() {
                return new GroovyCodeSource(expr, "T" + column, "/groovy/shell");
            }
        });

		CompilerConfiguration config = new CompilerConfiguration();
		config.getOptimizationOptions().put("indy", Boolean.TRUE);
		
		GroovyShell shell = new GroovyShell(config);
		return shell.parse(gcs);
	}
	
	private Object eval(Script script, Map<String, Object> context) {
		
		script.setBinding(new Binding(context) {
			
			@Override
			public Object getVariable(String name) {
				try {
					return super.getVariable(name);
				} catch (MissingPropertyException e){
					return null;
				}
			}
		});
		
		return script.run();
	}
	
	public Object eval(Map<String, Object> context) {
		if (Strings.isNullOrEmpty(expression)) {
			return context.get(column);
		}
		if (scriptEval == null) {
			scriptEval = newScript(expression);
		}
		return eval(scriptEval, context);
	}
	
	public boolean validate(Map<String, Object> context) {
		if (Strings.isNullOrEmpty(condition)) {
			return true;
		}
		String expr = condition + " ? true : false";
		if (scriptIf == null) {
			scriptIf = newScript(expr);
		}
		return (Boolean) eval(scriptIf, context);
	}

	@Override
	public String toString() {
		
		ToStringHelper ts = Objects.toStringHelper(this);
		
		if (column != null) ts.add("column", column);
		if (field != null) ts.add("field", field);
		if (type != null) ts.add("type", type);
		if (bindings != null) ts.add("bindings", bindings).toString();
		
		return ts.toString();
	}
}
