package com.axelor.rpc;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.persistence.EntityTransaction;

import org.hibernate.proxy.HibernateProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.axelor.db.JPA;
import com.axelor.db.Model;
import com.axelor.db.Query;
import com.axelor.db.QueryBinder;
import com.axelor.db.mapper.Mapper;
import com.axelor.db.mapper.Property;
import com.axelor.db.mapper.PropertyType;
import com.google.common.base.Function;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Injector;
import com.google.inject.TypeLiteral;

/**
 * This class defines CRUD like interface.
 * 
 */
public class Resource<T extends Model> {

	private Class<T> model;

    private Logger LOG = LoggerFactory.getLogger(Resource.class);
    
	public Resource(Class<T> model) {
		this.model = model;
	}

	@Inject
	@SuppressWarnings("unchecked")
	public Resource(TypeLiteral<T> typeLiteral) {
		this((Class<T>) typeLiteral.getRawType());
	}

	/**
	 * Returns the resource class.
	 * 
	 */
	public Class<?> getModel() {
		return model;
	}

	public Response fields() {
		
		Response response = new Response();
		Map<String, Object> meta = Maps.newHashMap();

		try {
			List<Object> fields = Lists.newArrayList();
			for (Property p : JPA.fields(model)) {
				fields.add(p.toMap());
			}

			meta.put("model", model.getName());
			//TODO: meta.put("title", "");
			//TODO: meta.put("description", "");
			//TODO: meta.put("defaults", null);
			meta.put("fields", fields);
			
			response.setData(meta);
			response.setStatus(Response.STATUS_SUCCESS);
		} catch (Exception e) {
			response.setException(e);
		}
		return response;
	}

	public static Response models(Request request) {

		Response response = new Response();
		
		List<String> data = Lists.newArrayList();
		for(Class<?> type : JPA.models()) {
			data.add(type.getName());
		}
		
		Collections.sort(data);

		response.setData(ImmutableList.copyOf(data));
		response.setStatus(Response.STATUS_SUCCESS);

		return response;
	}
	
	private List<String> getSortBy(Request request) {
		
		List<String> sortBy = Lists.newArrayList();
		Mapper mapper = Mapper.of(model);
		
		if (request.getSortBy() != null) {
			for(String spec : request.getSortBy()) {
				String name = spec;
				if (name.startsWith("-")) {
					name = name.substring(1);
				}
				Property property = mapper.getProperty(name);
				if (property != null && property.isReference()) {
					// use name field to sort many-to-one column
					Mapper m = Mapper.of(property.getTarget());
					Property p = m.getNameField();
					if (p != null) {
						spec = spec + "." + p.getName();
					}
				}
				sortBy.add(spec);
			}
		}
		
		if (sortBy.size() > 0) {
			return sortBy;
		}
		
		if (mapper.getNameField() != null) {
			sortBy.add(mapper.getNameField().getName());
			return sortBy;
		}
		
		if (mapper.getProperty("code") != null) {
			sortBy.add("code");
			return sortBy;
		}
		
		return sortBy;
	}
	
	private Criteria getCriteria(Request request) {
		if (request.getData() != null) {
			Object domain = request.getData().get("_domain");
			if (domain != null) {
				try {
					JPA.em().createQuery(
							"SELECT self FROM " + model.getSimpleName() +
							" self WHERE " + domain);
				} catch (Exception e){
					LOG.error("Invalid domain: {}", domain);
					request.getData().remove("_domain");
				}
			}
		}
		return request.getCriteria();
	}

