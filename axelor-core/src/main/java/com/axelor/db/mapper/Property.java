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
package com.axelor.db.mapper;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Version;
import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Digits;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import com.axelor.db.Model;
import com.axelor.db.NameColumn;
import com.axelor.db.VirtualColumn;
import com.axelor.db.Widget;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;

public class Property {

	private transient Class<?> entity;

	private String name;

	private PropertyType type;

	private transient Class<?> javaType;

	private transient Type genericType;

	private String mappedBy;

	private Class<?> target;

	private boolean primary;

	private boolean required;

	private boolean unique;

	private boolean orphan;

	private transient boolean hashKey;

	private Object maxSize;

	private Object minSize;

	private int precision;

	private int scale;

	private String title;

	private String help;

	private boolean image;

	private boolean nullable;

	private boolean readonly;

	private boolean hidden;

	private boolean virtual;

	private boolean nameColumn;

	private String[] nameSearch;

	private String selection;

	@SuppressWarnings("unchecked")
	Property(Class<?> entity, String name, Class<?> javaType, Type genericType,
			Annotation[] annotations) {
		this.entity = entity;
		this.name = name;
		this.javaType = javaType;
		this.genericType = genericType;

		try {
			this.type = PropertyType
					.get(javaType.getSimpleName().toUpperCase());
		} catch (Exception e) {
		}

		for (Annotation annotation : annotations) {

			if (annotation instanceof Lob) {
				if (type == PropertyType.STRING)
					type = PropertyType.TEXT;
				else if (type != PropertyType.TEXT)
					type = PropertyType.BINARY;
			}

			if (javaType == BigDecimal.class) {
				type = PropertyType.DECIMAL;
			}

			if (annotation instanceof OneToOne) {
				type = PropertyType.ONE_TO_ONE;
				target = (Class<? extends Model>) javaType;
				mappedBy = ((OneToOne) annotation).mappedBy();
			}

			if (annotation instanceof ManyToOne) {
				type = PropertyType.MANY_TO_ONE;
				target = (Class<? extends Model>) javaType;
			}

			if (annotation instanceof OneToMany) {
				type = PropertyType.ONE_TO_MANY;
				target = (Class<? extends Model>) ((ParameterizedType) genericType)
						.getActualTypeArguments()[0];
				mappedBy = ((OneToMany) annotation).mappedBy();
				orphan = !((OneToMany) annotation).orphanRemoval();
			}

			if (annotation instanceof ManyToMany) {
				type = PropertyType.MANY_TO_MANY;
				target = (Class<? extends Model>) ((ParameterizedType) genericType)
						.getActualTypeArguments()[0];
				mappedBy = ((ManyToMany) annotation).mappedBy();
			}

			if (annotation instanceof Id) {
				primary = true;
				readonly = true;
				hidden = true;
			}

			if (annotation instanceof Version) {
				readonly = true;
				hidden = true;
			}

			if (annotation instanceof Column) {
				unique = ((Column) annotation).unique();
				nullable = ((Column) annotation).nullable();
			}

			// Give javax.validators precedence
			if (annotation instanceof NotNull) {
				required = true;
			}
			if (annotation instanceof Size) {
				Size s = (Size) annotation;
				maxSize = s.max();
				minSize = s.min();
			}
			if (annotation instanceof Digits) {
				Digits d = (Digits) annotation;
				scale = d.fraction();
				precision = d.integer() + scale;
			}
			if (annotation instanceof Min) {
				Min m = (Min) annotation;
				minSize = m.value();
			}
			if (annotation instanceof Max) {
				Max m = (Max) annotation;
				maxSize = m.value();
			}
			if (annotation instanceof DecimalMin) {
				DecimalMin m = (DecimalMin) annotation;
				minSize = m.value();
			}
			if (annotation instanceof DecimalMax) {
				DecimalMax m = (DecimalMax) annotation;
				maxSize = m.value();
			}

			if (annotation instanceof VirtualColumn) {
				readonly = true;
				virtual = true;
			}

			if (annotation instanceof NameColumn) {
				nameColumn = true;
			}

			// Widget attributes
			if (annotation instanceof Widget) {
				Widget w = (Widget) annotation;
				title = w.title();
				help = w.help();
				readonly = w.readonly();
				hidden = w.hidden();
				nameSearch = w.search();
				selection = w.selection();

				if (w.multiline() && type == PropertyType.STRING) {
					type = PropertyType.TEXT;
				}

				if (type == PropertyType.BINARY) {
					image = w.image();
				}
			}
		}

		if (type == null) {
			throw new IllegalArgumentException(String.format(
					"Invalid property of type '%s': %s", javaType.getName(),
					name));
		}
	}

