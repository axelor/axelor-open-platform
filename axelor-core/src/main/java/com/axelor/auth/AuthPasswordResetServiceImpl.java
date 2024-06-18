/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2025 Axelor (<http://axelor.com>).
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.axelor.auth;

import com.axelor.app.AppSettings;
import com.axelor.app.AvailableAppSettings;
import com.axelor.auth.db.PasswordResetToken;
import com.axelor.auth.db.User;
import com.axelor.auth.db.repo.PasswordResetTokenRepository;
import com.axelor.auth.db.repo.UserRepository;
import com.axelor.common.StringUtils;
import com.axelor.db.tenants.TenantConfig;
import com.axelor.db.tenants.TenantModule;
import com.axelor.db.tenants.TenantResolver;
import com.axelor.i18n.I18n;
import com.axelor.mail.MailException;
import com.axelor.mail.db.MailAddress;
import com.axelor.mail.db.MailMessage;
import com.axelor.mail.service.MailService;
import com.google.inject.Provider;
import com.google.inject.persist.Transactional;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import java.lang.invoke.MethodHandles;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Base64;
import java.util.Objects;
import java.util.UUID;
import org.apache.shiro.web.util.WebUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Service for password reset. */
public class AuthPasswordResetServiceImpl implements AuthPasswordResetService {

  protected final Provider<MailService> mailService;

  protected final Provider<UserRepository> userRepository;
  protected final Provider<PasswordResetTokenRepository> tokenRepository;

  protected final boolean enabled;

  protected static final AppSettings settings = AppSettings.get();

  protected static final Logger logger =
      LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Inject
  public AuthPasswordResetServiceImpl(
      Provider<MailService> mailService,
      Provider<UserRepository> userRepository,
      Provider<PasswordResetTokenRepository> tokenRepository) {
    this.mailService = mailService;
    this.userRepository = userRepository;
    this.tokenRepository = tokenRepository;
    this.enabled = computeEnabled();
  }

  @Override
  public boolean isEnabled() {
    return enabled;
  }

  protected boolean computeEnabled() {
    // In production, check base URL configuration
    if (settings.isProduction()
        && !TenantModule.isEnabled()
        && StringUtils.isBlank(settings.get(AvailableAppSettings.APPLICATION_BASE_URL))) {
      return false;
    }

    return settings.getBoolean(AvailableAppSettings.APPLICATION_RESET_PASSWORD_ENABLED, true);
  }

  @Override
  @Transactional
  public void submitForgotPassword(String emailAddress) throws MailException {
    final var user = userRepository.get().findByEmail(emailAddress);

    if (user == null) {
      logger.info("User with email address \"{}\" not found", emailAddress);
      return;
    }

    consumeTokens(user);
    sendEmail(user);
  }

  @Override
  public void checkToken(String token) {
    getValidToken(token);
  }

  @Override
  @Transactional
  public void changePassword(String token, String password) {
    final var resetPasswordToken = getValidToken(token);
    final var user = resetPasswordToken.getUser();
    final var authService = AuthService.getInstance();

    // Check whether new password is the same as current one.
    if (authService.match(password, user.getPassword())) {
      throw new IllegalArgumentException(I18n.get("New password must be different."));
    }

    authService.changePassword(user, password);
    user.setForcePasswordChange(false);
    resetPasswordToken.setConsumed(true);
  }

  /**
   * Consumes existing password reset tokens for the given user.
   *
   * @param user the user whose password needs to be reset
   */
  @Transactional
  protected void consumeTokens(User user) {
    final var updated =
        tokenRepository
            .get()
            .all()
            .filter("self.user.id = :userId AND COALESCE(self.consumed, FALSE) = FALSE")
            .bind("userId", user.getId())
            .update("consumed", true);

    if (updated != 0) {
      logger.info(
          "Consumed {} unused password reset tokens for user \"{}\"", updated, user.getCode());
    }
  }

  /**
   * Sends a password reset email to the given user.
   *
   * @param user the user whose password needs to be reset
   * @throws MailException
   */
  @Transactional
  protected void sendEmail(User user) throws MailException {

    final var address = new MailAddress();
    address.setAddress(user.getEmail());
    address.setPersonal(user.getName());

    final var subject = getEmailSubject(user);
    final var resetUrl = createResetUrl(user);
    final var body = getEmailBody(user, resetUrl);

    final var message = new MailMessage();
    message.addRecipient(address);
    message.setSubject(subject);
    message.setBody(body);

    mailService.get().send(message);

    logger.info(
        "Password reset email sent to \"{}\" for user \"{}\"", user.getEmail(), user.getCode());
  }

  /**
   * Gets the subject of the password reset email.
   *
   * @param user the user whose password needs to be reset
   * @return the subject of the email
   */
  protected String getEmailSubject(User user) {
    return MessageFormat.format(
        getMessage(/*$$(*/ "PASSWORD_RESET_EMAIL_SUBJECT" /*)*/), user.getName());
  }

  /**
   * Gets the body of the password reset email.
   *
   * @param user the user whose password needs to be reset
   * @param resetUrl the password reset URL
   * @return the body of the email
   */
  protected String getEmailBody(User user, String resetUrl) {
    return MessageFormat.format(
        getMessage(/*$$(*/ "PASSWORD_RESET_EMAIL_BODY" /*)*/),
        user.getName(),
        user.getCode(),
        getBaseUrl(),
        resetUrl,
        getMaxAgeHours());
  }

  protected String getMessage(String key) {
    final var message = I18n.get(key);
    if (Objects.equals(key, message)) {
      throw new IllegalStateException(String.format("Missing translation for: %s", key));
    }
    return message;
  }

  /**
   * Creates a password reset URL.
   *
   * @param user the user whose password needs to be reset
   * @return the password reset URL
   */
  protected String createResetUrl(User user) {
    final var url =
        new StringBuilder()
            .append(
                String.format("%s/#/reset-password?token=%s", getBaseUrl(true), createToken(user)));
    final var httpRequest = getHttpRequest();
    final var headerTenantId = httpRequest.getHeader("X-Tenant-ID");

    if (StringUtils.notBlank(headerTenantId)) {
      url.append("&tenant=").append(headerTenantId);
    }

    return url.toString();
  }

  /**
   * Creates a password reset token.
   *
   * @param user the user whose password needs to be reset
   * @return the password reset token
   * @throws IllegalStateException if the user is not active
   */
  @Transactional
  protected String createToken(User user) {
    if (!AuthUtils.isActive(user)) {
      throw new IllegalStateException("User is not active");
    }

    final var token = UUID.randomUUID().toString();
    final var expiry = LocalDateTime.now().plusHours(getMaxAgeHours());
    final var passwordResetToken = new PasswordResetToken(user, hash(token), expiry);

    tokenRepository.get().save(passwordResetToken);

    return token;
  }

  /**
   * Gets a password reset token record if it is valid.
   *
   * <p>Checks whether it exists, not archived, not consumed, not expired, and user is active.
   *
   * @param token the password reset token
   * @return the password reset token record
   * @throws IllegalArgumentException if the token is invalid
   */
  protected PasswordResetToken getValidToken(String token) {
    final var passwordResetToken = tokenRepository.get().findByToken(hash(token));

    if (passwordResetToken != null
        && !Boolean.TRUE.equals(passwordResetToken.getArchived())
        && !Boolean.TRUE.equals(passwordResetToken.getConsumed())
        && passwordResetToken.getExpireAt().isAfter(LocalDateTime.now())
        && AuthUtils.isActive(passwordResetToken.getUser())) {
      return passwordResetToken;
    }

    throw new IllegalArgumentException(
        I18n.get("Your password reset token is invalid or expired."));
  }

  /**
   * Gets the maximum age of a password reset token in hours.
   *
   * @return the maximum age in hours
   */
  protected static int getMaxAgeHours() {
    return settings.getInt(AvailableAppSettings.APPLICATION_RESET_PASSWORD_MAX_AGE, 24);
  }

  /**
   * Gets the base URL of the application.
   *
   * <p>We cannot trust request host header because of host header injection attacks. We use either
   * `application.base-url` or current tenant `hosts` configuration.
   *
   * @return the base URL
   */
  protected String getBaseUrl(boolean trimSlash) {
    var url =
        settings.isProduction()
            ? settings.get(AvailableAppSettings.APPLICATION_BASE_URL)
            : settings.getBaseURL();
    final var tenantId = TenantResolver.currentTenantIdentifier();

    if (StringUtils.notBlank(tenantId)
        && !Objects.equals(tenantId, TenantConfig.DEFAULT_TENANT_ID)) {
      final var tenantBaseUrl = getTenantBaseUrl(tenantId);
      if (StringUtils.notBlank(tenantBaseUrl)) {
        url = tenantBaseUrl;
      }
    }

    if (StringUtils.isBlank(url)) {
      throw new IllegalArgumentException("Application base URL is not set");
    }

    if (trimSlash && url != null && url.endsWith("/")) {
      url = url.substring(0, url.length() - 1);
    }

    return url;
  }

  protected String getBaseUrl() {
    return getBaseUrl(false);
  }

  @Nullable
  protected String getTenantBaseUrl(String tenantId) {
    final var hostsKey = String.format("db.%s.hosts", tenantId);
    final var hostsValue = settings.get(hostsKey);
    if (StringUtils.notBlank(hostsValue)) {
      final var hosts = Arrays.asList(hostsValue.split("\\s*,\\s*"));
      if (!hosts.isEmpty()) {
        final var host = hosts.get(0);
        if (StringUtils.notBlank(host)) {
          final var httpRequest = getHttpRequest();
          return String.format(
              "%s://%s%s", httpRequest.getScheme(), host, httpRequest.getContextPath());
        }
      }
    }

    return null;
  }

  protected HttpServletRequest getHttpRequest() {
    return WebUtils.getHttpRequest(AuthUtils.getSubject());
  }

  protected String hash(String token) {
    try {
      final var digest = MessageDigest.getInstance("SHA-256");
      final var bytes = digest.digest(token.getBytes(StandardCharsets.UTF_8));
      return Base64.getEncoder().encodeToString(bytes);
    } catch (final NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }
  }
}
