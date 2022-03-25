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

import static com.axelor.tools.code.entity.model.Utils.getterName;
import static com.axelor.tools.code.entity.model.Utils.isBlank;
import static com.axelor.tools.code.entity.model.Utils.isFalse;
import static com.axelor.tools.code.entity.model.Utils.isTrue;
import static com.axelor.tools.code.entity.model.Utils.notBlank;
import static com.axelor.tools.code.entity.model.Utils.notEmpty;
import static com.axelor.tools.code.entity.model.Utils.notFalse;
import static com.axelor.tools.code.entity.model.Utils.notTrue;
import static com.axelor.tools.code.entity.model.Utils.stream;

import com.axelor.common.Inflector;
import com.axelor.common.StringUtils;
import com.axelor.tools.code.JavaAnnotation;
import com.axelor.tools.code.JavaCode;
import com.axelor.tools.code.JavaDoc;
import com.axelor.tools.code.JavaField;
import com.axelor.tools.code.JavaMethod;
import com.axelor.tools.code.JavaType;
import java.lang.reflect.Modifier;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlMixed;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;

@XmlType
public class Entity implements BaseType<Entity> {

  private static Set<String> INTERNAL_PACKAGES =
      Set.of("com.axelor.auth.db", "com.axelor.meta.db", "com.axelor.mail.db", "com.axelor.dms.db");

  @XmlMixed private List<String> comments;

  @XmlAttribute(name = "name", required = true)
  private String name;

  @XmlAttribute(name = "table")
  private String table;

  @XmlTransient private String packageName;

  @XmlTransient private String repoPackage;

  @XmlTransient private String tablePrefix;

  @XmlTransient boolean modelClass;

  @XmlTransient boolean dynamicUpdate;

  @XmlTransient Property idField;

  @XmlTransient Property attrsField;

  @XmlTransient Property nameField;

  @XmlAttribute(name = "sequential")
  private Boolean sequential;

  @XmlAttribute(name = "jsonAttrs")
  private Boolean jsonAttrs;

  @XmlAttribute(name = "logUpdates")
  private Boolean auditable;

  @XmlAttribute(name = "equalsIncludeAll")
  private Boolean equalsAll;

  @XmlAttribute(name = "cacheable")
  private Boolean cacheable;

  private Boolean mappedSuperClass;

  @XmlAttribute(name = "implements")
  private String superInterfaces;

  @XmlAttribute(name = "extends")
  private String superClass;

  @XmlAttribute(name = "strategy")
  private String strategy;

  @XmlAttribute(name = "repository")
  private String repositoryType;

  private transient boolean hasExtends;

  @XmlElements({
    @XmlElement(name = "string", type = Property.StringProperty.class),
    @XmlElement(name = "boolean", type = Property.BooleanProperty.class),
    @XmlElement(name = "integer", type = Property.IntegerProperty.class),
    @XmlElement(name = "decimal", type = Property.DecimalProperty.class),
    @XmlElement(name = "long", type = Property.LongProperty.class),
    @XmlElement(name = "date", type = Property.DateProperty.class),
    @XmlElement(name = "time", type = Property.TimeProperty.class),
    @XmlElement(name = "datetime", type = Property.DateTimeProperty.class),
    @XmlElement(name = "binary", type = Property.BinaryProperty.class),
    @XmlElement(name = "enum", type = Property.EnumProperty.class),
    @XmlElement(name = "one-to-one", type = Property.OneToOneProperty.class),
    @XmlElement(name = "many-to-one", type = Property.ManyToOneProperty.class),
    @XmlElement(name = "one-to-many", type = Property.OneToManyProperty.class),
    @XmlElement(name = "many-to-many", type = Property.ManyToManyProperty.class),
  })
  private List<Property> fields;

  @XmlElement(name = "index")
  private List<Index> indexes;

  @XmlElement(name = "unique-constraint")
  private List<UniqueConstraint> constraints;

  @XmlElement(name = "entity-listener")
  private List<EntityListener> listeners;

  @XmlElement(name = "finder-method")
  private List<Finder> finders;

  @XmlElement(name = "track")
  private Track track;

