package com.axelor.auth;

import javax.inject.Inject;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.axelor.auth.db.PermissionWizard;
import com.axelor.auth.web.AuthWizard;
import com.axelor.test.GuiceModules;
import com.axelor.test.GuiceRunner;

@RunWith(GuiceRunner.class)
@GuiceModules({ TestModule.class })
public class WizardTest {

	@Inject
	AuthWizard authWizard;
	
	@Test
	public void test() {

		PermissionWizard permissionWizard = new PermissionWizard("perms");
		permissionWizard.setTargetPackage("com.axelor.auth.db");
		
		Assert.assertTrue( authWizard.generatePermissions( permissionWizard ).size() == 4 );
	}

}
