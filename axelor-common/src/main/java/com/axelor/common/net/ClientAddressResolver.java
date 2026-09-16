/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.common.net;

import com.axelor.common.StringUtils;
import com.google.common.net.InetAddresses;
import java.net.InetAddress;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * Resolves the IP address of the client that originated a request, from the headers set by a
 * reverse proxy.
 *
 * <p>The client address is the left-most address of a single header: the configured client IP
 * header, or {@code X-Forwarded-For} if none is configured.
 *
 * <p>If this header is missing, or doesn't provide a valid IP address, the address of the direct
 * peer is used: no other header is tried, as the reverse proxy may not overwrite it, so it may have
 * been forged by the client.
 *
 * <p>This header is trusted as it is: the reverse proxy must overwrite it, and the application must
 * only be reachable through the proxy.
 */
public class ClientAddressResolver {

  public static final String X_FORWARDED_FOR = "X-Forwarded-For";

  private static final Pattern PORT_SUFFIX = Pattern.compile("(:\\d{1,5})?");

  private final String clientIpHeader;

  /**
   * Creates a resolver.
   *
   * @param clientIpHeader the header providing the client IP address, or {@code null} to use {@code
   *     X-Forwarded-For}
   */
  public ClientAddressResolver(String clientIpHeader) {
    this.clientIpHeader =
        StringUtils.isBlank(clientIpHeader) ? X_FORWARDED_FOR : clientIpHeader.trim();
  }

  /**
   * Resolves the client address.
   *
   * @param headers returns the value of a request header, or {@code null} if missing
   * @param remoteAddr the address of the direct peer of the connection
   * @return the normalized client address, or {@code remoteAddr} if it can't be resolved from the
   *     headers
   */
  public String resolve(Function<String, String> headers, String remoteAddr) {
    final String clientAddr = parseHeader(headers.apply(clientIpHeader));
    return StringUtils.isBlank(clientAddr) ? remoteAddr : clientAddr;
  }

  /** Parses the left-most address of a header value. */
  private static String parseHeader(String value) {
    if (value == null) {
      return null;
    }
    final int comma = value.indexOf(',');
    final InetAddress address =
        parseAddress((comma < 0 ? value : value.substring(0, comma)).trim());
    return address == null ? null : InetAddresses.toAddrString(address);
  }

  /** Parses an address that may have a port, e.g. {@code 192.0.2.1:1234}, {@code [::1]:1234}. */
  private static InetAddress parseAddress(String entry) {
    String address = entry;

    if (address.startsWith("[")) {
      final int end = address.indexOf(']');
      if (end < 0 || !PORT_SUFFIX.matcher(address.substring(end + 1)).matches()) {
        return null;
      }
      address = address.substring(1, end);
    } else {
      final int colon = address.indexOf(':');
      // a single colon is an IPv4 address with a port, several colons an IPv6 address
      if (colon >= 0 && colon == address.lastIndexOf(':')) {
        if (!PORT_SUFFIX.matcher(address.substring(colon)).matches()) {
          return null;
        }
        address = address.substring(0, colon);
      }
    }

    return IpAddressMatcher.parseAddress(address);
  }
}
