package com.axelor.data.xml;

import groovy.lang.Binding;
import groovy.lang.GroovyCodeSource;
import groovy.lang.GroovyShell;
import groovy.lang.MissingPropertyException;
import groovy.lang.Script;

import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.codehaus.groovy.control.CompilerConfiguration;

import com.google.common.base.Strings;
import com.google.common.collect.Sets;
import com.google.inject.Injector;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamImplicit;

@XStreamAlias("bind")
public class XMLBind {

	@XStreamAsAttribute
	private String node;
	
	@XStreamAlias("to")
	@XStreamAsAttribute
	private String field;
	
	@XStreamAsAttribute
	private String alias;
	
	@XStreamAlias("type")
	@XStreamAsAttribute
	private String typeName;

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
	
	@XStreamAlias("call")
	@XStreamAsAttribute
	private String callable;
	
	@XStreamAsAttribute
	private String adapter;

	@XStreamImplicit(itemFieldName = "bind")
	private List<XMLBind> bindings;

	public String getNode() {
		return node;
	}
	
	public String getField() {
		return field;
	}
	
	public String getAlias() {
		return alias;
	}
	
	public String getAliasOrName() {
		if (alias == null || "".equals(alias.trim()))
			return node;
		return alias;
	}
	
	public String getTypeName() {
		return typeName;
	}
	
	public Class<?> getType() {
		try {
			return Class.forName(typeName);
		} catch (ClassNotFoundException e) {
		}
		return null;
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
	
	public String getCallable() {
		return callable;
	}
	
	public String getAdapter() {
		return adapter;
	}
	
	public List<XMLBind> getBindings() {
		return bindings;
	}
	
	private Set<String> multiples;
	
	public boolean isMultiple(XMLBind bind) {
		if (multiples == null) {
			multiples = Sets.newHashSet();
			Set<String> found = Sets.newHashSet();
			for (XMLBind b : bindings) {
				if (found.contains(b.getNode())) {
					multiples.add(b.getNode());
				}
				found.add(b.getNode());
			}
		}
		return multiples.contains(bind.getNode());
	}
	
	private Object callObject;
	private Method callMethod;
	
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
		}
		return object;
	}

	private Script scriptIf;
	private Script scriptEval;

	private Script newScript(final String expr) {
		GroovyCodeSource gcs = AccessController.doPrivileged(new PrivilegedAction<GroovyCodeSource>() {
            public GroovyCodeSource run() {
                return new GroovyCodeSource(expr, "T" + node, "/groovy/shell");
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
			return context.get(this.getAliasOrName());
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
		
		StringBuilder sb = new StringBuilder("<bind");

		if (node != null)
			sb.append(" node='").append(node).append("'");
		if (field != null)
			sb.append(" to=='").append(field).append("'");
		if (typeName != null)
			sb.append(" type='").append(typeName).append("'");
		if (alias != null)
			sb.append(" alias='").append(alias).append("'");

		return sb.append(" ... >").toString();
	}
}
