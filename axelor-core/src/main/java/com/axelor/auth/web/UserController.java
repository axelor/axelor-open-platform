/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.auth.web;

import com.axelor.auth.MFAService;
import com.axelor.auth.db.User;
import com.axelor.i18n.I18n;
import com.axelor.inject.Beans;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import java.util.Objects;

public class UserController {

  public void onSave(ActionRequest request, ActionResponse response) {
    var mfaService = Beans.get(MFAService.class);
    var user = request.getContext().asType(User.class);
    var mfa = mfaService.getRelatedMfa(user, false);

    if (mfa != null
        && Boolean.TRUE.equals(mfa.getIsEmailValidated())
        && !Objects.equals(user.getEmail(), mfa.getEmail())) {
      mfaService.removeEmail(mfa);

      response.setAlert(
          I18n.get(
              "You have changed your email address. Please reconfigure it for multi-factor"
                  + " authentication."));
    }
  }
}
