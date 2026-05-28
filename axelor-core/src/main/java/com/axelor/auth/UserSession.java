/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.auth;

import com.axelor.auth.UserAgentParser.UserAgentInfo;

/**
 * Represents an active user session
 *
 * @param id session identifier
 * @param loginDate epoch milliseconds of the initial authentication, or {@code 0} if unavailable
 * @param lastAccess epoch milliseconds of the last request made on this session, or {@code 0} if
 *     unavailable
 * @param current {@code true} if this session belongs to the currently connected user
 * @param device device and network information associated with this session
 */
public record UserSession(
    String id, long loginDate, long lastAccess, boolean current, Device device) {

  /**
   * Device and network information
   *
   * @param remoteIp IP address of the client
   * @param browser browser family name (e.g. {@code "Chrome"}), or {@code null} if unknown
   * @param os operating system family name (e.g. {@code "Linux"}), or {@code null} if unknown
   * @param device device family name (e.g. {@code "Desktop"}), or {@code null} if unknown
   */
  public record Device(String remoteIp, String browser, String os, String device) {

    /**
     * Creates a {@link Device} from a remote IP and a parsed User-Agent.
     *
     * @param remoteIp client IP address
     * @param ua parsed User-Agent info
     * @return a fully populated {@link Device}
     */
    public static Device of(String remoteIp, UserAgentInfo ua) {
      return new Device(remoteIp, ua.browser(), ua.os(), ua.device());
    }

    /**
     * Creates a {@link Device} with only the IP address, when User-Agent parsing is unavailable.
     *
     * @param remoteIp client IP address
     * @return a {@link Device} with {@code null} browser, os, and device fields
     */
    public static Device ofIpOnly(String remoteIp) {
      return new Device(remoteIp, null, null, null);
    }
  }
}