  @XmlElement(name = "extra-code")
  private String extraCode;

  @XmlElement(name = "extra-imports")
  private String extraImports;

  @XmlTransient Entity baseEntity;

  void afterUnmarshal(Unmarshaller unmarshaller, Object parent) {
    final Namespace ns = ((DomainModels) parent).getNamespace();
    packageName = ns.getPackageName();
    repoPackage = ns.getRepoPackageName();
    tablePrefix = ns.getTablePrefix();
    modelClass =
        notBlank(packageName)
            && notBlank(name)
            && packageName.equals("com.axelor.db")
            && name.equals("Model");

    if (modelClass) {
      superClass = null;
      mappedSuperClass = true;
    }

    if (isBlank(tablePrefix)) {
      if (packageName.endsWith(".db")) {
        tablePrefix = packageName.replaceAll("\\.db$", "");
        tablePrefix = tablePrefix.substring(tablePrefix.lastIndexOf(".") + 1);
      } else {
        tablePrefix = ns.getName();
      }
    }

    if (isBlank(table)) {
      table = Inflector.getInstance().underscore(tablePrefix + name).toUpperCase();
    }

    if (isBlank(repoPackage)) {
      repoPackage = packageName + ".repo";
    }

    if (!modelClass) {
      if (isBlank(superClass)) {
        superClass =
            notFalse(auditable) ? "com.axelor.auth.db.AuditableModel" : "com.axelor.db.Model";
        idField = new Property.LongProperty();
        idField.setName("id");
      } else {
        hasExtends = true;
      }
    }

    boolean hasJsonAttrs = getFields().stream().anyMatch(p -> "attrs".equals(p.getName()));
    boolean addJsonAttrs =
        !modelClass
            && !hasJsonAttrs
            && (notFalse(jsonAttrs))
            && (isTrue(jsonAttrs) || !INTERNAL_PACKAGES.contains(packageName));

    if (addJsonAttrs) {
      attrsField = new Property.StringProperty();
      attrsField.setName("attrs");
      attrsField.setTitle("Attributes");
      attrsField.setJson(true);
    }

    dynamicUpdate = getFields().stream().anyMatch(p -> p.isVirtual() && notTrue(p.getTransient()));
    nameField = getFields().stream().filter(p -> isTrue(p.getNameField())).findFirst().orElse(null);

    sequential = notFalse(sequential);
    equalsAll = isTrue(equalsAll);
  }

  private boolean isCompatible(Property existing, Property property) {
    if (existing == null) return true;
    if (existing.isCollection() || isTrue(existing.getTransient()) || existing.isPrimary()) {
      return false;
    }
    if (!Objects.equals(existing.getType(), property.getType())) return false;
    if (!Objects.equals(existing.getTarget(), property.getTarget())) return false;
    if (isTrue(existing.getLarge()) && notTrue(property.getLarge())) return false;
    return true;
  }

  public Property findField(String name) {
    return getFields().stream()
        .filter(p -> Objects.equals(p.getName(), name))
        .findFirst()
        .orElse(null);
  }

  @Override
  public void merge(Entity other) {
    for (Property prop : other.getFields()) {
      Property existing = findField(prop.getName());
      if (isCompatible(existing, prop)) {
        prop.setInitParam(false); // can't be a constructor param
        if (existing != null) {
          fields.remove(existing);
          if (isTrue(existing.getInitParam())) {
            prop.setInitParam(true); // unless existing is a constructor param
          }
        }
        fields.add(prop);
        if (isTrue(prop.getNameField())) {
          nameField = prop;
        }
      }
    }

    getIndexes().addAll(other.getIndexes());
    getConstraints().addAll(other.getConstraints());
    getFinders().addAll(other.getFinders());
    getListeners().addAll(other.getListeners());

    if (other.track != null) {
      if (track == null || isTrue(other.track.getReplace())) {
        track = other.track.copyFor(this);
      } else {
        track.merge(other.track);
      }
    }

    other.baseEntity = this;
    other.repositoryType = this.repositoryType;

    if (other.cacheable != null) {
      cacheable = other.cacheable;
    }

    extraImports =
        Stream.of(extraImports, other.extraImports)
            .filter(Utils::notBlank)
            .collect(Collectors.joining("\n"));

    extraCode =
        Stream.of(extraCode, other.extraCode)
            .filter(Utils::notBlank)
            .collect(Collectors.joining("\n"));
  }

