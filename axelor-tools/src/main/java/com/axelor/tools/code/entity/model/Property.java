/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2022 Axelor (<http://axelor.com>).
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.axelor.tools.code.entity.model;

import static com.axelor.tools.code.entity.model.Utils.*;

import com.axelor.common.Inflector;
import com.axelor.common.StringUtils;
import com.axelor.tools.code.JavaAnnotation;
import com.axelor.tools.code.JavaCode;
import com.axelor.tools.code.JavaDoc;
import com.axelor.tools.code.JavaField;
import com.axelor.tools.code.JavaMethod;
import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;

@XmlType
public abstract class Property {

  @Overridable @XmlValue private String content;

  @XmlAttribute(name = "name", required = true)
  private String name;

  @XmlTransient private PropertyType type;

  @XmlAttribute(name = "column")
  private String column;

  @Overridable
  @XmlAttribute(name = "title")
  private String title;

  @Overridable
  @XmlAttribute(name = "help")
  private String help;

  @Overridable
  @XmlAttribute(name = "required")
  private Boolean required;

  @Overridable
  @XmlAttribute(name = "readonly")
  private Boolean readonly;

  @Overridable
  @XmlAttribute(name = "hidden")
  private Boolean hidden;

  @Overridable(PersistedChecker.class)
  @XmlAttribute(name = "transient")
  private Boolean _transient;

  @Overridable
  @XmlAttribute(name = "default")
  private String _default;

  @Overridable
  @XmlAttribute(name = "unique")
  private Boolean unique;

  @XmlAttribute(name = "initParam")
  private Boolean initParam;

  @Overridable
  @XmlAttribute(name = "index")
  private String index;

  @Overridable
  @XmlAttribute(name = "massUpdate")
  private Boolean massUpdate;

  @Overridable
  @XmlAttribute(name = "copy")
  private Boolean copy;

  @Overridable(SameTargetChecker.class)
  @XmlAttribute(name = "ref")
  private String target;

  @XmlAttribute(name = "mappedBy")
  private String mappedBy;

  @Overridable
  @XmlAttribute(name = "orphanRemoval")
  private Boolean orphanRemoval;

  @Overridable
  @XmlAttribute(name = "orderBy")
  private String orderBy;

  @XmlAttribute(name = "table")
  private String table;

  @XmlAttribute(name = "column2")
  private String column2;

  @Overridable
  @XmlAttribute(name = "nullable")
  private Boolean nullable;

  @Overridable
  @XmlAttribute(name = "selection")
  private String selection;

  @Overridable
  @XmlAttribute(name = "equalsInclude")
  private Boolean equalsInclude;

  @Overridable(PersistedChecker.class)
  @XmlAttribute(name = "formula")
  private Boolean formula;

  @Overridable
  @XmlAttribute(name = "min")
  private String min;

  @Overridable
  @XmlAttribute(name = "max")
  private String max;

  @Overridable
  @XmlAttribute(name = "precision")
  private Integer precision;

  @Overridable
  @XmlAttribute(name = "scale")
  private Integer scale;

  @Overridable
  @XmlAttribute(name = "image")
  private Boolean image;

  @Overridable
  @XmlAttribute(name = "encrypted")
  private Boolean encrypted;

  @XmlAttribute(name = "tz")
  private Boolean tz;

  @Overridable
  @XmlAttribute(name = "multiline")
  private Boolean multiline;

  @Overridable(LargeChecker.class)
  @XmlAttribute(name = "large")
  private Boolean large;

  @Overridable
  @XmlAttribute(name = "namecolumn")
  private Boolean nameField;

  @Overridable
  @XmlAttribute(name = "search")
  private String search;

  @XmlAttribute(name = "json")
  private Boolean json;

  @Overridable
  @XmlAttribute(name = "password")
  private Boolean password;

  @Overridable
  @XmlAttribute(name = "sequence")
  private String sequence;

  @Overridable
  @XmlAttribute(name = "translatable")
  private Boolean translatable;

  @Overridable @XmlAttribute private Boolean insertable;

  @Overridable @XmlAttribute private Boolean updatable;

  private static class LargeChecker implements BiConsumer<Object, Object> {
    @Override
    public void accept(Object a, Object b) {
      if (isTrue((Boolean) a) && isFalse((Boolean) b)) {
        throw new IllegalArgumentException("large field cannot become non-large");
      }
    }
  }

  private static class PersistedChecker implements BiConsumer<Object, Object> {
    @Override
    public void accept(Object a, Object b) {
      if (notTrue((Boolean) a) && isTrue((Boolean) b)) {
        throw new IllegalArgumentException("persisted field cannot become non-persisted");
      }
    }
  }

  private static class SameTargetChecker implements BiConsumer<Object, Object> {
    @Override
    public void accept(Object a, Object b) {
      String first = String.valueOf(a);
      String second = String.valueOf(b);
      final int firstIndex = first.lastIndexOf('.');
      final int secondIndex = second.lastIndexOf('.');
      if (firstIndex >= 0 || secondIndex >= 0) {
        first = first.substring(firstIndex + 1);
        second = second.substring(secondIndex + 1);
      }
      if (!Objects.equals(first, second)) {
        throw new IllegalArgumentException("cannot change reference entity");
      }
    }
  }

  private static List<PropertyAttribute> attributes = null;

  public static List<PropertyAttribute> getAttributes() {
    if (attributes != null) {
      return attributes;
    }

    final BeanInfo beanInfo;
    try {
      beanInfo = Introspector.getBeanInfo(Property.class);
    } catch (IntrospectionException e) {
      throw new RuntimeException(e);
    }

    final Map<String, PropertyDescriptor> descriptors =
        Arrays.stream(beanInfo.getPropertyDescriptors())
            .collect(
                Collectors.toUnmodifiableMap(PropertyDescriptor::getName, Function.identity()));
    final BiConsumer<Object, Object> notOverridable =
        (a, b) -> {
          throw new IllegalArgumentException("this attribute is not overridable");
        };
    final Map<Class<? extends BiConsumer<Object, Object>>, BiConsumer<Object, Object>>
        checkOverrides = new HashMap<>();

    attributes =
        Arrays.stream(Property.class.getDeclaredFields())
            .filter(
                field ->
                    field.isAnnotationPresent(XmlAttribute.class)
                        || field.isAnnotationPresent(XmlValue.class))
            .map(
                field -> {
                  final String fieldName = field.getName().replaceAll("(?:^_+)|(?:_+$)", "");
                  final String attributeName =
                      Optional.ofNullable(field.getAnnotation(XmlAttribute.class))
                          .map(XmlAttribute::name)
                          .filter(name -> !name.startsWith("#"))
                          .orElse(fieldName);
                  final Overridable overridableAnnotation = field.getAnnotation(Overridable.class);
                  final BiConsumer<Object, Object> checkOverride =
                      overridableAnnotation == null
                          ? notOverridable
                          : checkOverrides.computeIfAbsent(
                              overridableAnnotation.value(),
                              cls -> {
                                try {
                                  return cls.getDeclaredConstructor().newInstance();
                                } catch (ReflectiveOperationException e) {
                                  throw new RuntimeException(e);
                                }
                              });

                  if (field.isAnnotationPresent(XmlValue.class)) {
                    return PropertyAttribute.ofValue(
                        attributeName, descriptors.get(fieldName), checkOverride);
                  }

                  return PropertyAttribute.of(
                      attributeName, descriptors.get(fieldName), checkOverride);
                })
            .collect(Collectors.toUnmodifiableList());

    return attributes;
  }

  protected Property() {
    this.type =
        PropertyType.valueOf(
            getClass().getAnnotation(XmlType.class).name().toUpperCase().replace('-', '_'));
  }

  public String getContent() {
    return content;
  }

  public void setContent(String value) {
    this.content = value;
  }

  public String getName() {
    return name;
  }

  public void setName(String value) {
    this.name = value;
  }

  public PropertyType getType() {
    return type;
  }

  public void setType(PropertyType type) {
    this.type = type;
  }

  public String getJavaType() {
    if (type == PropertyType.STRING) return "String";
    if (type == PropertyType.INTEGER) return "Integer";
    if (type == PropertyType.LONG) return "Long";
    if (type == PropertyType.BOOLEAN) return "Boolean";
    if (type == PropertyType.DECIMAL) return "java.math.BigDecimal";
    if (type == PropertyType.DATE) return "java.time.LocalDate";
    if (type == PropertyType.TIME) return "java.time.LocalTime";
    if (type == PropertyType.DATETIME && isTrue(tz)) return "java.time.ZonedDateTime";
    if (type == PropertyType.DATETIME) return "java.time.LocalDateTime";
    if (type == PropertyType.BINARY) return "byte[]";
    if (type == PropertyType.ENUM) return target;
    if (type == PropertyType.ONE_TO_ONE) return target;
    if (type == PropertyType.MANY_TO_ONE) return target;
    if (type == PropertyType.ONE_TO_MANY) return String.format("java.util.List<%s>", target);
    if (type == PropertyType.MANY_TO_MANY) return String.format("java.util.Set<%s>", target);
    return null;
  }

  public String getColumn() {
    return column;
  }

  public void setColumn(String value) {
    this.column = value;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String value) {
    this.title = value;
  }

  public String getHelp() {
    return help;
  }

  public void setHelp(String value) {
    this.help = value;
  }

  public Boolean getRequired() {
    return required;
  }

  public void setRequired(Boolean value) {
    this.required = value;
  }

  public Boolean getReadonly() {
    return readonly;
  }

  public void setReadonly(Boolean value) {
    this.readonly = value;
  }

  public Boolean getHidden() {
    return hidden;
  }

  public void setHidden(Boolean value) {
    this.hidden = value;
  }

  public Boolean getTransient() {
    return _transient;
  }

  public void setTransient(Boolean value) {
    this._transient = value;
  }

  public String getDefault() {
    return _default;
  }

  public void setDefault(String value) {
    this._default = value;
  }

  public Boolean getUnique() {
    return unique;
  }

  public void setUnique(Boolean value) {
    this.unique = value;
  }

  public Boolean getInitParam() {
    return initParam;
  }

  public void setInitParam(Boolean value) {
    this.initParam = value;
  }

  public String getIndex() {
    return index;
  }

  public void setIndex(String value) {
    this.index = value;
  }

  public Boolean getMassUpdate() {
    return massUpdate;
  }

  public void setMassUpdate(Boolean value) {
    this.massUpdate = value;
  }

  public Boolean getCopy() {
    return copy;
  }

  public void setCopy(Boolean value) {
    this.copy = value;
  }

  public String getTarget() {
    return target;
  }

  public void setTarget(String value) {
    this.target = value;
  }

  public String getMappedBy() {
    return mappedBy;
  }

  public void setMappedBy(String value) {
    this.mappedBy = value;
  }

  public Boolean getOrphanRemoval() {
    if (orphanRemoval != null) {
      return orphanRemoval;
    }
    // if not specified, orphanRemoval is considered as true in case of bidirectional O2M
    return notBlank(mappedBy) && type == PropertyType.ONE_TO_MANY;
  }

  public void setOrphanRemoval(Boolean value) {
    this.orphanRemoval = value;
  }

  public String getOrderBy() {
    return orderBy;
  }

  public void setOrderBy(String value) {
    this.orderBy = value;
  }

  public String getTable() {
    return table;
  }

  public void setTable(String value) {
    this.table = value;
  }

  public String getColumn2() {
    return column2;
  }

  public void setColumn2(String value) {
    this.column2 = value;
  }

  public Boolean getNullable() {
    return nullable;
  }

  public void setNullable(Boolean value) {
    this.nullable = value;
  }

  public String getSelection() {
    return selection;
  }

  public void setSelection(String value) {
    this.selection = value;
  }

  public Boolean getEqualsInclude() {
    return equalsInclude;
  }

  public void setEqualsInclude(Boolean equalsInclude) {
    this.equalsInclude = equalsInclude;
  }

  public Boolean getFormula() {
    return formula;
  }

  public void setFormula(Boolean value) {
    this.formula = value;
  }

  public String getMin() {
    return min;
  }

  public void setMin(String value) {
    this.min = value;
  }

  public String getMax() {
    return max;
  }

  public void setMax(String value) {
    this.max = value;
  }

  public Integer getPrecision() {
    return precision;
  }

  public void setPrecision(Integer value) {
    this.precision = value;
  }

  public Integer getScale() {
    return scale;
  }

  public void setScale(Integer value) {
    this.scale = value;
  }

  public Boolean getImage() {
    return image;
  }

  public void setImage(Boolean value) {
    this.image = value;
  }

  public Boolean getMultiline() {
    return multiline;
  }

  public void setMultiline(Boolean value) {
    this.multiline = value;
  }

  public Boolean getLarge() {
    return large;
  }

  public void setLarge(Boolean value) {
    this.large = value;
  }

  public Boolean getNameField() {
    return nameField;
  }

  public void setNameField(Boolean value) {
    this.nameField = value;
  }

  public String getSearch() {
    return search;
  }

  public void setSearch(String value) {
    this.search = value;
  }

  public Boolean getJson() {
    return json;
  }

  public void setJson(Boolean value) {
    this.json = value;
  }

  public Boolean getPassword() {
    return password;
  }

  public void setPassword(Boolean value) {
    this.password = value;
  }

  public String getSequence() {
    return sequence;
  }

  public void setSequence(String value) {
    this.sequence = value;
  }

  public Boolean getTranslatable() {
    return translatable;
  }

  public void setTranslatable(Boolean value) {
    this.translatable = value;
  }

  public Boolean getInsertable() {
    return insertable;
  }

  public void setInsertable(Boolean insertable) {
    this.insertable = insertable;
  }

  public Boolean getUpdatable() {
    return updatable;
  }

  public void setUpdatable(Boolean updatable) {
    this.updatable = updatable;
  }

  public Boolean getEncrypted() {
    return encrypted;
  }

  public void setEncrypted(Boolean value) {
    this.encrypted = value;
  }

  public Boolean getTz() {
    return tz;
  }

  public void setTz(Boolean value) {
    this.tz = value;
  }

  public boolean isPrimary() {
    return "id".equals(name);
  }

  public boolean isVersion() {
    return "version".equals(name);
  }

  public boolean isSimple() {
    return !(isCollection() || isReference() || isEnum() || isBinary() || isTrue(large));
  }

  public boolean isVirtual() {
    return notBlank(content);
  }

  public boolean isCollection() {
    return type == PropertyType.ONE_TO_MANY || type == PropertyType.MANY_TO_MANY;
  }

  public boolean isReference() {
    return type == PropertyType.MANY_TO_ONE || type == PropertyType.ONE_TO_ONE;
  }

  public boolean isEnum() {
    return type == PropertyType.ENUM;
  }

  public boolean isBinary() {
    return type == PropertyType.BINARY;
  }

  public boolean isIndexable() {
    if (isTrue(unique) || isTrue(formula) || isTrue(_transient) || "false".equals(index)) {
      return false;
    }
    return isTrue(nameField)
        || "name".equals(name)
        || "code".equals(name)
        || (index != null && index.matches("^(true|idx_.*)"))
        || (isReference() && isBlank(mappedBy));
  }

  public Index createIndex() {
    if (isIndexable()) {
      String indexName = index;
      String columnName = getColumn();
      if ("true".equals(indexName)) indexName = null;
      if (columnName == null && isReference()) columnName = getColumnAuto();
      if (columnName == null) columnName = name;

      Index idx = new Index();
      idx.setName(indexName);
      idx.setColumns(columnName);

      return idx;
    }
    return null;
  }

  public String getSingularName() {
    return name.endsWith("Set") || name.endsWith("List")
        ? name + "Item"
        : Inflector.getInstance().singularize(name);
  }

  public String getColumnAuto() {
    String col = getColumn();
    if (notBlank(col)) {
      return col;
    }
    // follow hibernate naming
    final StringBuilder buf = new StringBuilder(name.replace('.', '_'));
    for (int i = 1; i < buf.length() - 1; i++) {
      if (Character.isLowerCase(buf.charAt(i - 1))
          && Character.isUpperCase(buf.charAt(i))
          && Character.isLowerCase(buf.charAt(i + 1))) {
        buf.insert(i++, '_');
      }
    }
    return buf.toString().toLowerCase();
  }

  private JavaCode getDefaultValue() {
    if (isPrimary()) {
      return null;
    }

    final String value = getDefault();
    final String empty = getEmptyValue();

    if (value == null) {
      return empty == null ? null : new JavaCode("{0:l}", empty);
    } else if (isBlank(value)) {
      return null;
    }

    if (type == PropertyType.BOOLEAN) {
      if (value.matches("true|t|1|Boolean\\.TRUE")) {
        return new JavaCode("{0:l}", "Boolean.TRUE");
      }
      return !isTrue(nullable) || value.matches("false|f|0|Boolean\\\\.FALSE")
          ? new JavaCode("{0:l}", "Boolean.FALSE")
          : new JavaCode("{0:l}", "null");
    }

    if (type == PropertyType.STRING) {
      return new JavaCode("{0:s}", value);
    }

    if (type == PropertyType.LONG) {
      return value.toLowerCase().endsWith("l")
          ? new JavaCode("{0:l}", value)
          : new JavaCode("{0:l}L", value);
    }

    if (type == PropertyType.INTEGER) {
      return new JavaCode("{0:l}", value);
    }

    if (type == PropertyType.DECIMAL) {
      return new JavaCode("new BigDecimal({0:s})", value);
    }

    if (type == PropertyType.DATE) {
      return "now".equals(value)
          ? new JavaCode("LocalDate.now()")
          : new JavaCode("LocalDate.parse({0:s})", value);
    }

    if (type == PropertyType.TIME) {
      return "now".equals(value)
          ? new JavaCode("LocalTime.now()")
          : new JavaCode("LocalTime.parse({0:s})", value);
    }

    if (type == PropertyType.DATETIME) {
      String dt = isTrue(tz) ? "ZonedDateTime" : "LocalDateTime";
      return "now".equals(value)
          ? new JavaCode("{0:t}.now()", dt)
          : new JavaCode("{0:t}.parse({1:s})", dt, value);
    }

    if (type == PropertyType.ENUM) {
      return new JavaCode("{0:m}", target + "." + value);
    }

    return null;
  }

  private String getEmptyValue() {
    if (isTrue(nullable)) return null;
    if (type == PropertyType.BOOLEAN) return "Boolean.FALSE";
    if (type == PropertyType.INTEGER) return "0";
    if (type == PropertyType.LONG) return "0L";
    if (type == PropertyType.DECIMAL) return "BigDecimal.ZERO";
    return null;
  }

  public JavaField toJavaField(Entity entity) {
    int modifiers = Modifier.PRIVATE;
    JavaField field = new JavaField(name, getJavaType(), modifiers).defaultValue(getDefaultValue());
    Stream.of(
            $id(entity),
            $equalsInclude(entity),
            $widget(),
            $binary(entity),
            $nameColumn(entity),
            $virtual(),
            $required(),
            $size(),
            $digits(),
            $transient(),
            $column(),
            $one2one(),
            $many2one(),
            $one2many(),
            $many2many(),
            $joinTable(),
            $orderBy(),
            $sequence(),
            $converter())
        .flatMap(x -> x instanceof Collection ? ((Collection<?>) x).stream() : Stream.of(x))
        .filter(Objects::nonNull)
        .map(JavaAnnotation.class::cast)
        .forEach(field::annotation);
    return field;
  }

  public List<JavaMethod> toJavaMethods() {
    return Arrays.asList(
            createGetterMethod(),
            createComputeMethod(),
            createSetterMethod(),
            createAddMethod(),
            createRemoveMethod(),
            createClearMethod())
        .stream()
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
  }

  private List<JavaAnnotation> $id(Entity entity) {
    if (!isPrimary()) {
      return null;
    }

    if (isFalse(entity.getSequential()) || isTrue(entity.getMappedSuperClass())) {
      return List.of(
          new JavaAnnotation("javax.persistence.Id"),
          new JavaAnnotation("javax.persistence.GeneratedValue")
              .param("strategy", "{0:m}", "javax.persistence.GenerationType.AUTO"));
    }

    String name = entity.getTable() + "_SEQ";

    return List.of(
        new JavaAnnotation("javax.persistence.Id"),
        new JavaAnnotation("javax.persistence.GeneratedValue")
            .param("strategy", "{0:m}", "javax.persistence.GenerationType.SEQUENCE")
            .param("generator", "{0:s}", name),
        new JavaAnnotation("javax.persistence.SequenceGenerator")
            .param("name", "{0:s}", name)
            .param("sequenceName", "{0:s}", name)
            .param("allocationSize", "{0:l}", 1));
  }

  private JavaAnnotation $equalsInclude(Entity entity) {
    if (isPrimary() || isVersion()) return null;
    if (isFalse(equalsInclude)) return null;
    if (isTrue(equalsInclude)
        || isTrue(unique)
        || (isTrue(entity.getEqualsAll()) && isSimple() && !isVirtual())) {
      return new JavaAnnotation("com.axelor.db.annotations.EqualsInclude");
    }
    return null;
  }

  private JavaAnnotation $widget() {

    if (isTrue(massUpdate) && (isTrue(unique) || isCollection() || isTrue(large))) {
      massUpdate = false;
    }

    if (notBlank(selection)) {
      selection = selection.replaceAll("\\],\\s*\\[", "], [");
    }

    JavaAnnotation widget = new JavaAnnotation("com.axelor.db.annotations.Widget");

    Map<String, JavaCode> params = new HashMap<>();
    BiFunction<String, Object, JavaCode> value = (s, v) -> v == null ? null : new JavaCode(s, v);

    params.put("image", value.apply("{0:l}", image));
    params.put("title", value.apply("{0:s}", title));
    params.put("help", value.apply("{0:s}", help));
    params.put("readonly", value.apply("{0:l}", readonly));
    params.put("hidden", value.apply("{0:l}", hidden));
    params.put("multiline", value.apply("{0:l}", multiline));
    params.put("search", value.apply("{0:a}", search));
    params.put("selection", value.apply("{0:s}", selection));
    params.put("password", value.apply("{0:l}", password));
    params.put("massUpdate", value.apply("{0:l}", massUpdate));
    params.put("translatable", value.apply("{0:l}", translatable));
    params.put("copyable", value.apply("{0:l}", copy));
    params.put("defaultNow", value.apply("{0:l}", "now".equals(getDefault()) ? "true" : null));

    params.values().removeIf(Objects::isNull);

    if (params.isEmpty()) {
      return null;
    }

    params.entrySet().stream().forEach(e -> widget.param(e.getKey(), e.getValue()));

    return widget;
  }

  private List<JavaAnnotation> $binary(Entity entity) {

    if (isTrue(json) && type == PropertyType.STRING) {
      if (isTrue(encrypted)) {
        throw new IllegalArgumentException(
            String.format(
                "Encryption is not supported on json field: %s.%s", entity.getName(), name));
      }
      return List.of(
          new JavaAnnotation("javax.persistence.Basic")
              .param("fetch", "{0:m}", "javax.persistence.FetchType.LAZY"),
          new JavaAnnotation("org.hibernate.annotations.Type").param("type", "{0:s}", "json"));
    }

    if (type == PropertyType.ENUM) {
      return List.of(
          new JavaAnnotation("javax.persistence.Basic"),
          new JavaAnnotation("org.hibernate.annotations.Type")
              .param("type", "{0:s}", "com.axelor.db.hibernate.type.ValueEnumType"));
    }

    if (isTrue(large) && type == PropertyType.STRING) {
      return List.of(
          new JavaAnnotation("javax.persistence.Lob"),
          new JavaAnnotation("javax.persistence.Basic")
              .param("fetch", "{0:m}", "javax.persistence.FetchType.LAZY"),
          new JavaAnnotation("org.hibernate.annotations.Type")
              .param("type", "{0:s}", isTrue(encrypted) ? "encrypted_text" : "text"));
    }

    if (isTrue(large) || type == PropertyType.BINARY) {
      return List.of(
          new JavaAnnotation("javax.persistence.Lob"),
          new JavaAnnotation("javax.persistence.Basic")
              .param("fetch", "{0:m}", "javax.persistence.FetchType.LAZY"));
    }

    return null;
  }

  private JavaAnnotation $nameColumn(Entity entity) {
    if (isTrue(nameField)
        && (entity == null || entity.getNameField() == null || entity.getNameField() == this)) {
      return new JavaAnnotation("com.axelor.db.annotations.NameColumn");
    }
    return null;
  }

  private List<JavaAnnotation> $virtual() {
    if (!isVirtual() || isBlank(content)) {
      return null;
    }

    List<JavaAnnotation> all = new ArrayList<>();

    all.add(new JavaAnnotation("com.axelor.db.annotations.VirtualColumn"));

    if (isTrue(_transient)) {
      return all;
    }

    if (isTrue(formula)) {

      String text = content;

      text = StringUtils.stripIndent(text); // remove extra indentation
      text = text.replaceAll("(?:^\\s+)|(?:\\s+$)", ""); // remove leading/trailing spaces
      text = text.replaceAll("\"", "\\\\\\\\\""); // escape quotes
      text = text.replaceAll("\n", "\\\\\\\\n\" + \n  \""); // concat multi-line formular
      text = "\"(" + text + ")\"";

      all.add(
          new JavaAnnotation(
                  isReference()
                      ? "org.hibernate.annotations.JoinFormula"
                      : "org.hibernate.annotations.Formula")
              .param("value", "{0:l}", text));
    } else {
      all.add(
          new JavaAnnotation("javax.persistence.Access")
              .param("value", "{0:m}", "javax.persistence.AccessType.PROPERTY"));
    }

    return all;
  }

  private JavaAnnotation $required() {
    return isTrue(required) ? new JavaAnnotation("javax.validation.constraints.NotNull") : null;
  }

  private List<JavaAnnotation> $size() {

    if (isBlank(min) && isBlank(max)) {
      return null;
    }

    final List<JavaAnnotation> all = new ArrayList<>();

    if (type == PropertyType.DECIMAL) {
      if (notBlank(min))
        all.add(
            new JavaAnnotation("javax.validation.constraints.DecimalMin")
                .param("value", "{0:s}", min));
      if (notBlank(max))
        all.add(
            new JavaAnnotation("javax.validation.constraints.DecimalMax")
                .param("value", "{0:s}", max));
      return all;
    }

    if (type == PropertyType.STRING) {
      if (isTrue(encrypted) && notBlank(max) && Integer.valueOf(max) < 256) {
        throw new IllegalArgumentException("Encrypted field size should be more than 255.");
      }
      all.add(
          new JavaAnnotation("javax.validation.constraints.Size")
              .param("min", min)
              .param("max", max));
      return all;
    }

    if (notBlank(min))
      all.add(new JavaAnnotation("javax.validation.constraints.Min").param("value", min));
    if (notBlank(max))
      all.add(new JavaAnnotation("javax.validation.constraints.Max").param("value", max));

    return all;
  }

  private JavaAnnotation $digits() {

    if (precision == null && scale == null) {
      return null;
    }

    if (precision == null) {
      throw new IllegalArgumentException(
          "Invalid use of 'scale' without 'precision' on field: " + name);
    }

    if (scale == null) {
      throw new IllegalArgumentException(
          "Invalid use of 'precision' without 'scale' on field: " + name);
    }

    if (scale > precision) {
      throw new IllegalArgumentException(
          "Invalid 'scale' value, should be less than 'precision' on field: " + name);
    }

    return new JavaAnnotation("javax.validation.constraints.Digits")
        .param("integer", "{0:l}", precision - scale)
        .param("fraction", "{0:l}", scale);
  }

  private JavaAnnotation $transient() {
    return isTrue(getTransient()) ? new JavaAnnotation("javax.persistence.Transient") : null;
  }

  private JavaAnnotation $column() {
    if (Naming.isReserved(name)) {
      throw new IllegalArgumentException("Invalid use of a reserved name: " + name);
    }

    if (isCollection()) {
      return null;
    }

    String col = getColumnAuto();
    if (Naming.isKeyword(col)) {
      throw new IllegalArgumentException("Invalid use of an SQL keyword: " + col);
    }

    if (column == null
        && unique == null
        && nullable == null
        && insertable == null
        && updatable == null) {
      return null;
    }

    JavaAnnotation res =
        new JavaAnnotation(
            isReference() ? "javax.persistence.JoinColumn" : "javax.persistence.Column");

    if (column != null) {
      res.param("name", "{0:s}", column);
    }

    if (unique != null) {
      res.param("unique", "{0:l}", unique);
    }

    if (nullable != null) {
      res.param("nullable", "{0:l}", nullable);
    }

    if (insertable != null) {
      res.param("insertable", "{0:l}", insertable);
    }

    if (updatable != null) {
      res.param("updatable", "{0:l}", updatable);
    }

    return res;
  }

  private JavaAnnotation $one2one() {
    if (type != PropertyType.ONE_TO_ONE) return null;

    JavaAnnotation annotation =
        new JavaAnnotation("javax.persistence.OneToOne")
            .param("fetch", "{0:m}", "javax.persistence.FetchType.LAZY");

    if (mappedBy != null) {
      annotation.param("mappedBy", "{0:s}", mappedBy);
    }

    if (isTrue(getOrphanRemoval())) {
      annotation.param("cascade", "{0:m}", "javax.persistence.CascadeType.ALL");
      annotation.param("orphanRemoval", "true");
    } else {
      annotation.param(
          "cascade",
          List.of("javax.persistence.CascadeType.PERSIST", "javax.persistence.CascadeType.MERGE"),
          t -> new JavaCode("{0:m}", t));
    }
    return annotation;
  }

  private JavaAnnotation $many2one() {
    if (type != PropertyType.MANY_TO_ONE) return null;

    return new JavaAnnotation("javax.persistence.ManyToOne")
        .param("fetch", "{0:m}", "javax.persistence.FetchType.LAZY")
        .param(
            "cascade",
            List.of("javax.persistence.CascadeType.PERSIST", "javax.persistence.CascadeType.MERGE"),
            t -> new JavaCode("{0:m}", t));
  }

  private JavaAnnotation $one2many() {
    if (type != PropertyType.ONE_TO_MANY) return null;

    JavaAnnotation annotation =
        new JavaAnnotation("javax.persistence.OneToMany")
            .param("fetch", "{0:m}", "javax.persistence.FetchType.LAZY");

    if (mappedBy != null) {
      annotation.param("mappedBy", "{0:s}", mappedBy);
    }

    if (isTrue(getOrphanRemoval())) {
      annotation.param("cascade", "{0:m}", "javax.persistence.CascadeType.ALL");
      annotation.param("orphanRemoval", "true");
    } else {
      annotation.param(
          "cascade",
          List.of("javax.persistence.CascadeType.PERSIST", "javax.persistence.CascadeType.MERGE"),
          t -> new JavaCode("{0:m}", t));
    }

    return annotation;
  }

  private JavaAnnotation $many2many() {
    if (type != PropertyType.MANY_TO_MANY) return null;

    JavaAnnotation annotation =
        new JavaAnnotation("javax.persistence.ManyToMany")
            .param("fetch", "{0:m}", "javax.persistence.FetchType.LAZY");

    if (mappedBy != null) {
      annotation.param("mappedBy", "{0:s}", mappedBy);
    }

    annotation.param(
        "cascade",
        List.of("javax.persistence.CascadeType.PERSIST", "javax.persistence.CascadeType.MERGE"),
        t -> new JavaCode("{0:m}", t));

    return annotation;
  }

  private JavaAnnotation $joinTable() {

    String joinTable = table;
    String fk1 = column;
    String fk2 = column2;

    if (joinTable == null) {
      return null;
    }

    JavaAnnotation annotation =
        new JavaAnnotation("javax.persistence.JoinTable").param("name", "{0:s}", joinTable);

    if (fk1 != null) {
      annotation.param(
          "joinColumns",
          new JavaAnnotation("javax.persistence.JoinColumn").param("name", "{0:s}", fk1));
    }

    if (fk2 != null) {
      annotation.param(
          "inverseJoinColumns",
          new JavaAnnotation("javax.persistence.JoinColumn").param("name", "{0:s}", fk2));
    }

    return annotation;
  }

  private JavaAnnotation $orderBy() {
    if (isBlank(orderBy)) return null;

    return new JavaAnnotation("javax.persistence.OrderBy")
        .param("value", "{0:s}", orderBy.replaceAll("-\\s*(\\w+)", "$1 DESC"));
  }

  private JavaAnnotation $sequence() {
    return isBlank(sequence)
        ? null
        : new JavaAnnotation("com.axelor.db.annotations.Sequence")
            .param("value", "{0:s}", sequence);
  }

  private JavaAnnotation $converter() {
    if (notTrue(encrypted) || isTrue(large)) return null;

    String converter =
        isBinary()
            ? "com.axelor.db.converters.EncryptedBytesConverter"
            : "com.axelor.db.converters.EncryptedStringConverter";
    return new JavaAnnotation("javax.persistence.Convert")
        .param("converter", "{0:t}.class", converter);
  }

  private JavaMethod createGetterMethod() {
    final String methodName = getterName(name);
    final String computeName = methodName("compute", name);

    final JavaMethod method = new JavaMethod(methodName, getJavaType(), Modifier.PUBLIC);

    if (isVirtual() && notTrue(formula)) {
      method.code("try {");
      method.code("  {0:l} = {1:l}();", name, computeName);
      method.code("} catch (NullPointerException e) {");
      method.code(
          "  {0:t} logger = {1:t}.getLogger(getClass());",
          "org.slf4j.Logger",
          "org.slf4j.LoggerFactory");
      method.code("  logger.error({0:s}, e);", "NPE in function field: " + methodName + "()");
      method.code("}");
      method.code("return {0:l};", name);
      return method;
    }

    if (isPrimary()) {
      method.code("return {0:l};", name);
      method.annotation(new JavaAnnotation("Override"));
      return method;
    }

    String empty = "".equals(getDefault()) ? null : this.getEmptyValue();

    if (notBlank(empty)) {
      method.code("return {0:l} == null ? {1:l} : {0:l};", name, empty);
    } else {
      method.code("return {0:l};", name);
    }

    if (notBlank(help)) {
      method.doc(new JavaDoc(help).returns("the property value"));
    }

    return method;
  }

  private JavaMethod createSetterMethod() {
    final String methodName = setterName(name);
    final String getterName = getterName(name);

    final JavaMethod method =
        new JavaMethod(methodName, "void", Modifier.PUBLIC).param(name, getJavaType());

    if (isPrimary()) {
      method.annotation(new JavaAnnotation("Override"));
    }

    if (notBlank(mappedBy) && type == PropertyType.ONE_TO_ONE) {
      method.code("if ({0:l}() != null) {", getterName);
      method.code("  {0:l}().{1:l}(null);", getterName, setterName(mappedBy));
      method.code("}");
      method.code("if ({0:l} != null) {", name);
      method.code("  {0:l}.{1:l}(this);", name, setterName(mappedBy));
      method.code("}");
    }

    method.code("this.{0:l} = {0:l};", name);

    return method;
  }

  private JavaMethod createComputeMethod() {
    if (isTrue(formula) || isBlank(content)) return null;

    final String methodName = methodName("compute", name);
    final JavaMethod method = new JavaMethod(methodName, getJavaType(), Modifier.PROTECTED);

    method.code(content.trim());

    return method;
  }

  private JavaMethod createAddMethod() {
    if (!isCollection()) return null;

    final String methodName = methodName("add", getSingularName());
    final String getterName = getterName(name);
    final String setterName = setterName(name);

    final JavaMethod method =
        new JavaMethod(methodName, "void", Modifier.PUBLIC).param("item", getTarget());

    final String collectionType =
        type == PropertyType.MANY_TO_MANY ? "java.util.HashSet" : "java.util.ArrayList";

    method.code("if ({0:l}() == null) {", getterName);
    method.code("  {0:l}(new {1:t}<>());", setterName, collectionType);
    method.code("}");
    method.code("{0:l}().add(item);", getterName);

    if (notBlank(mappedBy) && type == PropertyType.ONE_TO_MANY) {
      method.code("item.{0:l}(this);", setterName(mappedBy));
    }

    final JavaDoc doc =
        new JavaDoc(
            "Add the given {@link {0:t}} item to the {@code {1:l}} collection.", target, name);
    if (notBlank(mappedBy) && type == PropertyType.ONE_TO_MANY) {
      doc.line("<p>");
      doc.line("It sets {@code item.{0:l} = this} to ensure the proper relationship.", mappedBy);
      doc.line("</p>");
    }

    doc.param("item", "the item to add");

    return method.doc(doc);
  }

  private JavaMethod createRemoveMethod() {
    if (!isCollection()) return null;

    final String methodName = methodName("remove", getSingularName());
    final String getterName = getterName(name);

    final JavaMethod method =
        new JavaMethod(methodName, "void", Modifier.PUBLIC).param("item", getTarget());

    method.code("if ({0:l}() == null) {", getterName);
    method.code("  return;");
    method.code("}");
    method.code("{0:l}().remove(item);", getterName);

    if (notBlank(mappedBy) && notTrue(getOrphanRemoval()) && type == PropertyType.ONE_TO_MANY) {
      method.code("item.{0:l}(null);", setterName(mappedBy));
    }

    final JavaDoc doc =
        new JavaDoc(
            "Remove the given {@link {0:t}} item from the {@code {1:l}} collection.", target, name);
    if (notBlank(mappedBy) && notTrue(getOrphanRemoval()) && type == PropertyType.ONE_TO_MANY) {
      doc.line("<p>");
      doc.line("It sets {@code item.{0:l} = null} to break the relationship.", mappedBy);
      doc.line("</p>");
    }

    doc.param("item", "the item to remove");

    return method.doc(doc);
  }

  private JavaMethod createClearMethod() {
    if (!isCollection()) return null;

    final String methodName = methodName("clear", name);
    final String getterName = getterName(name);

    final JavaMethod method = new JavaMethod(methodName, "void", Modifier.PUBLIC);

    method.code("if ({0:l}() != null) {", getterName);

    if (notBlank(mappedBy) && notTrue(getOrphanRemoval()) && type == PropertyType.ONE_TO_MANY) {
      method.code("  {0:l}().forEach(item -> item.{1:l}(null));", getterName, setterName(mappedBy));
    }

    method.code("  {0:l}().clear();", getterName);
    method.code("}");

    final JavaDoc doc = new JavaDoc("Clear the {@code {0:l}} collection.", name);

    if (notBlank(mappedBy) && notTrue(getOrphanRemoval()) && type == PropertyType.ONE_TO_MANY) {
      doc.line("<p>");
      doc.line("It sets {@code item.{0:l} = null} to break the relationship.", mappedBy);
      doc.line("</p>");
      doc.line("<p>");
      doc.line("If you have to query {@link {0:t}} records in same transaction, make", target);
      doc.line("sure to call {@link javax.persistence.EntityManager#flush() } to avoid");
      doc.line("unexpected errors.");
      doc.line("</p>");
    }

    return method.doc(doc);
  }

  @XmlType(name = "string")
  static class StringProperty extends Property {}

  @XmlType(name = "boolean")
  static class BooleanProperty extends Property {}

  @XmlType(name = "integer")
  static class IntegerProperty extends Property {}

  @XmlType(name = "long")
  static class LongProperty extends Property {}

  @XmlType(name = "decimal")
  static class DecimalProperty extends Property {}

  @XmlType(name = "date")
  static class DateProperty extends Property {}

  @XmlType(name = "time")
  static class TimeProperty extends Property {}

  @XmlType(name = "datetime")
  static class DateTimeProperty extends Property {}

  @XmlType(name = "binary")
  static class BinaryProperty extends Property {}

  @XmlType(name = "enum")
  static class EnumProperty extends Property {}

  @XmlType(name = "one-to-one")
  static class OneToOneProperty extends Property {}

  @XmlType(name = "many-to-one")
  static class ManyToOneProperty extends Property {}

  @XmlType(name = "one-to-many")
  static class OneToManyProperty extends Property {}

  @XmlType(name = "many-to-many")
  static class ManyToManyProperty extends Property {}
}
