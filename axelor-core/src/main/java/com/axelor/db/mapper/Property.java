/**
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2016 Axelor (<http://axelor.com>).
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
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Transient;
import javax.persistence.Version;
import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Digits;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import com.axelor.common.Inflector;
import com.axelor.db.Model;
import com.axelor.db.annotations.NameColumn;
import com.axelor.db.annotations.Sequence;
import com.axelor.db.annotations.VirtualColumn;
import com.axelor.db.annotations.Widget;
import com.axelor.i18n.I18n;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;

public class Property {

	private transient Class<?> entity;

	private String name;

	private PropertyType type;

	private transient Class<?> javaType;

	private transient Type genericType;

	private String mappedBy;

	private Class<?> target;

	private String targetName;

	private List<String> targetSearch;

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
	
	private boolean transient_;

	private boolean password;

	private boolean massUpdate;

	private boolean nameColumn;

	private boolean sequence;

	private boolean translatable;

	private transient String sequenceName;

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
			
			if (annotation instanceof Transient) {
				transient_ = true;
			}
			
			if (annotation instanceof Sequence) {
				readonly = true;
				sequence = true;
				sequenceName = ((Sequence) annotation).value();
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
				password = w.password();
				massUpdate = w.massUpdate();
				translatable = w.translatable();

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

	public String getTargetName() {
		if (targetName == null) {
			findTargetName();
		}
		return targetName;
	}

	public List<String> getTargetSearch() {
		if (targetName == null) {
			findTargetName();
		}
		return targetSearch;
	}

	private void findTargetName() {

		if (target == null) {
			return;
		}

		Mapper mapper = Mapper.of(target);
		Property nameField = mapper.getNameField();
		Property codeField = mapper.getProperty("code");

		String targetName = null;
		Set<String> targetSearch = new LinkedHashSet<>();

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

		this.targetName = targetName;
		this.targetSearch = new ArrayList<>(targetSearch);
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
	
	public boolean isTransient() {
		return transient_;
	}

	public boolean isPassword() {
		return password;
	}

	public boolean isMassUpdate() {
		if (isCollection() || isUnique()) {
			return false;
		}
		return massUpdate;
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
	
	public boolean isSequence() {
		return sequence;
	}

	public boolean isTranslatable() {
		return translatable;
	}

	public String getSequenceName() {
		return sequenceName;
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
	public Object add(Object bean, Object item) {
		return add(bean, item, true);
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Object add(Object bean, Object item, boolean associate) {
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
		
		if (associate) {
			items.add(setAssociation(item, bean));
		} else {
			items.add(item);
		}
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
		if (items != null) {
			for (Object item : items) {
				add(bean, item);
			}
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

		// handle bidirectional m2m
		if (mapped.isCollection()) { // m2m -> m2m
			//XXX: `mapped.add(child, bean)` here may add an unmanaged object
			// to a managed collection.
			mapped.add(child, bean, false);
			return child;
		}

		if (mapped.get(child) != bean) { // o2m -> m2o
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
		
		Collection<?> items = (Collection<?>) get(bean);
		if (items == null || items.isEmpty()) {
			return bean;
		}

		// handle bidirectional m2m
		Property mapped = Mapper.of(target).getProperty(mappedBy);
		if (mapped != null && mapped.isCollection()) { // m2m -> m2m
			for (Object item : items) {
				Collection<?> inverse = (Collection<?>) mapped.get(item);
				if (inverse != null) {
					inverse.remove(bean);
				}
			}
		}

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

			String key = field.getName().replaceAll("_+$", "");

			if ("help".equals(key) &&
				"true".equals(value)) {
				value = "help:" + entity.getSimpleName() + "." + name;
			}
			if (value != null && key.matches("help|title")) {
				value = I18n.get(value.toString());
			}

			map.put(key, value);
		}
		
		if (!map.containsKey("title")) {
			map.put("title", I18n.get(Inflector.getInstance().humanize(getName())));
		}

		if (target != null) {
			map.put("targetName", getTargetName());
			map.put("targetSearch", getTargetSearch());
		}

		return map;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "(" + toMap() + ")";
	}
}