  public boolean addField(Property field) {
    for (Property current : getFields()) {
      if (Objects.equals(current.getName(), field.getName())) {
        return false;
      }
    }
    if (fields == null) {
      fields = new ArrayList<>();
    }
    fields.add(field);
    return true;
  }

  public boolean isModelClass() {
    return modelClass;
  }

  public List<String> getComments() {
    if (comments == null) {
      comments = new ArrayList<>();
    }
    return comments;
  }

  public void setComments(List<String> comments) {
    this.comments = comments;
  }

  public String getName() {
    return name;
  }

  public void setName(String value) {
    this.name = value;
  }

  public String getPackageName() {
    return packageName;
  }

  public String getTable() {
    return table;
  }

  public void setTable(String value) {
    this.table = value;
  }

  public Property getNameField() {
    return nameField;
  }

  public Boolean getSequential() {
    return sequential;
  }

  public void setSequential(Boolean sequential) {
    this.sequential = sequential;
  }

  public Boolean getJsonAttrs() {
    return jsonAttrs;
  }

  public void setJsonAttrs(Boolean jsonAttrs) {
    this.jsonAttrs = jsonAttrs;
  }

  public Boolean getAuditable() {
    return auditable;
  }

  public void setAuditable(Boolean auditable) {
    this.auditable = auditable;
  }

  public Boolean getEqualsAll() {
    return equalsAll;
  }

  public void setEqualsAll(Boolean equalsAll) {
    this.equalsAll = equalsAll;
  }

  public Boolean getCacheable() {
    return cacheable;
  }

  public void setCacheable(Boolean cacheable) {
    this.cacheable = cacheable;
  }

  public Boolean getMappedSuperClass() {
    return mappedSuperClass;
  }

  public void setMappedSuperClass(Boolean mappedSuperClass) {
    this.mappedSuperClass = mappedSuperClass;
  }

  @XmlAttribute(name = "persistable")
  public Boolean getPersistable() {
    return mappedSuperClass == null ? null : !mappedSuperClass;
  }

  public void setPersistable(Boolean persistable) {
    mappedSuperClass = persistable == null ? null : !persistable;
  }

  public void setConstraints(List<UniqueConstraint> constraints) {
    this.constraints = constraints;
  }

  public String getSuperClass() {
    return superClass;
  }

  public void setSuperClass(String superClass) {
    this.superClass = superClass;
  }

  public String getSuperInterfaces() {
    return superInterfaces;
  }

  public void setSuperInterfaces(String superInterfaces) {
    this.superInterfaces = superInterfaces;
  }

  public String getStrategy() {
    return strategy;
  }

  public void setStrategy(String value) {
    this.strategy = value;
  }

  public String getRepoPackage() {
    return repoPackage;
  }

  public String getRepositoryType() {
    return repositoryType;
  }

  public void setRepositoryType(String repositoryType) {
    this.repositoryType = repositoryType;
  }

  public List<Property> getFields() {
    if (fields == null) {
      fields = new ArrayList<>();
    }
    return fields;
  }

  public List<Index> getIndexes() {
    if (indexes == null) {
      indexes = new ArrayList<>();
    }
    return indexes;
  }

  public List<UniqueConstraint> getConstraints() {
    if (constraints == null) {
      constraints = new ArrayList<>();
    }
    return constraints;
  }

  public List<EntityListener> getListeners() {
    if (listeners == null) {
      listeners = new ArrayList<>();
    }
    return listeners;
  }

  public List<Finder> getFinders() {
    if (finders == null) {
      finders = new ArrayList<>();
    }
    return finders;
  }

  public Track getTrack() {
    return track;
  }

  public void setTrack(Track track) {
    this.track = track;
  }

  public String getExtraCode() {
    return extraCode;
  }

  public String getExtraImports() {
    return extraImports;
  }

