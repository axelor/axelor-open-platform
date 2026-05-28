/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.test.db.repo;

import com.axelor.db.JpaRepository;
import com.axelor.test.db.Person;
import jakarta.validation.ValidationException;

public class PersonRepository extends JpaRepository<Person> {

  public PersonRepository() {
    super(Person.class);
  }

  public Person findByCode(String code) {
    return all().filter("self.code = ?", code).cacheable().fetchOne();
  }

  @Override
  public Person save(Person entity) {
    if (entity.getCode().equals("my-unique-code2")) {
      throw new ValidationException();
    }
    return super.save(entity);
  }
}
