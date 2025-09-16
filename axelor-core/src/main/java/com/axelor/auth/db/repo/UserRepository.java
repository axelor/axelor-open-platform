/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.auth.db.repo;

import com.axelor.auth.db.User;

public class UserRepository extends AbstractUserRepository {

  @Override
  public User save(User entity) {
    var email = entity.getEmail();
    if (email != null) {
      entity.setEmail(email.toLowerCase());
    }

    return super.save(entity);
  }
}
