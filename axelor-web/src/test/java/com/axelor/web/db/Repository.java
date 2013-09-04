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
package com.axelor.web.db;

import com.google.inject.persist.Transactional;

public class Repository {

	@Transactional
	public void load() {
		
		if (Title.all().count() > 0) return;
		
		Title[] titles = {
			new Title("mr", "Mr."),
			new Title("mrs", "Mrs."),
			new Title("miss", "Miss")
		};
	
		Contact[] contacts = {
       		new Contact ("John", "Smith", "john.smith@gmail.com", null),
       		new Contact ("Tin", "Tin", "tin.tin@gmail.com", null),
       		new Contact ("Teen", "Teen", "teen.teen@gmail.com", null),
		};

		Address[] addresses = {
       		new Address ("My", "Home", "Paris", "232323"),
       		new Address ("My", "Office", "Paris", "232323")
       	};
		
		contacts[0].setTitle(titles[0]);
		contacts[1].setTitle(titles[1]);
		contacts[2].setTitle(titles[2]);
		
		addresses[0].setContact(contacts[0]);
		addresses[1].setContact(contacts[0]);
		
		for (Title e: titles) e.save();
		for (Contact e : contacts) e.save();
		for (Address e : addresses) e.save();
	}
}