	public Class<?> getEntity() {
		return entity;
	}

	public String getName() {
		return name;
	}

	public PropertyType getType() {
		return type;
	}

	public Class<?> getJavaType() {
		return javaType;
	}

	public Type getGenericType() {
		return genericType;
	}

	public String getMappedBy() {
		return mappedBy;
	}

	public Class<?> getTarget() {
		return target;
	}

	public boolean isPrimary() {
		return primary;
	}

	public boolean isVersion() {
		return "version".equals(name);
	}

	public boolean isRequired() {
		return required;
	}

	public boolean isUnique() {
		return unique;
	}

	public boolean isOrphan() {
		return orphan;
	}

	public boolean isHashKey() {
		return hashKey;
	}

	public boolean isVirtual() {
		return virtual;
	}

	public boolean isReference() {
		return type == PropertyType.MANY_TO_ONE
				|| type == PropertyType.ONE_TO_ONE;
	}

	public boolean isCollection() {
		return type == PropertyType.ONE_TO_MANY
				|| type == PropertyType.MANY_TO_MANY;
	}

	public Object getMaxSize() {
		return maxSize;
	}

	public Object getMinSize() {
		return minSize;
	}

	public int getPrecision() {
		return precision;
	}

	public int getScale() {
		return scale;
	}

	public String getTitle() {
		return title;
	}

	public String getHelp() {
		return help;
	}

	public boolean isImage() {
		return image;
	}

	public boolean isNullable() {
		return nullable;
	}

	public boolean isReadonly() {
		return readonly;
	}

	public boolean isHidden() {
		return hidden;
	}

	public boolean isNameColumn() {
		return nameColumn;
	}

	public String[] getNameSearch() {
		return nameSearch;
	}

	public String getSelection() {
		return selection;
	}

	/**
	 * Get the value of this property from the given bean instance.
	 *
	 * @param bean
	 *            the instance
	 * @return value of the current property
	 */
	public Object get(Object bean) {
		return Mapper.of(entity).get(bean, name);
	}

	/**
	 * Set the value for this property to the given bean instance.
	 *
	 * If the property is a collection, ensure the proper parent-child
	 * relationship marked with <i>mappedBy</i> attribute.
	 *
	 * @param bean
	 *            the bean instance
	 * @param value
	 *            the value for the property
	 * @return old value of the property
	 */
	public Object set(Object bean, Object value) {

		Object old = this.get(bean);

		if (old == value) {
			return value;
		}

		if (this.isCollection()) {
			this.clear(bean);
			if (value instanceof Collection<?>) {
				this.addAll(bean, (Collection<?>) value);
			} else {
				this.add(bean, value);
			}
		} else {
			// ignore readonly fields
			if (Mapper.of(entity).getSetter(name) != null) {
				Mapper.of(entity).set(bean, name, setAssociation(value, bean));
			}
		}

		return old;
	}

	/**
	 * If this is a multi-valued field (one-to-many, many-to-many), add the
	 * specified item to the collection.
	 *
	 * @param bean
	 *            the bean instance
	 * @param item
	 *            collection item
	 * @param rest
	 *            additional items
	 * @return the same bean instance
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public Object add(Object bean, Object item) {
		Preconditions.checkNotNull(bean);
		Preconditions.checkArgument(entity.isInstance(bean));
		Preconditions.checkState(isCollection());

		if (item == null) {
			return this.clear(bean);
		}

		Preconditions.checkArgument(target.isInstance(item));

		Collection items = (Collection) get(bean);

		if (items == null) {
			items = Set.class.isAssignableFrom(javaType) ? new HashSet() : new ArrayList();
			Mapper.of(entity).set(bean, name, items);
			// The type adapter creates new instance of collection so grab the new reference
			items = (Collection) get(bean);
		}

		items.add(setAssociation(item, bean));
		return bean;
	}

	/**
	 * If this is a multi-valued field (one-to-many, many-to-many), add all the
	 * specified items to the collection.
	 *
	 * @param bean
	 *            the bean instance
	 * @param items
	 *            the items to add
	 * @return the same bean instance
	 */
	public Object addAll(Object bean, Collection<?> items) {
		for (Object item : items) {
			add(bean, item);
		}
		return bean;
	}