	public Response search(Request request) {
		
		if (LOG.isDebugEnabled()) {
			LOG.debug("Searching '{}' with {}", model.getCanonicalName(),
					request.getData());
		}

		Response response = new Response();

		try {
			int offset = request.getOffset();
			int limit = request.getLimit();
	
			Criteria criteria = getCriteria(request);
	
			Query<?> query = JPA.all(model);
			if (criteria != null) {
				query = criteria.createQuery(model);
			}
			
			for(String sortBy : getSortBy(request)) {
				query = query.order(sortBy);
			}
			
			List<?> data = null;
			try {
				if (request.getFields() != null) {
					Query<?>.Selector selector = query.select(request.getFields().toArray(new String[]{}));
					if (LOG.isDebugEnabled()) {
						LOG.debug(selector.toString());
					}
					data = selector.fetch(limit, offset);
				} else {
					if (LOG.isDebugEnabled()) {
						LOG.debug(query.toString());
					}
					data = query.fetch(limit, offset);
				}
			} catch (Exception e) {
				EntityTransaction txn = JPA.em().getTransaction();
				if (txn.isActive()) {
					txn.rollback();
				}
				LOG.error("Fetch data without filter, query failed: " + Throwables.getRootCause(e));
				data = (query = JPA.all(model)).fetch(limit, offset);
			}
	
			data = Lists.transform(data, new Function<Object, Object>() {
	
				@Override
				public Object apply(Object input) {
					if (input instanceof Map)
						return input;
					return toMap(input);
				}
			});
			
			if (LOG.isDebugEnabled()) {
				LOG.debug("Records found: {}", data.size());
			}
	
			response.setData(data);
			
			response.setOffset(offset);
			response.setTotal(query.count());
			
			response.setStatus(Response.STATUS_SUCCESS);
		} catch (Exception e) {
			if (LOG.isDebugEnabled())
				LOG.debug(e.toString(), e);
			response.setException(e);
		}
		return response;
	}

	public Response read(long id) {
		Response response = new Response();
		List<Object> data = Lists.newArrayList();
		
		try {
			Model entity = JPA.find(model, id);
			if (entity != null)
				data.add(entity);
			response.setData(data);
			response.setStatus(Response.STATUS_SUCCESS);
		} catch (Exception e) {
			if (LOG.isDebugEnabled())
				LOG.debug(e.toString(), e);
			response.setException(e);
		}
		return response;
	}
	
	public Response fetch(long id, Request request) {
		Response response = new Response();
		List<Object> data = Lists.newArrayList();
		try {
			Model entity = JPA.find(model, id);
			if (entity != null) {
				data.add(toMap(entity, request.getFields().toArray(new String[]{})));
			}
			response.setData(data);
			response.setStatus(Response.STATUS_SUCCESS);
		} catch (Exception e) {
			if (LOG.isDebugEnabled())
				LOG.debug(e.toString(), e);
			response.setException(e);
		}
		return response;
	}

	@SuppressWarnings("all")
	public Response save(final Request request) {

		Response response = new Response();
		
		List<Object> records = request.getRecords();
		List<Object> data = Lists.newArrayList();

		if (records == null) {
			records = Lists.newArrayList();
			records.add(request.getData());
		}
		
		for(Object record : records) {
			Model bean = JPA.edit(model, (Map) record);
			bean = JPA.manage(bean);
			data.add(bean);
		}
	
		response.setData(data);
		response.setStatus(Response.STATUS_SUCCESS);
		
		return response;
	}

	public Response remove(long id, Request request) {
		
		final Response response = new Response();
		final Map<String, Object> data = Maps.newHashMap();
		
		data.put("id", id);
		data.put("version", request.getData().get("version"));
		
		Model bean = JPA.edit(model, data);
		if (bean.getId() != null) {
			JPA.remove(bean);
		}
		
		response.setData(ImmutableList.of(_toMap(bean, true, 0)));
		response.setStatus(Response.STATUS_SUCCESS);
		
		return response;
	}
	
	@SuppressWarnings("all")
	public Response remove(Request request) {
		
		final Response response = new Response();
		final List<Object> result = Lists.newArrayList();
		final List<Object> records = request.getRecords();

		if (records == null || records.isEmpty()) {
			response.setException(new IllegalArgumentException("No records provides."));
			return response;
		}

		for(Object record : records) {
			Model bean = JPA.edit(model, (Map) record);
			if (bean.getId() != null) {
				JPA.remove(bean);
				result.add(record);
			}
		}

		response.setData(result);
		response.setStatus(Response.STATUS_SUCCESS);

		return response;
	}
	
