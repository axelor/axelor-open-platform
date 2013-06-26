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
import com.axelor.meta.db.MetaProfile;
import com.google.common.collect.Lists;
import com.google.inject.persist.Transactional;

public class MetaProfileService {

	private static Logger log = LoggerFactory.getLogger(MetaProfileService.class);

	public List<Permission> generatePermissions( MetaProfile metaProfile ){

		log.debug("Generate perms for package : {}", metaProfile.getTargetPackage() );
		
		Set<URL> urls = ClasspathHelper.forPackage( metaProfile.getTargetPackage() );
		
		Reflections reflections = new Reflections( new ConfigurationBuilder().addUrls( urls ) );
		
		List<Permission> permissions = Lists.newArrayList();
		
		for (Class<?> klass : reflections.getTypesAnnotatedWith( Table.class )) {
			
			permissions.add( generatePermission( klass, metaProfile ) );
			
		}
		
		return permissions;
		
	}
	
	@Transactional
	protected Permission generatePermission( Class<?> klass, MetaProfile metaProfile ){
		
		log.debug("Generate perms for : {}", klass);
		String object = klass.getName();
		
		Permission permission = createPerms(object, metaProfile);
		groupsAddPermission(permission, metaProfile.getGroups());
		viewsAddGroups(metaProfile.getMetaMenus(), metaProfile.getGroups());
		
		return permission.save();
	}
	
	protected Permission createPerms( String object, MetaProfile metaProfile ){

		String[] objectSplit = object.split("\\.");
		String name = metaProfile.getName() + objectSplit[ objectSplit.length - 1 ];
		
		Permission permission = Permission.findByName(name);
		
		if ( permission == null ) {
			
			permission = new Permission();
	
			permission.setName( name );
			permission.setObject( object );
			
			permission.setCanRead( metaProfile.getCanRead() );
			permission.setCanWrite( metaProfile.getCanWrite() );
			permission.setCanCreate( metaProfile.getCanCreate() );
			permission.setCanRemove( metaProfile.getCanRemove() );
			
			permission.setReadCondition( metaProfile.getReadCondition() );
			permission.setReadParams( metaProfile.getReadParams() );
			
			permission.setWriteCondition( metaProfile.getWriteCondition() );
			permission.setWriteParams( metaProfile.getWriteParams() );
			
			permission.setCreateCondition( metaProfile.getCreateCondition() );
			permission.setCreateParams( metaProfile.getCreateParams() );
			
			permission.setRemoveCondition( metaProfile.getRemoveCondition() );
			permission.setRemoveParams( metaProfile.getRemoveParams() );
		
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
