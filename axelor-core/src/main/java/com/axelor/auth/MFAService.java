/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.auth;

import com.axelor.app.AppSettings;
import com.axelor.app.AvailableAppSettings;
import com.axelor.auth.db.MFA;
import com.axelor.auth.db.MFAMethod;
import com.axelor.auth.db.User;
import com.axelor.auth.db.repo.MFARepository;
import com.axelor.cache.AxelorCache;
import com.axelor.cache.CacheBuilder;
import com.axelor.common.ObjectUtils;
import com.axelor.common.StringUtils;
import com.axelor.i18n.I18n;
import com.axelor.mail.MailException;
import com.axelor.mail.db.MailAddress;
import com.axelor.mail.db.MailMessage;
import com.axelor.mail.service.MailService;
import com.google.inject.Inject;
import com.google.inject.persist.Transactional;
import dev.samstevens.totp.code.CodeGenerator;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.DefaultCodeVerifier;
import dev.samstevens.totp.code.HashingAlgorithm;
import dev.samstevens.totp.exceptions.QrGenerationException;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.qr.QrGenerator;
import dev.samstevens.totp.qr.ZxingPngQrGenerator;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import dev.samstevens.totp.time.TimeProvider;
import jakarta.annotation.Nullable;
import java.security.SecureRandom;
import java.text.MessageFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.concurrent.locks.Lock;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service responsible for managing Multi-Factor Authentication (MFA) for users.
 *
 * <p>This service handles the configuration, verification, and lifecycle of MFA methods, including
 * TOTP-based apps and email-based verification codes as well as providing recovery methods.
 *
 * <p>Core functionalities include:
 *
 * <ul>
 *   <li>Generating TOTP secrets and QR codes
 *   <li>Verifying TOTP tokens and email codes
 *   <li>Sending email codes via the configured mail service
 *   <li>Generating and managing sets of recovery codes
 *   <li>Managing default and valid MFA configurations per user
 *   <li>Testing MFA setups before activation
 * </ul>
 */
public class MFAService {

  public static final HashingAlgorithm TOTP_ALGORITHM = HashingAlgorithm.SHA1;
  public static final int TOTP_DIGITS = 6;
  public static final int TOTP_PERIOD = 30;
  public static final int EMAIL_CODE_VALIDITY = 5;
  public static final Duration EMAIL_SEND_COOL_DOWN = Duration.ofSeconds(60);
  public static final String CHARACTERS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
  public static final int RECOVERY_CODE_PART_LENGTH = 4;

  private final MailService mailService;
  private final MFARepository mfaRepository;

  private static final String CODE_SEPARATOR = " ";

  private static final Logger log = LoggerFactory.getLogger(MFAService.class);

  private static final AxelorCache<String, LocalDateTime> emailRetryAfters =
      CacheBuilder.newBuilder("emailRetryAfters")
          .expireAfterWrite(EMAIL_SEND_COOL_DOWN)
          .tenantAware()
          .build();

  @Inject
  public MFAService(MailService mailService, MFARepository mfaRepository) {
    this.mailService = mailService;
    this.mfaRepository = mfaRepository;
  }

  @Transactional
  public void enableMFA(MFA mfa) {
    mfa.setEnabled(true);
  }

  @Transactional
  public void disableMFA(MFA mfa) {
    removeTOTP(mfa, false);
    removeEmail(mfa, false);
    removeRecoveryCodes(mfa);
    mfa.setDefaultMethod(null);
    mfa.setEnabled(false);
  }

  public List<MFAMethod> getMethods(User user) {
    return getMethods(getRelatedMfa(user));
  }

  public List<MFAMethod> getMethods(MFA mfa) {
    List<MFAMethod> methods = new ArrayList<>();

    if (Boolean.TRUE.equals(mfa.getIsTotpValidated())) {
      methods.add(MFAMethod.TOTP);
    }

    if (Boolean.TRUE.equals(mfa.getIsEmailValidated())) {
      methods.add(MFAMethod.EMAIL);
    }

    // Move default to first position
    MFAMethod defaultMethod = mfa.getDefaultMethod();
    if (defaultMethod != null && methods.remove(defaultMethod)) {
      methods.add(0, defaultMethod);
    }

    return methods;
  }

