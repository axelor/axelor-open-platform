package com.axelor.meta;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import groovy.lang.MissingPropertyException;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.runtime.InvokerHelper;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;

import com.axelor.auth.db.User;
import com.google.common.base.Strings;

/**
 * This class implements support for evaluating Groovy
 * expressions and calling methods.
 * 
 */
public class GroovyScriptHelper {

	private GroovyShell shell;
	
	private Binding binding;
	
	public GroovyScriptHelper(Map<String, Object> variables) {
		
		CompilerConfiguration config = new CompilerConfiguration();
		config.getOptimizationOptions().put("indy", Boolean.TRUE);

		binding = new Binding(variables) {
		
			@Override
			public Object getVariable(String name) {
				try {
					return super.getVariable(name);
				} catch (MissingPropertyException e) {
					if ("__date__".equals(name))
						return new LocalDate();
					else if ("__time__".equals(name))
						return new LocalDateTime();
					else if ("__datetime__".equals(name))
						return new DateTime();
				}
				return null;
			}
		};
		
		this.shell = new GroovyShell(binding, config);
		
		Subject subject = SecurityUtils.getSubject();
		if (subject != null) {
			User user = User.all().filter("self.code = ?1", subject.getPrincipal()).fetchOne();
			binding.setProperty("__user__", user);
		}
		
		this.configure(binding);
	}

	/**
	 * Configure the binding variables.
	 * 
	 * The default implementation does nothing. Subclasses can override this
	 * method to provide some extra binding variables.
	 * 
	 * @param binding
	 *            the groovy binding to be configured
	 */
	protected void configure(Binding binding) {
		
	}
	
	public Binding getBinding() {
		return binding;
	}
	
	/**
	 * Evaluate a boolean expression.
	 * 
	 * @param expression
	 *            a boolean expression
	 * @return true if expression evaluates to true else false
	 */
	public final boolean test(String expression) {
		if (Strings.isNullOrEmpty(expression))
			return true;
		Object result = eval(expression);
		if (result == null)
			return false;
		if (result instanceof Number && result.equals(0))
			return false;
		if (result instanceof Boolean)
			return (Boolean) result;
		return true;
	}
	
	/**
	 * Evaluate the given expression.
	 * 
	 * @param expression
	 *            the groovy expression
	 * @return expression result
	 */
	public Object eval(String expression) {
		if (Strings.isNullOrEmpty(expression)) {
			return null;
		}
		return shell.evaluate(expression);
	}
	
	/**
	 * Call a method on the given object with the provided arguments.
	 * 
	 * @param object
	 *            the object on which method should be called
	 * @param methodName
	 *            the name of the method
	 * @param arguments
	 *            method arguments
	 * @return return value of the metho
	 */
	public Object call(Object object, String methodName, Object[] arguments) {
		return InvokerHelper.invokeMethod(object, methodName, arguments);
	}
	
	/**
	 * Call a method on the given object.
	 * 
	 * The methodCallCode is a string expression containing arguments to be
	 * passed. For example:
	 * 
	 * <pre>
	 * scriptHelper.call(bean, &quot;test(var1, var2, var3)&quot;);
	 * </pre>
	 * 
	 * This is a convenient method to call:
	 * 
	 * <pre>
	 * scriptHelper.call(bean, &quot;test&quot;, new Object[] { var1, var2, var3 });
	 * </pre>
	 * 
	 * @param object
	 *            the object on which method should be called
	 * @param methodCallCode
	 *            method call expression
	 * @return return value of the method
	 */
	public Object call(Object object, String methodCallCode) {
		Pattern p = Pattern.compile("(\\w+)\\((.*?)\\)");
		Matcher m = p.matcher(methodCallCode);
		
		if (!m.matches()) return null;
		
		String method = m.group(1);
		String params = "[" + m.group(2) + "] as Object[]";
		Object[] arguments = (Object[]) shell.evaluate(params);
		
		return call(object, method, arguments);
	}
}
