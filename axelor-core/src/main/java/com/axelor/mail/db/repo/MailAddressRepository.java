/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.mail.db.repo;

import com.axelor.db.JpaRepository;
import com.axelor.mail.db.MailAddress;

public class MailAddressRepository extends JpaRepository<MailAddress> {

  public MailAddressRepository() {
    super(MailAddress.class);
  }

  public MailAddress findByEmail(String email) {
    return all().filter("self.address = :email").bind("email", email).cacheable().fetchOne();
  }

  public MailAddress findOrCreate(String email) {
    return findOrCreate(email, email);
  }

  public MailAddress findOrCreate(String email, String displayName) {
    MailAddress address = findByEmail(email);
    if (address == null) {
      address = new MailAddress();
      address.setAddress(email);
      address.setPersonal(displayName);
      save(address);
    }
    return address;
  }
}
