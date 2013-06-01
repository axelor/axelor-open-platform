package com.axelor.meta.service;

import com.axelor.auth.AuthUtils;
import com.axelor.auth.db.User;
import com.axelor.meta.db.MetaUser;
import com.google.inject.persist.Transactional;

public class MetaUserService {

	@Transactional
	public MetaUser getPreferences() {
		User user = AuthUtils.getUser();
		MetaUser prefs = MetaUser.findByUser(user);
		if (prefs == null) {
			prefs = new MetaUser();
			prefs.setUser(user);
			prefs = prefs.save();
		}
		return prefs;
	}
}
