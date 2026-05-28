/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.web.db;

import com.axelor.db.JpaSupport;
import com.google.inject.persist.Transactional;

public class Repository extends JpaSupport {

  @Transactional
  public void load() {

    if (all(Title.class).count() > 0) return;

    Title[] titles = {new Title("mr", "Mr."), new Title("mrs", "Mrs."), new Title("miss", "Miss")};

    Contact[] contacts = {
      new Contact("John", "Smith", "john.smith@gmail.com", null),
      new Contact("Tin", "Tin", "tin.tin@gmail.com", null),
      new Contact("Teen", "Teen", "teen.teen@gmail.com", null),
    };

    Address[] addresses = {
      new Address("My", "Home", "Paris", "232323"), new Address("My", "Office", "Paris", "232323")
    };

    contacts[0].setTitle(titles[0]);
    contacts[1].setTitle(titles[1]);
    contacts[2].setTitle(titles[2]);

    addresses[0].setContact(contacts[0]);
    addresses[1].setContact(contacts[0]);

    for (Contact c : contacts) {
      getEntityManager().persist(c);
    }
  }
}
