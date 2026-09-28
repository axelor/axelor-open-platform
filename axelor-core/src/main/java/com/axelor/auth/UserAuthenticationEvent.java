/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.auth;

/**
 * Represents an authentication event of a user, for display.
 *
 * @param message sentence describing how the user authenticated
 * @param status authentication status name (e.g. {@code "SUCCESS"})
 * @param authDate epoch milliseconds of the authentication, or {@code 0} if unavailable
 * @param device device and network information associated with this authentication
 */
public record UserAuthenticationEvent(
    String message, String status, long authDate, UserSession.Device device) {}