  private JavaAnnotation $entity() {
    return isTrue(mappedSuperClass) ? null : new JavaAnnotation("javax.persistence.Entity");
  }

  private JavaAnnotation $table() {
    if (isBlank(table) || isTrue(mappedSuperClass)) return null;

    JavaAnnotation annotation =
        new JavaAnnotation("javax.persistence.Table").param("name", "{0:s}", table);

    List<Index> indexes = new ArrayList<>(getIndexes());

    getFields().stream().map(Property::createIndex).filter(Objects::nonNull).forEach(indexes::add);

    annotation.param("uniqueConstraints", getConstraints(), x -> x.toJavaAnnotation(this));
    annotation.param("indexes", indexes, x -> x.toJavaAnnotation(this));

    return annotation;
  }

  private JavaAnnotation $cacheable() {
    if (modelClass || cacheable == null) return null;
    if (isTrue(cacheable)) {
      return new JavaAnnotation("javax.persistence.Cacheable");
    }
    if (isFalse(cacheable)) {
      return new JavaAnnotation("javax.persistence.Cacheable").param("value", "false");
    }
    return null;
  }

  private JavaAnnotation $mappedSuperClass() {
    return isTrue(mappedSuperClass)
        ? new JavaAnnotation("javax.persistence.MappedSuperclass")
        : null;
  }

  private JavaAnnotation $strategy() {
    // Inheritance strategy can be specified on root entity only
    if (isBlank(strategy) || hasExtends) return null;
    String type = "SINGLE_TABLE";
    if (strategy.equals("JOINED")) type = "JOINED";
    if (strategy.equals("CLASS")) type = "TABLE_PER_CLASS";

    return new JavaAnnotation("javax.persistence.Inheritance")
        .param("strategy", "{0:m}", "javax.persistence.InheritanceType." + type);
  }

  private JavaAnnotation $listeners() {
    if (listeners == null || listeners.isEmpty()) return null;
    return new JavaAnnotation("javax.persistence.EntityListeners")
        .param("value", listeners, t -> new JavaCode("{0:t}.class", t.getClazz()));
  }

  private JavaAnnotation $track() {
    return track == null ? null : track.toJavaAnnotation();
  }

  public List<JavaAnnotation> getAnnotations() {
    List<JavaAnnotation> all = new ArrayList<>();

    if (notTrue(mappedSuperClass)) {
      all.add($entity());
      all.add($cacheable());
    }

    if (notTrue(mappedSuperClass) && isTrue(dynamicUpdate)) {
      all.add(new JavaAnnotation("org.hibernate.annotations.DynamicInsert"));
      all.add(new JavaAnnotation("org.hibernate.annotations.DynamicUpdate"));
    }

    all.add($table());
    all.add($strategy());
    all.add($track());
    all.add($mappedSuperClass());
    all.add($listeners());

    return all.stream().filter(Objects::nonNull).collect(Collectors.toList());
  }

  private List<Property> getEqualsIncludes() {
    return getFields().stream()
        .filter(
            p -> {
              if (p.isPrimary() || p.isVersion() || isFalse(p.getEqualsInclude())) return false;
              if (isTrue(p.getEqualsInclude()) || isTrue(p.getUnique())) return true;
              return isTrue(equalsAll) && p.isSimple() && !p.isVirtual();
            })
        .collect(Collectors.toList());
  }

  private List<JavaMethod> toConstructors() {
    JavaMethod m1 = new JavaMethod(name, null, Modifier.PUBLIC);
    JavaMethod m2 = new JavaMethod(name, null, Modifier.PUBLIC);

    if (hasExtends) {
      return List.of(m1);
    }

    List<Property> fields =
        getFields().stream().filter(p -> isTrue(p.getInitParam())).collect(Collectors.toList());
    if (fields.isEmpty()) {
      fields =
          getFields().stream()
              .filter(p -> p.getName().matches("name|code"))
              .collect(Collectors.toList());
    }

    if (fields.isEmpty()) {
      return List.of(m1);
    }

    fields.forEach(
        p -> {
          m2.param(p.getName(), p.getJavaType());
          m2.code("this.{0:l} = {0:l};", p.getName());
        });

    return List.of(m1, m2);
  }