  public byte[] configureTOTP(MFA mfa) {
    Objects.requireNonNull(mfa, "MFA cannot be null");
    Objects.requireNonNull(mfa.getOwner(), "Owner cannot be null");

    String secret = new DefaultSecretGenerator().generate();
    byte[] qrCode = generateQRCode(secret, mfa.getOwner().getCode());

    mfa.setIsTotpValidated(false);
    mfa.setTotpSecret(secret);

    return qrCode;
  }

  public void removeTOTP(MFA mfa) {
    removeTOTP(mfa, true);
  }

  @Transactional
  protected void removeTOTP(MFA mfa, boolean updateDefault) {
    mfa.setIsTotpValidated(false);
    mfa.setTotpSecret(null);
    if (updateDefault) {
      updatedDefaultMethod(mfa);
    }
  }

  public void removeEmail(MFA mfa) {
    removeEmail(mfa, true);
  }

  @Transactional
  protected void removeEmail(MFA mfa, boolean updateDefault) {
    mfa.setIsEmailValidated(false);
    mfa.setEmail(null);
    mfa.setEmailCode(null);
    if (updateDefault) {
      updatedDefaultMethod(mfa);
    }
  }

  private void updatedDefaultMethod(MFA mfa) {
    mfa.setDefaultMethod(null);
    var methods = getMethods(mfa);

    if (methods.isEmpty()) {
      disableMFA(mfa);
    } else {
      mfa.setDefaultMethod(methods.get(0));
    }
  }

  protected byte[] generateQRCode(String secret, String username) {
    String issuer = AppSettings.get().get(AvailableAppSettings.APPLICATION_NAME, "Axelor");

    try {
      QrData data =
          new QrData.Builder()
              .label(issuer + ":" + username)
              .secret(secret)
              .issuer(issuer)
              .algorithm(TOTP_ALGORITHM)
              .digits(TOTP_DIGITS)
              .period(TOTP_PERIOD)
              .build();
      QrGenerator qrGenerator = new ZxingPngQrGenerator();
      return qrGenerator.generate(data);

    } catch (QrGenerationException e) {
      log.error("QR code generation failed: {}", e.getMessage());
      throw new RuntimeException("QR code generation failed", e);
    }
  }

  public boolean verifyCode(User user, String mfaCode, String mfaMethod) {
    Objects.requireNonNull(user, "User cannot be null");
    Objects.requireNonNull(mfaCode, "MFA code cannot be null");
    Objects.requireNonNull(mfaMethod, "MFA method cannot be null");

    if ("RECOVERY".equalsIgnoreCase(mfaMethod)) {
      return verifyRecoveryCode(mfaCode, user);
    }

    MFAMethod method = null;
    try {
      method = MFAMethod.valueOf(mfaMethod.trim().toUpperCase());
    } catch (IllegalArgumentException e) {
      log.error("Not a known enum value for MFA method: {}", mfaMethod);
    }

    if (method != null) {
      MFA mfa = getRelatedMfa(user);

      return switch (method) {
        case TOTP -> Boolean.TRUE.equals(mfa.getIsTotpValidated()) && verifyTotpCode(mfa, mfaCode);
        case EMAIL ->
            Boolean.TRUE.equals(mfa.getIsEmailValidated()) && verifyEmailCode(mfa, mfaCode);
      };
    }

    return false;
  }

  private boolean verifyTotpCode(MFA mfa, String code) {
    TimeProvider timeProvider = new SystemTimeProvider();
    CodeGenerator codeGenerator = new DefaultCodeGenerator(TOTP_ALGORITHM, TOTP_DIGITS);
    DefaultCodeVerifier verifier = new DefaultCodeVerifier(codeGenerator, timeProvider);
    verifier.setTimePeriod(TOTP_PERIOD);
    return verifier.isValidCode(mfa.getTotpSecret(), code);
  }

  private String generateEmailCode(MFA mfa) {
    AuthService authService = AuthService.getInstance();

    String code = "%06d".formatted(new SecureRandom().nextInt(1000000));
    mfa.setEmailCode(authService.encrypt(code));
    mfa.setEmailCodeExpiresAt(LocalDateTime.now().plusMinutes(EMAIL_CODE_VALIDITY));
    return code;
  }

