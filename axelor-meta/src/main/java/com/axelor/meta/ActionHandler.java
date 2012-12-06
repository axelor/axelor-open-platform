package com.axelor.meta;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import groovy.lang.MissingPropertyException;

import java.io.File;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.persistence.NoResultException;
import javax.persistence.Query;

import org.codehaus.groovy.runtime.InvokerHelper;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.axelor.auth.db.User;
import com.axelor.db.JPA;
import com.axelor.db.Model;
import com.axelor.db.QueryBinder;
import com.axelor.db.mapper.Mapper;
import com.axelor.meta.views.Action;
import com.axelor.meta.views.Action.ActionMethod;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.axelor.rpc.Context;
import com.axelor.rpc.Response;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Injector;
import com.google.inject.servlet.RequestScoped;

@RequestScoped
public final class ActionHandler {
	
	private final Logger log = LoggerFactory.getLogger(ActionHandler.class);
	
	private Injector injector;

	private ActionRequest request;
	
	private Class<?> entity;
	
	private GroovyShell shell;
	
	private Binding binding;
	
	private Pattern pattern = Pattern.compile("^(select\\[\\]|select|action|call|eval):\\s*(.*)");
	
	public ActionHandler(ActionRequest request, Injector injector) {
		
		Context context = request.getContext();
		if (context == null) {
			log.debug("null context for action: {}", request.getAction());
			context = Context.create(null, request.getBeanClass());
		}
		
		this.injector = injector;
		this.request = request;
		this.entity = request.getBeanClass();
		
		binding = new Binding(context) {
		
			@Override
			public Object getVariable(String name) {
				try {
					return super.getVariable(name);
				} catch (MissingPropertyException e) {
					if ("__me__".equals(name))
						return this;
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
		
		this.shell = new GroovyShell(binding);
		this.configureObjects();
	}
	
	@SuppressWarnings("all")
	private void configureObjects() {
		
		Mapper mapper = Mapper.of(entity);
		
		Model bean = (Model) Mapper.toBean(entity, binding.getVariables());
		Model self = bean;
		
		if (bean.getId() != null) {
			self = JPA.find((Class<Model>) entity, bean.getId());
		}
		
		Object ref = binding.getProperty("_ref");
		if (ref instanceof Map) {
			try {
				Class<?> refClass = Class.forName((String) ((Map) ref).get("_model"));
				Object refId = ((Map) ref).get("id");
				ref = JPA.find((Class<Model>) refClass, Long.parseLong(refId.toString()));
				binding.setProperty("__ref__", ref);
			} catch (Exception e) {
			}
		}
		
		binding.setProperty("__this__", bean);
		binding.setProperty("__self__", self);

		Subject subject = SecurityUtils.getSubject();
		if (subject != null) {
			User user = User.all().filter("self.code = ?1", subject.getPrincipal()).fetchOne();
			binding.setProperty("__user__", user);
		}
	}
	
	/**
	 * Evaluate the given <code>expression</code>.
	 * 
	 * @param expression
	 * 					the expression to evaluate prefixed with action type
	 * 					followed by a <code>:</code>
	 * @param references
	 * @return
	 * 					expression result
	 */
	public Object evaluate(String expression) {
		
		if (Strings.isNullOrEmpty(expression)) {
			return null;
		}
		
		String kind = null;
		String expr = expression;
		Matcher matcher = pattern.matcher(expression);
		
		if (matcher.matches()) {
			kind = matcher.group(1);
			expr = matcher.group(2);
		} else {
			return expr;
		}

		if ("eval".equals(kind)) {
			return handleGroovy(expr);
		}
		
		if ("action".equals(kind)) {
			return handleAction(expr);
		}
		
		if ("call".equals(kind)) {
			return handleCall(expr);
		}
		
		if ("select".equals(kind)) {
			return handleSelectOne(expr);
		}
		
		if ("select[]".equals(kind)) {
			return handleSelectAll(expr);
		}
		
		return expr;
	}
	
	public Injector getInjector() {
		return injector;
	}
	
	@SuppressWarnings("unchecked")
	public Map<String, Object> getContext() {
		return binding.getVariables();
	}

	public Object call(String className, String method) {
		ActionResponse response = new ActionResponse();
		try {
			Class<?> klass = Class.forName(className);
			Method m = klass.getMethod(method,
					ActionRequest.class,
					ActionResponse.class);
			Object obj = injector.getInstance(klass);
			m.invoke(obj, new Object[] { request, response });
			response.setStatus(Response.STATUS_SUCCESS);
		} catch (Exception e) {
			e.printStackTrace();
			response.setException(e);
		}
		return response;
	}
	
	public Object rpc(String className, String methodCall) {
		
		Pattern p = Pattern.compile("(\\w+)\\((.*?)\\)");
		Matcher m = p.matcher(methodCall);

		if (!m.matches()) return null;
		
		try {
			Class<?> klass = Class.forName(className);
			Object object = injector.getInstance(klass);

			String method = m.group(1);
			String params = "[" + m.group(2) + "] as Object[]";

			Object[] arguments = (Object[]) shell.evaluate(params);
			
			return InvokerHelper.invokeMethod(object, method, arguments);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	@SuppressWarnings("all")
	public String template(File template) {
		return TemplateHelper.make(template, binding);
	}

	@SuppressWarnings("all")
	private Query select(String query, Object... params) {
		Preconditions.checkArgument(!Strings.isNullOrEmpty(query));
		if (!query.toLowerCase().startsWith("select "))
			query = "SELECT " + query;
		
		Query q = JPA.em().createQuery(query);
		QueryBinder binder = new QueryBinder(q);
		binder.bind(binding.getVariables(), params);
		
		return q;
	}
	
	public Object selectOne(String query, Object... params) {
		Query q = select(query, params);
		try {
			return q.getSingleResult();
		} catch (NoResultException e) {
		}
		return null;
	}
	
	public Object selectAll(String query, Object... params) {
		try {
			return select(query, params).getResultList();
		} catch (Exception e) {
		}
		return null;
	}
	
	@SuppressWarnings("all")
	public Object search(Class<?> entityClass, String filter, Map params) {
		filter = makeMethodCall("filter", filter);
		filter = String.format("%s.all().%s", entityClass.getName(), filter);
		com.axelor.db.Query q = (com.axelor.db.Query) shell.evaluate(filter);
		Map vars = Maps.newHashMap();
		if (params != null)
			vars.putAll(params);
		vars.putAll(binding.getVariables());
		q = q.bind(vars);
		return q.fetchOne();
	}
	
	private String makeMethodCall(String method, String expression) {
		expression = expression.trim();
		// check if expression is parameterized 
		if (!expression.startsWith("(")) {
			if (!expression.matches("('|\")")) {
				expression = "\"\"\"" + expression + "\"\"\""; 
			}
			expression = "(" + expression + ")";
		}
		return method + expression;
	}
	
	private Object handleSelectOne(String expression) {
		expression = makeMethodCall("__me__.selectOne", expression);
		return shell.evaluate(expression);
	}
	
	private Object handleSelectAll(String expression) {
		expression = makeMethodCall("__me__.selectAll", expression);
		return shell.evaluate(expression);
	}
	
	private Object handleGroovy(String expression) {
		return shell.evaluate(expression);
	}
	
	private Object handleAction(String expression) {
		
		Action action = MetaStore.getAction(expression);
		if (action == null) {
			log.debug("no such action found: {}", expression);
			return null;
		}

		return action.evaluate(this);
	}
	
	private Object handleCall(String expression) {
		
		if (Strings.isNullOrEmpty(expression))
			return null;

		String[] parts = expression.split("\\:");
		if (parts.length != 2) {
			log.error("Invalid call expression: ", expression);
			return null;
		}
		
		Action action = new ActionMethod();
		Action.Call call = new Action.Call();
		
		call.setController(parts[0]);
		call.setMethod(parts[1]);
		action.setElements(Lists.newArrayList(call));
		
		return action.evaluate(this);
	}
	
	private Action findAction(String name) {
		
		if (name == null || "".equals(name.trim()))
			return null;
		
		name = name.trim();
		if (name.contains(":")) {
			String[] parts = name.split("\\:");
			Action.Call method = new Action.Call();
			ActionMethod action = new ActionMethod();
			
			method.setController(parts[0]);
			method.setMethod(parts[1]);
			action.setElements(ImmutableList.of(method));

			return action;
		}

		return MetaStore.getAction(name);
	}
	
	@SuppressWarnings("all")
	public ActionResponse execute() {
		
		ActionResponse response = new ActionResponse();
		
		List<Object> all = Lists.newArrayList();
		String name = request.getAction();
		
		Action action = this.findAction(name);
		if (action == null) {
			log.error("No such action: {}", name);
		} else {
			Object value = action.wrap(this);
			if (value instanceof ActionResponse)
				return (ActionResponse) value;
			if (value != null)
				all.add(value);
		}
		
		response.setStatus(ActionResponse.STATUS_SUCCESS);
		response.setData(all);
		
		return response;
	}
}
