package com.axelor.test.db;

import com.axelor.db.JpaRepository;

public class ContactRepository extends JpaRepository<Contact> {

	public ContactRepository() {
		super(Contact.class);
	}

	public Contact findByEmail(String email) {
		return all().filter("self.email = ?", email).fetchOne();
	}
}
