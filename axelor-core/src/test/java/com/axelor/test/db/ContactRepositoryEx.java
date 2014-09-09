package com.axelor.test.db;

public class ContactRepositoryEx extends ContactRepository {

	public Contact findByEmail(String email) {
		return all().filter("self.email = ?", email).fetchOne();
	}
}