  private JavaMethod createEqualsMethod() {
    final JavaMethod method =
        new JavaMethod("equals", "boolean", Modifier.PUBLIC)
            .annotation(new JavaAnnotation("Override"))
            .param("obj", "Object");

    if (hasExtends) {
      method.code("return {0:t}.equals(this, obj);", "com.axelor.db.EntityHelper");
      return method;
    }

    method.code("if (obj == null) return false;");
    method.code("if (this == obj) return true;");
    method.code("if (!(obj instanceof {0:l})) return false;", getName());
    method.code("");
    method.code("final {0:l} other = ({0:l}) obj;", getName());
    method.code("if (this.getId() != null || other.getId() != null) {");
    method.code("  return {0:t}.equals(this.getId(), other.getId());", "java.util.Objects");
    method.code("}");
    method.code("");

    var data = getEqualsIncludes();

    if (data.isEmpty()) {
      method.code("return false;");
      return method;
    }

    var conditions =
        data.stream()
            .map(p -> p.getName())
            .map(n -> getterName(n))
            .map(n -> String.format("Objects.equals(%s(), other.%s())", n, n))
            .collect(Collectors.joining("\n  && "));

    var nullconditions =
        data.stream()
            .map(p -> p.getName())
            .map(n -> getterName(n))
            .map(n -> String.format("%s() != null", n))
            .collect(Collectors.joining("\n    || "));

    method.code("return " + conditions);
    method.code("  && (" + nullconditions + ");");

    return method;
  }

  private JavaMethod createHashCodeMethod() {
    final JavaMethod method =
        new JavaMethod("hashCode", "int", Modifier.PUBLIC)
            .annotation(new JavaAnnotation("Override"));

    return method.code("return 31;");
  }

  private boolean canToString(Property property) {
    return !property.isPrimary()
        && !property.isVersion()
        && !property.isVirtual()
        && isTrue(property.isSimple())
        && notTrue(property.getJson())
        && notTrue(property.getPassword());
  }

  private JavaMethod createToStringMethod() {
    final JavaMethod method =
        new JavaMethod("toString", "String", Modifier.PUBLIC)
            .annotation(new JavaAnnotation("Override"));

    if (hasExtends) {
      method.code("return {0:t}.toString(this);", "com.axelor.db.EntityHelper");
      return method;
    }

    method.code("return {0:t}.toStringHelper(this)", "com.google.common.base.MoreObjects");
    method.code(" .add({0:s}, getId())", "id");

    getFields().stream()
        .filter(this::canToString)
        .limit(10)
        .forEach(p -> method.code("  .add({0:s}, {1:l}())", p.getName(), getterName(p.getName())));

    method.code("  .omitNullValues()");
    method.code("  .toString();");

    return method;
  }

