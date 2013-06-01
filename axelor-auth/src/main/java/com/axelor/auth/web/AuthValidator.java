package com.axelor.auth.web;

import java.math.BigInteger;
import java.util.Set;

import javax.persistence.Query;

import com.axelor.auth.db.Group;
import com.axelor.auth.db.Permission;
import com.axelor.db.JPA;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;

public class AuthValidator {

	public void validateGroup(ActionRequest request, ActionResponse response) {
		Group group = request.getContext().asType(Group.class);
		if (group.getPermissions() == null) {
			return;
		}
		
		Set<Permission> all = Sets.newHashSet();
		Set<String> unique = Sets.newHashSet();
		Set<String> duplicates = Sets.newHashSet();
		Set<String> notValid = Sets.newHashSet();
		
		for(Permission permission : group.getPermissions()) {
			String object = permission.getObject();
			if (unique.contains(object)) {
				duplicates.add(object);
				continue;
			} else {
				unique.add(object);
			}
			
			Query query = JPA.em().createNativeQuery("SELECT count(*) from meta_model where CONCAT(package_name, '.', name) = ?1");
			query.setParameter(1, object);
			
			BigInteger requestResult = (BigInteger) query.getSingleResult();
			if(requestResult.equals(BigInteger.ZERO)){
				notValid.add(object);
				continue;
			}
			
			all.add(permission);
		}
		
		if (duplicates.size() > 0) {
			response.setFlash(JPA.translate("Only one permission per object is required.")+"<br><br>"+
					JPA.translate("Duplicates found:<br>") + Joiner.on("<br>").join(duplicates));
			response.setValues(ImmutableMap.of("permissions", all));
		}
		else if(notValid.size() >0){
			response.setFlash(JPA.translate("Not a valid object.")+"<br><br>"+
					JPA.translate("Object found:")+"<br>" + Joiner.on("<br>").join(notValid));
			response.setValues(ImmutableMap.of("permissions", all));
		}
	}
}
