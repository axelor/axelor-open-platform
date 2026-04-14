/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.meta.service;

import com.axelor.db.JPA;
import com.axelor.db.Query;
import com.axelor.db.mapper.Mapper;
import com.axelor.db.mapper.Property;
import com.axelor.meta.db.MetaField;
import com.axelor.meta.db.MetaModel;
import com.axelor.meta.db.repo.MetaFieldRepository;
import com.axelor.meta.db.repo.MetaModelRepository;
import com.google.inject.persist.Transactional;
import jakarta.inject.Inject;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.hibernate.annotations.Formula;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Keeps {@link MetaModel} and {@link MetaField} records in sync with the registered JPA entity
 * classes.
 */
public class MetaModelService {

  private static final Logger log = LoggerFactory.getLogger(MetaModelService.class);

  @Inject private MetaModelRepository models;

  @Inject private MetaFieldRepository fields;

  /** Synchronizes meta records for every registered entity class. */
  @Transactional
  public void process() {
    final Set<String> existing =
        models.all().select("fullName").fetch(0, 0).stream()
            .map(m -> (String) m.get("fullName"))
            .collect(Collectors.toSet());
    for (Class<?> klass : JPA.models()) {
      final MetaModel entity =
          existing.contains(klass.getName()) ? updateEntity(klass) : createEntity(klass);
      models.save(entity);
    }
  }

  /** Synchronizes the meta record for a single entity class. */
  @Transactional
  public void process(Class<?> klass) {
    if (Modifier.isAbstract(klass.getModifiers())) return;
    final MetaModel entity;
    if (models.all().filter("self.fullName = ?1", klass.getName()).count() == 0) {
      entity = this.createEntity(klass);
    } else {
      entity = this.updateEntity(klass);
    }
    models.save(entity);
  }

  /** Builds a new {@link MetaModel} with its fields for the given entity class. */
  private MetaModel createEntity(Class<?> klass) {

    log.trace("Create entities : {}", klass.getName());

    MetaModel metaModel = new MetaModel();
    metaModel.setName(klass.getSimpleName());
    metaModel.setFullName(klass.getName());
    metaModel.setPackageName(klass.getPackage().getName());

    if (klass.getAnnotation(Table.class) != null) {
      metaModel.setTableName(klass.getAnnotation(Table.class).name());
    }

    metaModel.setMetaFields(new ArrayList<>());
    metaModel.getMetaFields().addAll(this.createFields(metaModel, klass));

    return metaModel;
  }

  /**
   * Refreshes an existing {@link MetaModel}: appends new fields and updates label/description on
   * already-known ones. Fields removed from the class are left in place.
   */
  private MetaModel updateEntity(Class<?> klass) {
    MetaModel metaModel = getMetaModel(klass);
    Mapper mapper = Mapper.of(klass);

    Map<String, MetaField> existing =
        fields.all().filter("self.metaModel = ?1", metaModel).fetch().stream()
            .collect(Collectors.toMap(MetaField::getName, Function.identity()));

    for (Property property : mapper.getProperties()) {
      final MetaField field = existing.get(property.getName());
      if (field == null) {
        MetaField created = createField(metaModel, property);
        if (created != null) {
          metaModel.getMetaFields().add(created);
        }
      } else {
        field.setLabel(property.getTitle());
        field.setDescription(property.getHelp());
      }
    }

    return metaModel;
  }