  @Override
  public JavaType toJavaClass() {
    int modifiers = isModelClass() ? Modifier.PUBLIC | Modifier.ABSTRACT : Modifier.PUBLIC;
    JavaType pojo = JavaType.newClass(name, modifiers);

    if (notBlank(superClass)) pojo.superType(superClass);
    if (notEmpty(superInterfaces)) stream(superInterfaces).forEach(pojo::superInterface);

    List<JavaAnnotation> annotations = getAnnotations();
    List<JavaField> fields = new ArrayList<>();
    List<JavaMethod> methods = new ArrayList<>();

    if (idField != null) {
      fields.add(idField.toJavaField(this));
      methods.addAll(idField.toJavaMethods());
    }

    for (Property property : getFields()) {
      fields.add(property.toJavaField(this));
      methods.addAll(property.toJavaMethods());
    }

    if (attrsField != null) {
      fields.add(attrsField.toJavaField(this));
      methods.addAll(attrsField.toJavaMethods());
    }

    annotations.forEach(pojo::annotation);

    if (modelClass) {
      JavaField id = new JavaField("id", "Long");
      JavaField version = new JavaField("version", "Integer", Modifier.PRIVATE);
      JavaField selected =
          new JavaField("selected", "boolean", Modifier.PRIVATE | Modifier.TRANSIENT);
      JavaField archived = new JavaField("archived", "Boolean", Modifier.PRIVATE);

      version.annotation(new JavaAnnotation("javax.persistence.Version"));
      selected.annotation(new JavaAnnotation("javax.persistence.Transient"));
      archived.annotation(
          new JavaAnnotation("com.axelor.db.annotations.Widget").param("massUpdate", "true"));

      pojo.field(version);
      pojo.field(selected);
      pojo.field(archived);

      pojo.method(id.getGetterMethod().modifiers(Modifier.PUBLIC | Modifier.ABSTRACT));
      pojo.method(id.getSetterMethod().modifiers(Modifier.PUBLIC | Modifier.ABSTRACT));

      pojo.method(version.getGetterMethod());
      pojo.method(version.getSetterMethod());

      pojo.method(selected.getGetterMethod());
      pojo.method(selected.getSetterMethod());

      pojo.method(archived.getGetterMethod());
      pojo.method(archived.getSetterMethod());
    }

    fields.forEach(pojo::field);
    methods.forEach(pojo::method);

    if (!modelClass) {
      toConstructors().forEach(pojo::constructor);
      pojo.method(createEqualsMethod());
      pojo.method(createHashCodeMethod());
      pojo.method(createToStringMethod());
    }

    getComments().stream()
        .filter(s -> notBlank(s))
        .findFirst()
        .ifPresent(
            comment -> {
              String text = StringUtils.stripIndent(comment).trim();
              Iterator<String> iter = Stream.of(text.split("\n", -1)).iterator();
              JavaDoc doc = new JavaDoc(iter.next());
              // skip initial blank lines
              while (iter.hasNext()) {
                String next = iter.next();
                if (notBlank(next)) {
                  doc.line(next);
                  break;
                }
              }
              iter.forEachRemaining(doc::line);
              pojo.doc(doc);
            });

    String extraImports = getExtraImports();
    if (StringUtils.notBlank(extraImports)) {
      pojo.rawImports(extraImports.split("\n"));
    }

    return pojo;
  }

  @Override
  public JavaType toRepoClass() {
    if ("none".equals(repositoryType) || isTrue(mappedSuperClass) || isModelClass()) {
      return null;
    }

    boolean isAbstract = "abstract".equals(repositoryType);
    String modelClassName = getName();
    String modelClass = getPackageName() + "." + getName();
    String baseClass = "com.axelor.db.JpaRepository<" + modelClass + ">";
    String className =
        isAbstract
            ? MessageFormat.format("Abstract{0}Repository", modelClassName)
            : MessageFormat.format("{0}Repository", modelClassName);

    int modifiers = isAbstract ? Modifier.PUBLIC | Modifier.ABSTRACT : Modifier.PUBLIC;

    JavaType pojo =
        JavaType.newClass(className, modifiers)
            .superType(baseClass)
            .constructor(
                new JavaMethod(className, null, Modifier.PUBLIC)
                    .code("super({0:t}.class);", modelClass));

    List<Finder> finders = new ArrayList<>(getFinders());

    boolean hasCodeFinder =
        finders.stream().anyMatch(finder -> "findByCode".equals(finder.getName()));
    boolean hasNameFinder =
        finders.stream().anyMatch(finder -> "findByName".equals(finder.getName()));

    Property code = findField("code");
    Property name = findField("name");

    if (!hasCodeFinder && code != null) pojo.method(new Finder("code").toJavaMethod(this));
    if (!hasNameFinder && name != null) pojo.method(new Finder("name").toJavaMethod(this));

    getFinders().stream()
        .map(finder -> finder.toJavaMethod(this))
        .filter(Objects::nonNull)
        .forEach(pojo::method);

    String extraImports = getExtraImports();
    if (StringUtils.notBlank(extraImports)) {
      pojo.rawImports(extraImports.split("\n"));
    }

    String extraCode = getExtraCode();
    if (StringUtils.notBlank(extraCode)) {
      pojo.rawCode(StringUtils.stripIndent(extraCode).trim());
    }

    return pojo;
  }
}
