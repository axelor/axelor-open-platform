/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.auth.db.repo;

import com.axelor.auth.db.MFA;
import com.axelor.db.JpaSecurity;
import com.axelor.inject.Beans;

public class MFARepository extends AbstractMFARepository {
  public boolean isPermitted() {
    return Beans.get(JpaSecurity.class).isPermitted(JpaSecurity.CAN_READ, MFA.class);
  }
}
