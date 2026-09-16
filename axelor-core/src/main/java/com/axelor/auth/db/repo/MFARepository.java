/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.auth.db.repo;

import com.axelor.auth.MFASummaryDTO;
import com.axelor.auth.db.User;
import com.axelor.db.JPA;
import jakarta.persistence.TypedQuery;
import java.util.List;

public class MFARepository extends AbstractMFARepository {

  public MFASummaryDTO findSummaryByOwner(User owner) {
    TypedQuery<MFASummaryDTO> query =
        JPA.em()
            .createQuery(
                "select new "
                    + MFASummaryDTO.class.getName()
                    + "("
                    + "m.id, m.enabled, m.owner, m.defaultMethod, "
                    + "m.isTotpValidated, m.isEmailValidated) "
                    + "from MFA m where m.owner = :owner",
                MFASummaryDTO.class);
    query.setParameter("owner", owner);
    query.setHint("org.hibernate.cacheable", true);
    query.setMaxResults(1);
    List<MFASummaryDTO> result = query.getResultList();
    return result.isEmpty() ? null : result.get(0);
  }

  public MFASummaryDTO findSummaryById(Long id) {
    TypedQuery<MFASummaryDTO> query =
        JPA.em()
            .createQuery(
                "select new "
                    + MFASummaryDTO.class.getName()
                    + "("
                    + "m.id, m.enabled, m.owner, m.defaultMethod, "
                    + "m.isTotpValidated, m.isEmailValidated) "
                    + "from MFA m where m.id = :id",
                MFASummaryDTO.class);
    query.setParameter("id", id);
    query.setHint("org.hibernate.cacheable", true);
    query.setMaxResults(1);
    List<MFASummaryDTO> result = query.getResultList();
    return result.isEmpty() ? null : result.get(0);
  }
}
