/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.test.db.repo;

import com.axelor.db.JPA;
import com.axelor.db.JpaRepository;
import com.axelor.test.db.Contact;
import java.util.Map;

public class ContactRepository extends JpaRepository<Contact> {

  public ContactRepository() {
    super(Contact.class);
  }

  public Contact findByEmail(String email) {
    return all().filter("self.email = ?", email).fetchOne();
  }

  public Contact edit(Map<String, Object> values) {
    return JPA.edit(Contact.class, values);
  }

  public Contact manage(Contact contact) {
    return JPA.manage(contact);
  }
}
