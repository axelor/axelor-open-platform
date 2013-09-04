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
package com.axelor.meta.service;

import java.net.URL;
import java.util.List;
import java.util.Set;

import javax.persistence.Table;

import org.reflections.Reflections;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.axelor.auth.db.Group;
import com.axelor.auth.db.Permission;
import com.axelor.meta.db.MetaMenu;
import com.axelor.meta.db.MetaAuthProfile;
import com.google.common.collect.Lists;
import com.google.inject.persist.Transactional;

public class MetaAuthProfileService {

	private static Logger log = LoggerFactory.getLogger(MetaAuthProfileService.class);

	public List<Permission> generatePermissions( MetaAuthProfile MetaAuthProfile ){

		log.debug("Generate perms for package : {}", MetaAuthProfile.getTargetPackage() );
		
		Set<URL> urls = ClasspathHelper.forPackage( MetaAuthProfile.getTargetPackage() );
		
		Reflections reflections = new Reflections( new ConfigurationBuilder().addUrls( urls ) );
		
		List<Permission> permissions = Lists.newArrayList();
		
		for (Class<?> klass : reflections.getTypesAnnotatedWith( Table.class )) {
			
			permissions.add( generatePermission( klass, MetaAuthProfile ) );
			
		}
		
		return permissions;
		
	}
	
	@Transactional
	protected Permission generatePermission( Class<?> klass, MetaAuthProfile MetaAuthProfile ){
		
		log.debug("Generate perms for : {}", klass);
		String object = klass.getName();
		
		Permission permission = createPerms(object, MetaAuthProfile);
		groupsAddPermission(permission, MetaAuthProfile.getGroups());
		viewsAddGroups(MetaAuthProfile.getMetaMenus(), MetaAuthProfile.getGroups());
		
		return permission.save();
	}
	
	protected Permission createPerms( String object, MetaAuthProfile MetaAuthProfile ){

		String[] objectSplit = object.split("\\.");
		String name = MetaAuthProfile.getName() + objectSplit[ objectSplit.length - 1 ];
		
		Permission permission = Permission.findByName(name);
		
		if ( permission == null ) {
			
			permission = new Permission();
	
			permission.setName( name );
			permission.setObject( object );
			
			permission.setCanRead( MetaAuthProfile.getCanRead() );
			permission.setCanWrite( MetaAuthProfile.getCanWrite() );
			permission.setCanCreate( MetaAuthProfile.getCanCreate() );
			permission.setCanRemove( MetaAuthProfile.getCanRemove() );
			
			permission.setReadCondition( MetaAuthProfile.getReadCondition() );
			permission.setReadParams( MetaAuthProfile.getReadParams() );
			
			permission.setWriteCondition( MetaAuthProfile.getWriteCondition() );
			permission.setWriteParams( MetaAuthProfile.getWriteParams() );
			
			permission.setCreateCondition( MetaAuthProfile.getCreateCondition() );
			permission.setCreateParams( MetaAuthProfile.getCreateParams() );
			
			permission.setRemoveCondition( MetaAuthProfile.getRemoveCondition() );
			permission.setRemoveParams( MetaAuthProfile.getRemoveParams() );
		
		}
		
		return permission;
	}
	
	protected void groupsAddPermission( Permission permission, Set<Group> groups ){

		for (Group group : groups ){ group.addPermission( permission ); }
		
	}
	
	protected void viewsAddGroups( Set<MetaMenu> menus, Set<Group> groups ){

		for ( MetaMenu menu : menus ){ viewAddGroups(menu, groups); }
		
	}
	
	protected void viewAddGroups( MetaMenu menu, Set<Group> groups ){
		
		if ( menu.getGroups() == null ) { menu.setGroups(groups); }
		else { menu.getGroups().addAll(groups); }
		
		if ( menu.getParent() != null ){
			viewAddGroups(menu.getParent(), groups);
		}
		
	}
	
}
