package com.axelor.auth.web;

import java.util.Set;

import com.axelor.auth.db.Group;
import com.axelor.auth.db.Permission;
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
		
		for(Permission permission : group.getPermissions()) {
			String object = permission.getObject();
			if (unique.contains(object)) {
				duplicates.add(object);
			} else {
				all.add(permission);
				unique.add(object);
			}
		}
		
		if (duplicates.size() > 0) {
			response.setFlash("Only one permission per object is required.<br><br>"+
							  "Duplicates found:<br>" + Joiner.on("<br>").join(duplicates));
			response.setValues(ImmutableMap.of("permissions", all));
		}
	}
}
