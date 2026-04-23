/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.auth;

import com.axelor.auth.db.MFA;
import com.axelor.auth.db.MFAMethod;
import com.axelor.auth.db.User;

/**
 * Lightweight projection of {@link com.axelor.auth.db.MFA} for simple usage; unneeded fields are
 * left out of the projection.
 */
public record MFASummaryDTO(
    Long id,
    Boolean enabled,
    User owner,
    MFAMethod defaultMethod,
    Boolean isTotpValidated,
    Boolean isEmailValidated) {

  @Override
  public Boolean enabled() {
    return enabled == null ? Boolean.FALSE : enabled;
  }

  @Override
  public Boolean isTotpValidated() {
    return isTotpValidated == null ? Boolean.FALSE : isTotpValidated;
  }

  @Override
  public Boolean isEmailValidated() {
    return isEmailValidated == null ? Boolean.FALSE : isEmailValidated;
  }

  public static MFASummaryDTO from(MFA mfa) {
    return new MFASummaryDTO(
        mfa.getId(),
        mfa.getEnabled(),
        mfa.getOwner(),
        mfa.getDefaultMethod(),
        mfa.getIsTotpValidated(),
        mfa.getIsEmailValidated());
  }
}