  private LocalDateTime sendEmail(
      String recipientEmail, String code, User user, boolean isConfirmation) {
    Objects.requireNonNull(recipientEmail, "Recipient email cannot be null");
    Objects.requireNonNull(code, "Code cannot be null");
    Objects.requireNonNull(user, "User cannot be null");

    LocalDateTime retryAfter = checkEmailRetryAfter(user);

    MailAddress address = new MailAddress();
    address.setAddress(recipientEmail);
    address.setPersonal("Axelor");

    MailMessage message = new MailMessage();
    message.addRecipient(address);
    message.setSubject(getMFAEmailSubject(user, isConfirmation, code));
    message.setBody(getMFAEmailBody(user, isConfirmation, code));

    try {
      mailService.send(message);
    } catch (MailException e) {
      throw new IllegalStateException("Email send failure", e);
    }

    return retryAfter;
  }

  @Nullable
  public LocalDateTime getEmailRetryAfter(User user) {
    String userCode = user.getCode();
    return emailRetryAfters.get(userCode);
  }

  private LocalDateTime checkEmailRetryAfter(User user) {
    String userCode = user.getCode();
    Lock lock = emailRetryAfters.getLock(userCode);
    LocalDateTime now = LocalDateTime.now();

    lock.lock();
    try {
      LocalDateTime retryAfter = emailRetryAfters.get(userCode);

      if (retryAfter != null && now.isBefore(retryAfter)) {
        throw new MFATooManyRequestsException(retryAfter);
      }

      retryAfter = now.plus(EMAIL_SEND_COOL_DOWN);
      emailRetryAfters.put(userCode, retryAfter);
      return retryAfter;
    } finally {
      lock.unlock();
    }
  }

  @Transactional
  protected boolean verifyEmailCode(MFA mfa, String code) {
    AuthService authService = AuthService.getInstance();
    LocalDateTime expiresAt = mfa.getEmailCodeExpiresAt();
    String codeHash = mfa.getEmailCode();

    // Clear one-time usage email code after success.
    if (expiresAt != null
        && expiresAt.isAfter(LocalDateTime.now())
        && codeHash != null
        && authService.match(code, codeHash)) {
      mfa.setEmailCode(null);
      mfa.setEmailCodeExpiresAt(null);
      emailRetryAfters.invalidate(mfa.getOwner().getCode());
      return true;
    }

    return false;
  }

  @Transactional
  public LocalDateTime sendEmailCode(User user) {
    if (user == null) {
      throw new IllegalArgumentException("Cannot send authentication code: user not found");
    }

    MFA mfa = getRelatedMfa(user);

    if (!Boolean.TRUE.equals(mfa.getIsEmailValidated())) {
      throw new IllegalStateException(
          "No valid email MFA configuration found for user: " + user.getCode());
    }

    try {
      String code = generateEmailCode(mfa);
      return sendEmail(user.getEmail(), code, user, false);
    } catch (MFATooManyRequestsException e) {
      throw e;
    } catch (Exception e) {
      throw new IllegalStateException("Failed to send MFA email for user: " + user.getCode(), e);
    }
  }

  @Transactional
  public LocalDateTime sendEmailConfirmation(MFA mfa) {
    String code = generateEmailCode(mfa);
    return sendEmail(mfa.getOwner().getEmail(), code, AuthUtils.getUser(), true);
  }

  protected String getMFAEmailSubject(User user, boolean isConfirmation, String code) {
    return getMessage(
        isConfirmation
            ? /*$$(*/ "MFA_EMAIL_SUBJECT_CONFIRM" /*)*/
            : /*$$(*/ "MFA_EMAIL_SUBJECT" /*)*/,
        getAppName(),
        user.getName(),
        code);
  }

  protected String getMFAEmailBody(User user, boolean isConfirmation, String code) {
    return getMessage(
        /*$$(*/ "MFA_EMAIL_BODY" /*)*/,
        user.getName(),
        getMessage(
            isConfirmation
                ? /*$$(*/ "MFA_EMAIL_BODY_CODE_CONFIRM" /*)*/
                : /*$$(*/ "MFA_EMAIL_BODY_CODE" /*)*/,
            getAppName()),
        code,
        EMAIL_CODE_VALIDITY);
  }

  protected String getAppName() {
    return AppSettings.get().get(AvailableAppSettings.APPLICATION_NAME, "Axelor");
  }

