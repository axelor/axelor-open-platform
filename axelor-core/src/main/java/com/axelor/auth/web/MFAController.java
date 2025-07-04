/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.auth.web;

import com.axelor.auth.AuthUtils;
import com.axelor.auth.MFAService;
import com.axelor.auth.MFATooManyRequestsException;
import com.axelor.auth.db.MFA;
import com.axelor.auth.db.MFAMethod;
import com.axelor.auth.db.User;
import com.axelor.auth.db.repo.MFARepository;
import com.axelor.auth.db.repo.UserRepository;
import com.axelor.common.ObjectUtils;
import com.axelor.common.StringUtils;
import com.axelor.i18n.I18n;
import com.axelor.meta.schema.actions.ActionView;
import com.axelor.meta.schema.actions.ActionView.ActionViewBuilder;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import jakarta.inject.Inject;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.shiro.authz.UnauthorizedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MFAController {

  @Inject MFAService mfaService;
  @Inject UserRepository userRepository;
  @Inject MFARepository mfaRepository;

  private static final Logger log = LoggerFactory.getLogger(MFAController.class);

  public void enableMFA(ActionRequest request, ActionResponse response) {
    MFA mfa = request.getContext().asType(MFA.class);
    mfa = mfaRepository.find(mfa.getId());
    checkAuthorized(mfa);
    mfaService.enableMFA(mfa);

    if (StringUtils.isBlank(mfa.getRecoveryCodes())) {
      generateAndShowRecoveryCodes(mfa, response);
    }

    response.setReload(true);
    response.setNotify(I18n.get("Multi-factor authentication has been enabled."));
  }

  public void disableMFA(ActionRequest request, ActionResponse response) {
    MFA mfa = request.getContext().asType(MFA.class);
    mfa = mfaRepository.find(mfa.getId());
    checkAuthorized(mfa);
    mfaService.disableMFA(mfa);
    response.setReload(true);
    response.setNotify(I18n.get("Multi-factor authentication has been disabled."));
  }

  public void generateRecoveryCodes(ActionRequest request, ActionResponse response) {
    MFA mfa = request.getContext().asType(MFA.class);
    mfa = mfaRepository.find(mfa.getId());
    checkAuthorized(mfa);
    generateAndShowRecoveryCodes(mfa, response);
  }

  private void generateAndShowRecoveryCodes(MFA mfa, ActionResponse response) {
    List<String> recoveryCodes = mfaService.generateRecoveryCodes(mfa);
    ActionViewBuilder builder =
        ActionView.define("Recovery codes")
            .model(MFA.class.getName())
            .add("form", "mfa-recovery-codes-form")
            .param("popup", "true")
            .param("popup-save", "false")
            .param("show-toolbar", "false")
            .context("_showRecord", mfa.getId())
            .context("_recoveryCodes", recoveryCodes);
    response.setView(builder.map());
    response.setReload(true);
  }

  public void loadRecoveryCodes(ActionRequest request, ActionResponse response) {
    @SuppressWarnings("unchecked")
    List<String> recoveryCodes = (List<String>) request.getContext().get("_recoveryCodes");

    if (ObjectUtils.notEmpty(recoveryCodes)) {
      response.setValue("_recoveryCodes", getRecoveryCodesText(recoveryCodes));
    } else {
      throw new IllegalStateException("Recovery codes not found");
    }
  }

  private String getRecoveryCodesText(List<String> codes) {
    String recsString =
        codes.stream().map(code -> "<li>" + code + "</li>").collect(Collectors.joining("\n"));

    return "<ul>%s</ul>".formatted(recsString);
  }

  public void configureTOTP(ActionRequest request, ActionResponse response) {
    MFA mfa = request.getContext().asType(MFA.class);
    mfa = mfaRepository.find(mfa.getId());
    checkAuthorized(mfa);

    response.setView(
        ActionView.define("TOTP authentication configuration")
            .model(MFA.class.getName())
            .add("form", "mfa-totp-config-form")
            .param("popup", "reload")
            .param("show-toolbar", "false")
            .param("show-confirm", "false")
            .param("popup-save", "false")
            .param("forceEdit", "true")
            .context("_showRecord", mfa.getId())
            .map());
  }

  public void configureEmail(ActionRequest request, ActionResponse response) {
    MFA mfa = request.getContext().asType(MFA.class);
    mfa = mfaRepository.find(mfa.getId());
    checkAuthorized(mfa);

    if (StringUtils.isBlank(mfa.getOwner().getEmail())) {
      response.setError(
          I18n.get(
              "You must have an email address in your user profile to use email confirmation"
                  + " method."));
    } else {
      try {
        mfaService.sendEmailConfirmation(mfa);
      } catch (MFATooManyRequestsException e) {
        log.warn(
            "Email code already sent to {}; can retry after {}",
            mfa.getOwner().getEmail(),
            e.getRetryAfter());
      }

      response.setView(
          ActionView.define("Email configuration")
              .model(MFA.class.getName())
              .add("form", "mfa-email-config-form")
              .param("popup", "reload")
              .param("show-toolbar", "false")
              .param("show-confirm", "false")
              .param("popup-save", "false")
              .param("forceEdit", "true")
              .context("_showRecord", mfa.getId())
              .map());
    }
  }

  public void removeTOTP(ActionRequest request, ActionResponse response) {
    MFA mfa = request.getContext().asType(MFA.class);
    mfa = mfaRepository.find(mfa.getId());
    checkAuthorized(mfa);

    mfaService.removeTOTP(mfa);
    response.setReload(true);
    response.setNotify(I18n.get("Authenticator app method has been removed."));
  }

  public void removeEmail(ActionRequest request, ActionResponse response) {
    MFA mfa = request.getContext().asType(MFA.class);
    mfa = mfaRepository.find(mfa.getId());
    checkAuthorized(mfa);

    mfaService.removeEmail(mfa);
    response.setReload(true);
    response.setNotify(I18n.get("Email confirmation method has been removed."));
  }

  public void loadTOTPConfig(ActionRequest request, ActionResponse response) {
    MFA mfa = request.getContext().asType(MFA.class);
    mfa = mfaRepository.find(mfa.getId());
    checkAuthorized(mfa);

    byte[] qrCode = mfaService.configureTOTP(mfa);
    String qrCodeData = "data:image/png;base64," + Base64.getEncoder().encodeToString(qrCode);

    response.setValues(
        Map.of(
            "_qrCode",
            qrCodeData,
            "_secretKey",
            mfa.getTotpSecret(),
            "isTotpValidated",
            mfa.getIsTotpValidated()));
  }

  public void validateTotpMethod(ActionRequest request, ActionResponse response) {
    MFA mfa = request.getContext().asType(MFA.class);
    mfa = mfaRepository.find(mfa.getId());
    checkAuthorized(mfa);

    String secretKey = (String) request.getContext().get("_secretKey");
    String code = (String) request.getContext().get("_code");
    mfa.setTotpSecret(secretKey);

    validateMethod(response, mfa, code, MFAMethod.TOTP);
    response.setNotify(I18n.get("Authenticator app method has been verified and enabled."));
  }

  public void validateEmailMethod(ActionRequest request, ActionResponse response) {
    MFA mfa = request.getContext().asType(MFA.class);
    mfa = mfaRepository.find(mfa.getId());
    checkAuthorized(mfa);

    String code = (String) request.getContext().get("_code");
    mfa.setEmail(mfa.getOwner().getEmail());

    validateMethod(response, mfa, code, MFAMethod.EMAIL);
    response.setNotify(I18n.get("Email confirmation method has been verified and enabled."));
  }

  private void validateMethod(ActionResponse response, MFA mfa, String code, MFAMethod method) {
    try {
      mfaService.validateMethod(mfa, code, method);
      response.setCanClose(true);
    } catch (IllegalArgumentException e) {
      response.addError("_code", I18n.get("The verification code is invalid."));
    } catch (Exception e) {
      setError(response, I18n.get("An error occurred while validating the configuration."), e);
    }
  }

  public void sendEmailConfirmation(ActionRequest request, ActionResponse response) {
    try {
      MFA mfa = request.getContext().asType(MFA.class);
      mfa = mfaRepository.find(mfa.getId());
      checkAuthorized(mfa);

      mfaService.sendEmailConfirmation(mfa);
      response.setInfo(I18n.get("A new verification code has been sent to your email address."));
    } catch (Exception e) {
      setError(response, I18n.get("Failed to send the verification code"), e);
    }
  }

  public void changeDefaultMethodTOTP(ActionRequest request, ActionResponse response) {
    response.setValue("defaultMethod", MFAMethod.TOTP);
    response.setNotify(I18n.get("Authenticator app method has been selected as default."));
  }

  public void changeDefaultMethodEmail(ActionRequest request, ActionResponse response) {
    response.setValue("defaultMethod", MFAMethod.EMAIL);
    response.setNotify(I18n.get("Email confirmation method has been selected as default."));
  }

  public void showRelatedMfa(ActionRequest request, ActionResponse response) {
    User user = request.getContext().asType(User.class);
    user = userRepository.find(user.getId());
    MFA mfa = mfaService.getRelatedMfa(user);

    response.setView(
        ActionView.define("MFA Configuration")
            .model(MFA.class.getName())
            .add("form", "mfa-form")
            .param("popup", "reload")
            .param("show-toolbar", "false")
            .param("forceEdit", "true")
            .context("_showRecord", mfa.getId())
            .map());
  }

  private void setError(ActionResponse response, String userMessage, Exception e) {
    if (e instanceof MFATooManyRequestsException) {
      response.setError(e.getMessage());
      return;
    }

    log.error(userMessage, e);
    response.setError(userMessage);
  }

  private void checkAuthorized(MFA mfa) {
    var owner = mfa.getOwner();
    var user = AuthUtils.getUser();

    if (user != null && owner != null && !user.equals(owner) && !AuthUtils.isAdmin(user)) {
      throw new UnauthorizedException(I18n.get("You are not authorized to perform this action."));
    }
  }
}
