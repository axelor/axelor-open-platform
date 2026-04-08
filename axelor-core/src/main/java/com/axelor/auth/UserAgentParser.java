/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.auth;

import com.axelor.common.StringUtils;
import jakarta.inject.Singleton;
import ua_parser.Client;
import ua_parser.Parser;

/** Parses raw User-Agent header strings into structured {@link UserAgentInfo} objects. */
@Singleton
public class UserAgentParser {

  /**
   * Structured representation of a parsed User-Agent string.
   *
   * @param browser browser family name (e.g. {@code "Chrome"})
   * @param os operating system family name (e.g. {@code "Linux"})
   * @param device device family name (e.g. {@code "Desktop"})
   */
  public record UserAgentInfo(String browser, String os, String device) {}

  private final Parser parser = new Parser();

  /**
   * Parses the given User-Agent string into a {@link UserAgentInfo}.
   *
   * @param userAgent raw {@code User-Agent} header value
   * @return parsed info, or {@code null} if the string is blank
   */
  public UserAgentInfo parse(String userAgent) {
    if (StringUtils.isBlank(userAgent)) {
      return null;
    }
    Client result = parser.parse(userAgent);
    assert result != null; // should never be null
    return new UserAgentInfo(result.userAgent.family, result.os.family, result.device.family);
  }
}
