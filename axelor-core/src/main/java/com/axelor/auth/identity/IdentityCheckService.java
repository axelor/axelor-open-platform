/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.auth.identity;

import com.axelor.auth.AuthUtils;
import jakarta.annotation.Nullable;
import java.time.LocalDateTime;
import org.apache.shiro.session.Session;

/**
 * Manages an identity-checked session flag for sensitive operations.
 *
 * <p>After a user confirms their identity (password, TOTP, etc.), a short-lived flag is set on the
 * session. Subsequent sensitive operations can check this flag instead of re-prompting.
 */
class IdentityCheckService {

  private static final String IDENTITY_CHECKED_AT = "com.axelor.internal.identityCheckedAt";
  private static final long TTL_MINUTES = 10;

  /**
   * Marks the current session as identity-checked.
   *
   * @throws IllegalStateException if no authenticated user
   */
  public void markIdentityChecked() {
    Session session = getSession();
    if (session == null) {
      throw new IllegalStateException("No authenticated user");
    }
    session.setAttribute(IDENTITY_CHECKED_AT, LocalDateTime.now());
  }

  /**
   * Checks whether the current session has a valid (non-expired) identity check.
   *
   * @return true if identity was checked within the last {@value #TTL_MINUTES} minutes
   */
  public boolean isIdentityChecked() {
    Session session = getSession();
    if (session == null) {
      return false;
    }
    LocalDateTime checkedAt = (LocalDateTime) session.getAttribute(IDENTITY_CHECKED_AT);
    if (checkedAt == null) {
      return false;
    }
    return checkedAt.plusMinutes(TTL_MINUTES).isAfter(LocalDateTime.now());
  }

  /** Clears the identity check flag from the current session. */
  public void clearIdentityCheck() {
    Session session = getSession();
    if (session == null) {
      return;
    }
    session.removeAttribute(IDENTITY_CHECKED_AT);
  }

  @Nullable
  private Session getSession() {
    var subject = AuthUtils.getSubject();
    if (subject == null) {
      return null;
    }

    return subject.getSession(false);
  }
}