	public Response copy(long id) {
		Response response = new Response();
		Model bean = JPA.find(model, id);
		
		try {
			bean = JPA.copy(bean, true);
			response.setData(ImmutableList.of(bean));
			response.setStatus(Response.STATUS_SUCCESS);
		} catch (Exception e) {
			if (LOG.isDebugEnabled())
				LOG.debug(e.toString(), e);
			response.setException(e);
		}
		return response;
	}

	@Inject
	Injector injector;

	@SuppressWarnings("all")
	public ActionResponse action(ActionRequest request) {

		ActionResponse response = new ActionResponse();

		Map<String, Object> data = (Map<String, Object>) request.getData();
		String[] parts = request.getAction().split("\\:");

		if (parts.length != 2) {
			response.setStatus(Response.STATUS_FAILURE);
			return response;
		}

		String controller = parts[0];
		String method = parts[1];

		try {
			Class<?> klass = Class.forName(controller);
			Method m = klass.getDeclaredMethod(method, ActionRequest.class,
					ActionResponse.class);
			Object obj = injector.getInstance(klass);

			m.invoke(obj, new Object[] { request, response });
			response.setStatus(Response.STATUS_SUCCESS);
		} catch (Exception e) {
			if (LOG.isDebugEnabled())
				LOG.debug(e.toString(), e);
			response.setException(e);
		}
		return response;
	}
	
	/**
	 * Get the name of the record. This method should be used to get the value
	 * of name field if it's a function field.
	 * 
	 * @param request
	 *            the request containing the current values of the record
	 * @return response with the updated values with record name
	 */
	public Response getRecordName(Request request) {

		Response response = new Response();
		
		Mapper mapper = Mapper.of(model);
		Map<String, Object> data = request.getData();
		
		try {
			Property property = mapper.getNameField();
			if (property != null) {
				String qs = String.format(
						"SELECT self.%s FROM %s self WHERE self.id = :id",
						property.getName(), model.getSimpleName());

				javax.persistence.Query query = JPA.em().createQuery(qs);
				QueryBinder binder = new QueryBinder(query);
				query = binder.bind(data, null);
				
				Object name = query.getSingleResult();
				data.put(property.getName(), name);
			}
	
			response.setData(ImmutableList.of(data));
			response.setStatus(Response.STATUS_SUCCESS);
		} catch (Exception e) {
			if (LOG.isDebugEnabled())
				LOG.debug(e.toString(), e);
			response.setException(e);
		}
		return response;
	}

	/**
	 * Convert the given model instance to {@link Map}.
	 * 
	 * This method converts the given bean to map using {@link Mapper}. The
	 * multi-value fields will be converted to list of ids.
	 * 
	 * @param bean
	 *            JPA managed model instance
	 * @return a {@link Map} with property names as keys and corresponding
	 *         property values as values.
	 */
	public static Map<String, Object> toMap(Object bean) {
		return _toMap(bean, false, 0);
	}
	