  /**
   * Builds a {@link MetaField} for the given property. Returns {@code null} when the backing field
   * is missing, synthetic, transient, or annotated with {@link Formula}.
   */
  private MetaField createField(MetaModel metaModel, Property property) {

    Field field = getField(property.getEntity(), property.getName());
    MetaField metaField = null;

    if (field != null
        && !field.isSynthetic()
        && !field.isAnnotationPresent(Formula.class)
        && !Modifier.isTransient(field.getModifiers())) {

      log.trace("Create field : {}", field.getName());

      metaField = new MetaField();

      metaField.setMetaModel(metaModel);
      metaField.setName(field.getName());
      metaField.setTypeName(field.getType().getSimpleName());
      metaField.setJson(property.isJson());

      if (field.getType().getPackage() != null) {
        metaField.setPackageName(field.getType().getPackage().getName());
      }

      metaField.setLabel(property.getTitle());
      metaField.setDescription(property.getHelp());

      if (field.isAnnotationPresent(ManyToOne.class)) {
        metaField.setRelationship(ManyToOne.class.getSimpleName());
      }

      if (field.isAnnotationPresent(ManyToMany.class)) {
        metaField.setRelationship(ManyToMany.class.getSimpleName());
        metaField.setMappedBy(field.getAnnotation(ManyToMany.class).mappedBy());
        metaField.setTypeName(this.getGenericClassName(field));
        metaField.setPackageName(this.getGenericPackageName(field));
      }

      if (field.isAnnotationPresent(OneToMany.class)) {
        metaField.setRelationship(OneToMany.class.getSimpleName());
        metaField.setMappedBy(field.getAnnotation(OneToMany.class).mappedBy());
        metaField.setTypeName(this.getGenericClassName(field));
        metaField.setPackageName(this.getGenericPackageName(field));
      }

      if (field.isAnnotationPresent(OneToOne.class)) {
        metaField.setRelationship(OneToOne.class.getSimpleName());
        metaField.setMappedBy(field.getAnnotation(OneToOne.class).mappedBy());
      }
    }

    return metaField;
  }

  /** Builds all {@link MetaField} rows backing the given entity class. */
  private List<MetaField> createFields(MetaModel metaModel, Class<?> klass) {

    List<MetaField> modelFields = new ArrayList<>();
    Mapper mapper = Mapper.of(klass);

    for (Property property : mapper.getProperties()) {
      MetaField metaField = this.createField(metaModel, property);
      if (metaField != null) {
        modelFields.add(metaField);
      }
    }

    return modelFields;
  }

  /**
   * Returns the raw {@code Type.toString()} of the field's last generic type argument (e.g. {@code
   * "class com.example.Foo"}), or {@code null} if the field is not parameterized.
   */
  private String getGenericCanonicalName(Field field) {

    Type type = field.getGenericType();
    String typeName = null;

    if (type instanceof ParameterizedType parameterizedType) {
      for (Type t : parameterizedType.getActualTypeArguments()) {
        typeName = t.toString();
      }
    }

    return typeName;
  }

  /** Returns the simple class name of the field's generic type argument (e.g. {@code "Foo"}). */
  private String getGenericClassName(Field field) {

    String typeName = this.getGenericCanonicalName(field);

    if (typeName != null) {
      typeName = typeName.replace("class ", "");
      String[] splitName = typeName.split("\\.");
      typeName = splitName[splitName.length - 1];
    }

    return typeName;
  }

  /** Returns the package of the field's generic type argument (e.g. {@code "com.example"}). */
  private String getGenericPackageName(Field field) {

    String typeName = this.getGenericCanonicalName(field);

    if (typeName != null) {
      typeName = typeName.replace("class ", "");
      String[] splitName = typeName.split("\\.");
      typeName = typeName.replace("." + splitName[splitName.length - 1], "");
    }

    return typeName;
  }

  /** Walks the class hierarchy to find a declared field by name. */
  private Field getField(Class<?> klass, String name) {
    if (klass == null) {
      return null;
    }
    try {
      return klass.getDeclaredField(name);
    } catch (NoSuchFieldException e) {
      return getField(klass.getSuperclass(), name);
    }
  }

  /** Returns the {@link MetaModel} record for the given entity class. */
  public static MetaModel getMetaModel(Class<?> klass) {
    return Query.of(MetaModel.class).filter("self.fullName = ?1", klass.getName()).fetchOne();
  }
}