  protected String getMessage(String key, Object... arguments) {
    final var message = I18n.get(key);
    if (Objects.equals(key, message)) {
      throw new IllegalStateException("Missing translation for: %s".formatted(key));
    }
    return MessageFormat.format(message, arguments);
  }

  @Transactional
  public void validateMethod(MFA mfa, String code, MFAMethod method) {
    switch (method) {
      case TOTP -> {
        if (verifyTotpCode(mfa, code)) {
          mfa.setIsTotpValidated(true);
        } else {
          throw new IllegalArgumentException("Invalid TOTP code");
        }
      }
      case EMAIL -> {
        if (verifyEmailCode(mfa, code)) {
          mfa.setIsEmailValidated(true);
        } else {
          throw new IllegalArgumentException("Invalid email code");
        }
      }
    }

    if (mfa.getDefaultMethod() == null) {
      mfa.setDefaultMethod(method);
    }
  }

  @Transactional
  public void setDefaultMethod(MFA mfa, MFAMethod method) {
    boolean isValid =
        switch (method) {
          case TOTP -> mfa.getIsTotpValidated();
          case EMAIL -> mfa.getIsEmailValidated();
        };

    if (!isValid) {
      throw new IllegalArgumentException("Invalid method: %s".formatted(method));
    }

    mfa.setDefaultMethod(method);
  }

  @Transactional
  public List<String> generateRecoveryCodes(MFA mfa) {
    AuthService authService = AuthService.getInstance();

    removeRecoveryCodes(mfa);

    List<String> plainRecoveryCodes = new ArrayList<>();
    List<String> recoveryCodes = new ArrayList<>();
    for (int i = 0; i < 10; i++) {
      String plainCode = "%s-%s".formatted(randomPart(), randomPart());
      String encrypted = authService.encrypt(plainCode);

      plainRecoveryCodes.add(plainCode);
      recoveryCodes.add(encrypted);
    }

    mfa.setRecoveryCodes(listToString(recoveryCodes));

    return plainRecoveryCodes;
  }

  @Transactional
  protected void removeRecoveryCodes(MFA mfa) {
    mfa.setRecoveryCodes(null);
  }

  private String randomPart() {
    SecureRandom random = new SecureRandom();
    StringBuilder part = new StringBuilder(RECOVERY_CODE_PART_LENGTH);
    for (int i = 0; i < RECOVERY_CODE_PART_LENGTH; i++) {
      part.append(CHARACTERS.charAt(random.nextInt(CHARACTERS.length())));
    }
    return part.toString();
  }

  @Transactional
  protected boolean verifyRecoveryCode(String code, User user) {
    MFA mfa = getRelatedMfa(user);
    List<String> codes = stringToList(mfa.getRecoveryCodes());

    AuthService authService = AuthService.getInstance();
    OptionalInt matchingIndex =
        IntStream.range(0, codes.size())
            .filter(i -> authService.match(code, codes.get(i)))
            .findAny();

    // Remove used recovery code.
    if (matchingIndex.isPresent()) {
      List<String> remainingCodes = new ArrayList<>(codes);
      remainingCodes.remove(matchingIndex.getAsInt());
      mfa.setRecoveryCodes(listToString(remainingCodes));
      return true;
    }

    return false;
  }

  @Transactional
  public @Nullable MFA getRelatedMfa(User user, boolean create) {
    MFA mfa =
        mfaRepository
            .all()
            .filter("self.owner.id = :userId")
            .bind("userId", user.getId())
            .fetchOne();

    if (mfa == null && create) {
      mfa = new MFA();
      mfa.setOwner(user);
      mfa.setEnabled(false);
      mfa = mfaRepository.save(mfa);
    }

    return mfa;
  }

  public @Nullable MFA getRelatedMfa(User user) {
    return getRelatedMfa(user, true);
  }

  protected List<String> stringToList(String codes) {
    if (StringUtils.isBlank(codes)) {
      return Collections.emptyList();
    }

    return Arrays.stream(codes.split(CODE_SEPARATOR)).toList();
  }

  protected String listToString(List<String> codes) {
    if (ObjectUtils.isEmpty(codes)) {
      return "";
    }

    return codes.stream().collect(Collectors.joining(CODE_SEPARATOR));
  }
}