	/**
	 * If this is a multi-valued field, ensure the proper parent-child
	 * relationship if association is bidirectional (marked with mappedBy
	 * attribute).
	 *
	 * @param child
	 *            the child item
	 * @param bean
	 *            the parent bean instance
	 * @return the updated child instance
	 */
	public <T, U> U setAssociation(U child, T bean) {

		if (mappedBy == null || child == null) {
			return child;
		}

		Property mapped = Mapper.of(target).getProperty(mappedBy);
		if (mapped == null) {
			return child;
		}

		if (mapped.isCollection()) { // m2m -> m2m
			// doing `mapped.add(child, bean)` here may add an unmanaged object
			// to a managed collection.
			return child;
		}

		if (!Objects.equal(mapped.get(child), bean)) { // o2m -> m2o
			mapped.set(child, bean);
		}

		return child;
	}

	/**
	 * If this is a multi-valued field, clear the collection values.
	 *
	 * @param bean
	 *            the bean instance
	 * @return the same bean instance
	 */
	public Object clear(Object bean) {
		Preconditions.checkNotNull(bean);
		Preconditions.checkArgument(entity.isInstance(bean));
		Preconditions.checkState(this.isCollection());

		try {
			((Collection<?>) get(bean)).clear();
		} catch (NullPointerException e) {
		} catch (Exception e) {
			throw new IllegalArgumentException(e);
		}
		return bean;
	}

	/**
	 * Check whether the property value in the given bean is changed.
	 *
	 */
	public boolean valueChanged(Object bean, Object oldValue) {
		Object current = get(bean);
		if (current instanceof BigDecimal && oldValue instanceof BigDecimal) {
			return ((BigDecimal)current).compareTo((BigDecimal)oldValue) != 0;
		}
		return !Objects.equal(current, oldValue);
	}

	private Map<String, Object> update(Map<String, Object> attrs) {

		if (target == null) {
			return attrs;
		}

		Mapper mapper = Mapper.of(target);
		Property nameField = mapper.getNameField();
		Property codeField = mapper.getProperty("code");

		String targetName = null;
		LinkedHashSet<String> targetSearch = Sets.newLinkedHashSet();

		if (nameField != null) {
			targetName = nameField.getName();
			targetSearch.add(targetName);
			if (nameField.getNameSearch() != null) {
				targetSearch.addAll(Arrays.asList(nameField.getNameSearch()));
			}
		}
		if (codeField != null) {
			targetSearch.add(codeField.getName());
		}

		attrs.put("targetName", targetName);
		attrs.put("targetSearch", targetSearch.size() > 0 ? targetSearch : null);

		return attrs;
	}

	/**
	 * Create a {@link Map} of property attributes. Transient and null valued
	 * attributes with be omitted.
	 *
	 * This method should be used to convert property to JSON format.
	 *
	 * @return map of property attributes
	 */
	public Map<String, Object> toMap() {

		Map<String, Object> map = new HashMap<String, Object>();

		for (Field field : this.getClass().getDeclaredFields()) {

			Object value = null;

			try {
				value = field.get(this);
			} catch (IllegalAccessException e) {
				continue;
			}

			if ((value == null)
					|| Modifier.isTransient(field.getModifiers())
					|| (value instanceof String && ((String) value).equals(""))
					|| (value instanceof Boolean && !((Boolean) value))
					|| (value instanceof Integer && ((Integer) value) == 0)
					|| (value instanceof Object[] && ((Object[]) value).length == 0)) {
				continue;
			}
			map.put(field.getName(), value);
		}

		return update(map);
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "(" + toMap() + ")";
	}
}