	@SuppressWarnings("unchecked")
	public static Map<String, Object> _toMap(Object bean, boolean compact, int level) {

		if (bean == null) {
			return null;
		}
		
		if (bean instanceof HibernateProxy) {
			bean = ((HibernateProxy) bean).getHibernateLazyInitializer().getImplementation();
		}

		Map<String, Object> result = new HashMap<String, Object>();
		Mapper mapper = Mapper.of(bean.getClass());

		if ((compact && ((Model)bean).getId() != null) || level >= 1) {
			Property pn = mapper.getProperty("name");
			Property pc = mapper.getProperty("code");

			for (Property p : mapper.getProperties()) {
				if (p.isNameColumn())
					pn = p;
			}

			result.put("id", mapper.get(bean, "id"));
			result.put("$version", mapper.get(bean, "version"));

			if (pn != null)
				result.put(pn.getName(), mapper.get(bean, pn.getName()));
			if (pc != null)
				result.put(pc.getName(), mapper.get(bean, pc.getName()));

			return result;
		}
		
		for (final Property p : mapper.getProperties()) {
			
			PropertyType type = p.getType();
			
			if (type == PropertyType.BINARY) {
				continue;
			}
			
			String name = p.getName();
			Object value = mapper.get(bean, name);
			
			if (value != null) {

				if (p.isReference()) {
					value = _toMap(value, true, level+1);
				}
				else if (p.isCollection()) {
					List<Object> items = Lists.newArrayList();
					for(Model input : (Collection<Model>) value) {
						Map<String, Object> item;
						if (input.getId() != null) {
							item = Maps.newHashMap();
							item.put("id", input.getId());
							item.put("$version", input.getVersion());
						} else {
							item = _toMap(input, false, 0);
						}
						if (item != null)
							items.add(item);
					}
					value = items;
				}
			}
			result.put(name, value);
		}
		
		return result;
	}

	/**
	 * This method allows to expand given field names with dotted notations.
	 * 
	 */
	private static Map<String, Object> toMap(Object bean, String... names) {
		Map<String, Object> fields = unflatten(null, names);
		return _toMap(bean, fields);
	}
	
	@SuppressWarnings("all")
	private static Map<String, Object> _toMap(Object bean, Map<String, Object> fields) {
		
		if (bean == null) {
			return null;
		}
		
		if (bean instanceof HibernateProxy) {
			bean = ((HibernateProxy) bean).getHibernateLazyInitializer().getImplementation();
		}

		Map<String, Object> result = new HashMap<String, Object>();
		Mapper mapper = Mapper.of(bean.getClass());
		boolean compact = fields.containsKey("$version");

		if ((compact && ((Model)bean).getId() != null)) {
			Property pn = mapper.getNameField();
			Property pc = mapper.getProperty("code");

			result.put("id", mapper.get(bean, "id"));
			result.put("$version", mapper.get(bean, "version"));

			if (pn != null)
				result.put(pn.getName(), pn.get(bean));
			if (pc != null)
				result.put(pc.getName(), pc.get(bean));

			for(String name: fields.keySet()) {
				Object child = mapper.get(bean, name);
				if (child instanceof Model) {
					child = _toMap(child, (Map) fields.get(name));
				}
				if (child != null) {
					result.put(name, child);
				}
			}

			return result;
		}
		
		for (final Property p : mapper.getProperties()) {
			
			PropertyType type = p.getType();
			
			if (type == PropertyType.BINARY) {
				continue;
			}
			
			String name = p.getName();
			Object value = mapper.get(bean, name);
			
			if (value instanceof Model) { // m2o
				Map<String, Object> _fields = (Map) fields.get(p.getName());
				if (_fields == null) {
					_fields = Maps.newHashMap();
				}
				_fields.put("$version", null);
				value = _toMap(value, _fields);
			}
			if (value instanceof Collection<?>) { // m2m | o2m
				List<Object> items = Lists.newArrayList();
				for(Object item : (Collection) value) {
					items.add(toMap(item, "$version"));
				}
				value = items;
			}
			
			result.put(name, value);
		}
		
		return result;
	}

	@SuppressWarnings("all")
	private static Map<String, Object> unflatten(Map<String, Object> map, String... names) {
		if (map == null) map = Maps.newHashMap();
		for(String name : names) {
			if (map.containsKey(name))
				continue;
			if (name.contains(".")) {
				String[] parts = name.split("\\.", 2);
				Map<String, Object> child = (Map) map.get(parts[0]);
				if (child == null) {
					child = Maps.newHashMap();
					child.put("$version", null);
				}
				map.put(parts[0], unflatten(child, parts[1]));
			} else {
				map.put(name, Maps.newHashMap());
			}
		}
		return map;
	}
}
