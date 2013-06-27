package com.axelor.meta.web;

import javax.inject.Inject;

import com.axelor.meta.db.MetaAuthProfile;
import com.axelor.meta.service.MetaAuthProfileService;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;

public class MetaAuthProfileController {

	@Inject
	protected MetaAuthProfileService metaAuthProfileService;
	
	public void generate(ActionRequest request, ActionResponse response) {
		
		MetaAuthProfile metaProfile = request.getContext().asType( MetaAuthProfile.class );		
		response.setFlash( String.format( "%d permissions created", metaAuthProfileService.generatePermissions( metaProfile ).size() ) );
		
	}
}
