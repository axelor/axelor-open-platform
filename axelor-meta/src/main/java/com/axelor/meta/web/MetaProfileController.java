package com.axelor.meta.web;

import javax.inject.Inject;

import com.axelor.meta.db.MetaProfile;
import com.axelor.meta.service.MetaProfileService;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;

public class MetaProfileController {

	@Inject
	protected MetaProfileService metaProfileService;
	
	public void generate(ActionRequest request, ActionResponse response) {
		
		MetaProfile metaProfile = request.getContext().asType( MetaProfile.class );		
		response.setFlash( String.format( "%d permissions created", metaProfileService.generatePermissions( metaProfile ).size() ) );
		
	}
}
