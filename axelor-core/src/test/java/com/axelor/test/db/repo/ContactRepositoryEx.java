/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.test.db.repo;

import com.axelor.test.db.Contact;

public class ContactRepositoryEx extends ContactRepository {

  public Contact findByEmail(String email) {
    return all().filter("self.email = ?", email).fetchOne();
  }
}
