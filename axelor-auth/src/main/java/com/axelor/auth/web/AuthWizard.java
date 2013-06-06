package com.axelor.auth.web;

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
import com.axelor.auth.db.PermissionWizard;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.google.common.collect.Lists;
import com.google.inject.persist.Transactional;

public class AuthWizard {

	private static Logger log = LoggerFactory.getLogger(AuthWizard.class);
	
	public void generate(ActionRequest request, ActionResponse response) {
		
		PermissionWizard permissionWizard = request.getContext().asType( PermissionWizard.class );		
		response.setFlash( String.format( "%d permissions created", generatePermissions( permissionWizard ).size() ) );
		
	}

	public List<Permission> generatePermissions( PermissionWizard permissionWizard ){

		log.debug("Generate perms for package : {}", permissionWizard.getTargetPackage() );
		
		Set<URL> urls = ClasspathHelper.forPackage( permissionWizard.getTargetPackage() );
		
		Reflections reflections = new Reflections( new ConfigurationBuilder().addUrls( urls ) );
		
		List<Permission> permissions = Lists.newArrayList();
		
		for (Class<?> klass : reflections.getTypesAnnotatedWith( Table.class )) {
			
			String object = klass.getName();
			String[] objectSplit = object.split("\\.");
			String name = permissionWizard.getName() + objectSplit[ objectSplit.length - 1 ];
			
			if ( Permission.findByName(name) == null ) { permissions.add( generatePermission( name, object, permissionWizard ) ); }
			
		}
		
		return permissions;
		
	}
	
	@Transactional
	protected Permission generatePermission( String name, String object, PermissionWizard permissionWizard ){
		
		log.debug("Generate perms for : {}", object);
		
		Permission permission = new Permission();

		permission.setName( name );
		permission.setObject( object );
		
		permission.setCanRead( permissionWizard.getCanRead() );
		permission.setCanWrite( permissionWizard.getCanWrite() );
		permission.setCanCreate( permissionWizard.getCanCreate() );
		permission.setCanRemove( permissionWizard.getCanRemove() );
		
		permission.setReadCondition( permissionWizard.getReadCondition() );
		permission.setReadParams( permissionWizard.getReadParams() );
		
		permission.setWriteCondition( permissionWizard.getWriteCondition() );
		permission.setWriteParams( permissionWizard.getWriteParams() );
		
		permission.setCreateCondition( permissionWizard.getCreateCondition() );
		permission.setCreateParams( permissionWizard.getCreateParams() );
		
		permission.setRemoveCondition( permissionWizard.getRemoveCondition() );
		permission.setRemoveParams( permissionWizard.getRemoveParams() );
		
		for (Group group : permissionWizard.getGroups() ){
			group.addPermission( permission );
		}
		
		return permission.save();
	}
	
}
