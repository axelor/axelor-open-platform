/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.dms.db.repo;

import com.axelor.db.JpaRepository;
import com.axelor.dms.db.DMSFileTag;

public class DMSFileTagRepository extends JpaRepository<DMSFileTag> {

  public DMSFileTagRepository() {
    super(DMSFileTag.class);
  }
}
