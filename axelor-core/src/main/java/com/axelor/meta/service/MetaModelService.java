/**
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2017 Axelor (<http://axelor.com>).
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
package com.axelor.meta.service;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import org.hibernate.annotations.Formula;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.axelor.db.JPA;
import com.axelor.db.Query;
import com.axelor.db.annotations.Widget;
import com.axelor.meta.db.MetaField;
import com.axelor.meta.db.MetaModel;
import com.axelor.meta.db.repo.MetaFieldRepository;
import com.axelor.meta.db.repo.MetaModelRepository;
import com.google.inject.persist.Transactional;

/**
 * API for MetaModel and MetaField entity.
 * 
 * @author guerrier
 * @version 1.0
 *
 */
public class MetaModelService {
	
	private static Logger log = LoggerFactory.getLogger(MetaModelService.class);
	
	@Inject
	private MetaModelRepository models;
	
	@Inject
	private MetaFieldRepository fields;

	/**
	 * Process to create all MetaModel with MetaField.
	 * 
	 * @see MetaModel
	 * @see MetaField
	 */
	@Transactional
	public void process(){
		for (Class<?> klass : JPA.models()){
			process(klass);
		}
	}
	
	@Transactional
	public void process(Class<?> klass){
		final MetaModel entity;
		if (models.all().filter("fullName = ?1", klass.getName()).count() == 0){
			entity = this.createEntity(klass);
		} else {
			entity = this.updateEntity(klass);
		}
		models.save(entity);
	}
	
	/**
	 * Create MetaModel from Class.
	 * 
	 * @param klass
	 * 		Class to load. 
	 * 
	 * @return
	 * @see MetaModel
	 */
	private MetaModel createEntity(Class<?> klass){
		
		log.trace("Create entities : {}", klass.getName());
		
		MetaModel metaModel = new MetaModel();
		metaModel.setName(klass.getSimpleName());
		metaModel.setFullName(klass.getName());
		metaModel.setPackageName(klass.getPackage().getName());

		if (klass.getAnnotation(Table.class) != null) {
			metaModel.setTableName(klass.getAnnotation(Table.class).name());
		}		
		
		metaModel.setMetaFields(new ArrayList<MetaField>());
		metaModel.getMetaFields().addAll(this.createFields(metaModel, klass));
		
		return metaModel;
	}
	
	/**
	 * Create MetaModel from Class.
	 * 
	 * @param klass
	 * 		Class to load. 
	 * 
	 * @return
	 * @see MetaModel
	 */
	private MetaModel updateEntity(Class<?> klass){
		MetaModel metaModel = getMetaModel(klass);
		for (Field field : klass.getDeclaredFields()){
			if (fields.all().filter("metaModel = ?1 AND name = ?2", metaModel, field.getName()).count() == 0){
				metaModel.getMetaFields().add(createField(metaModel, field));
			}
		}
		return metaModel;
	}
	
	/**
	 * Create MetaField from Field for one MetaModel.
	 * 
	 * @param metaModel
	 * 		MetaModel attachment. 
	 * @param field
	 * 		Field to load. 
	 * 
	 * @return
	 * @see MetaModel
	 * @see MetaField
	 */
	private MetaField createField(MetaModel metaModel, Field field){
		
		MetaField metaField = null;
		
		if (!field.isSynthetic() && !field.isAnnotationPresent(Formula.class)){
			
			log.trace("Create field : {}", field.getName());
			
			metaField = new MetaField();
			
			metaField.setMetaModel(metaModel);
			metaField.setName(field.getName());
			metaField.setTypeName(field.getType().getSimpleName());
			
			if (field.getType().getPackage() != null){
				metaField.setPackageName(field.getType().getPackage().getName());
			}
			
			if (field.isAnnotationPresent(Widget.class)){
				metaField.setLabel(field.getAnnotation(Widget.class).title());
				metaField.setDescription(field.getAnnotation(Widget.class).help());
			}
			
			if (field.isAnnotationPresent(ManyToOne.class)){
				metaField.setRelationship(ManyToOne.class.getSimpleName());
			}
			
			if (field.isAnnotationPresent(ManyToMany.class)){
				metaField.setRelationship(ManyToMany.class.getSimpleName());
				metaField.setMappedBy(field.getAnnotation(ManyToMany.class).mappedBy());
				metaField.setTypeName(this.getGenericClassName(field));
				metaField.setPackageName(this.getGenericPackageName(field));
			}
			
			if (field.isAnnotationPresent(OneToMany.class)){
				metaField.setRelationship(OneToMany.class.getSimpleName());
				metaField.setMappedBy(field.getAnnotation(OneToMany.class).mappedBy());
				metaField.setTypeName(this.getGenericClassName(field));
				metaField.setPackageName(this.getGenericPackageName(field));
			}
			
			if (field.isAnnotationPresent(OneToOne.class)){
				metaField.setRelationship(OneToOne.class.getSimpleName());
				metaField.setMappedBy(field.getAnnotation(OneToOne.class).mappedBy());
			}
		}
		
		return metaField;
	}
	
	/**
	 * Create all ModelFields from Class for one MetaModel.
	 * 
	 * @param metaModel
	 * 		MetaModel attachment.
	 * @param klass
	 * 		Class to load. 
	 * 
	 * @return
	 * @see MetaModel
	 * @see MetaField
	 */
	private List<MetaField> createFields(MetaModel metaModel, Class<?> klass){
		
		List<MetaField> modelFields = new ArrayList<MetaField>();
		MetaField metaField = new MetaField();
		
		for (Field field : klass.getDeclaredFields()){
			metaField = this.createField(metaModel, field);
			if (metaField != null){
				modelFields.add(metaField);
			}
		}
		
		return modelFields;
	}
	
	/**
	 * Get canonical name from generic type in field.
	 * 
	 * @param field
	 * 		One field with generic type.
	 * 
	 * @return
	 * 		The canonical name of the generic type.
	 */
	private String getGenericCanonicalName(Field field){
		
		Type type = field.getGenericType();
		String typeName = null;
		
		if (type instanceof ParameterizedType) {  
            ParameterizedType pt = (ParameterizedType) type;  
            for (Type t : pt.getActualTypeArguments()) {
            	typeName = t.toString();  
            }  
        }
		
		return typeName;
	}
	
	/**
	 * Get class name from generic type in field.
	 * 
	 * @param field
	 * 		One field with generic type.
	 * 
	 * @return
	 * 		The class name of the generic type.
	 */
	private String getGenericClassName(Field field){
		
		String typeName = this.getGenericCanonicalName(field);
		
		if (typeName != null){
			typeName = typeName.replace("class ", "");
			String[] splitName = typeName.split("\\.");
			typeName = splitName[splitName.length - 1];
		}
		
		return typeName;
	}
	
	/**
	 * Get package name from generic type in field.
	 * 
	 * @param field
	 * 		One field with generic type.
	 * 
	 * @return
	 * 		The package name of the generic type.
	 */
	private String getGenericPackageName(Field field){
		
		String typeName = this.getGenericCanonicalName(field);
		
		if (typeName != null){
			typeName = typeName.replace("class ", "");
			String[] splitName = typeName.split("\\.");
			typeName = typeName.replace("."+splitName[splitName.length - 1], "");
		}
		
		return typeName;
	}
	
	/**
	 * Get metaModel from Class
	 * 
	 * @param klass
	 * @return
	 */
	public static MetaModel getMetaModel(Class<?> klass){
		return Query.of(MetaModel.class).filter("self.fullName = ?1", klass.getName()).fetchOne();
	}
}
