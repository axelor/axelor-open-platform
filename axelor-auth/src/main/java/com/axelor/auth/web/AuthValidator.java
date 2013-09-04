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
