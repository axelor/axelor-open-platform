/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.common.net;

import com.google.common.net.InetAddresses;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Matches IP addresses against an IP address or a CIDR range.
 *
 * <p>Both IPv4 and IPv6 are supported. IPv4-mapped IPv6 addresses (e.g. {@code ::ffff:192.0.2.1})
 * are handled as their IPv4 equivalent.
 *
 * <p>Only literal IP addresses are accepted: host names are rejected and no DNS lookup is ever
 * performed.
 */
public final class IpAddressMatcher {

  private static final Pattern PREFIX_LENGTH = Pattern.compile("\\d{1,3}");

  private final byte[] network;
  private final int prefixLength;
  private final String text;

  private IpAddressMatcher(InetAddress address, int prefixLength) {
    this.network = mask(address.getAddress(), prefixLength);
    this.prefixLength = prefixLength;

    final String networkText = InetAddresses.toAddrString(toInetAddress(network));
    this.text =
        prefixLength == network.length * Byte.SIZE ? networkText : networkText + "/" + prefixLength;
  }

  /**
   * Creates a matcher from an IP address (e.g. {@code 192.0.2.1}) or a CIDR range (e.g. {@code
   * 192.0.2.0/24}, {@code 2001:db8::/32}).
   *
   * <p>Host bits of a CIDR range are cleared, so {@code 192.0.2.1/24} is the same as {@code
   * 192.0.2.0/24}.
   *
   * @param value the IP address or CIDR range
   * @return the matcher
   * @throws IllegalArgumentException if the value is not a valid IP address or CIDR range
   */
  public static IpAddressMatcher of(String value) {
    final String trimmed = value == null ? "" : value.trim();
    final int slash = trimmed.indexOf('/');
    final InetAddress address = parseAddress(slash < 0 ? trimmed : trimmed.substring(0, slash));

    if (address == null) {
      throw new IllegalArgumentException("Invalid IP address or CIDR range: " + value);
    }

    final int maxPrefixLength = address.getAddress().length * Byte.SIZE;
    if (slash < 0) {
      return new IpAddressMatcher(address, maxPrefixLength);
    }

    final String prefix = trimmed.substring(slash + 1);
    if (!PREFIX_LENGTH.matcher(prefix).matches() || Integer.parseInt(prefix) > maxPrefixLength) {
      throw new IllegalArgumentException("Invalid IP address or CIDR range: " + value);
    }

    return new IpAddressMatcher(address, Integer.parseInt(prefix));
  }

  /**
   * Splits a comma-separated list of IP addresses or CIDR ranges.
   *
   * <p>Entries are trimmed and empty entries are ignored.
   *
   * @param value the list
   * @return the entries, empty if the value is blank
   */
  public static List<String> splitList(String value) {
    if (value == null || value.isBlank()) {
      return List.of();
    }
    return Arrays.stream(value.split(",")).map(String::trim).filter(e -> !e.isEmpty()).toList();
  }

  /**
   * Parses a comma-separated list of IP addresses or CIDR ranges.
   *
   * @param value the list
   * @return the matchers, empty if the value is blank
   * @throws IllegalArgumentException if any entry is not a valid IP address or CIDR range
   */
  public static List<IpAddressMatcher> parseList(String value) {
    return splitList(value).stream().map(IpAddressMatcher::of).toList();
  }

  /**
   * Checks whether an address matches any of the given matchers.
   *
   * @param matchers the matchers
   * @param address the IP address
   * @return {@code true} if the address is a valid IP address matched by any matcher
   */
  public static boolean matchesAny(Collection<IpAddressMatcher> matchers, String address) {
    final InetAddress parsed = parseAddress(address);
    return parsed != null && matchers.stream().anyMatch(matcher -> matcher.matches(parsed));
  }

  /**
   * Parses a literal IP address, without any DNS lookup.
   *
   * <p>IPv6 addresses may be enclosed in brackets and may have a zone index ({@code %eth0}), which
   * is ignored. IPv4-mapped IPv6 addresses are converted to IPv4 addresses.
   *
   * @param value the IP address
   * @return the address, or {@code null} if the value is not a literal IP address
   */
  public static InetAddress parseAddress(String value) {
    if (value == null) {
      return null;
    }

    String address = value.trim();
    if (address.startsWith("[") && address.endsWith("]")) {
      address = address.substring(1, address.length() - 1);
    }

    final int zone = address.indexOf('%');
    if (zone >= 0) {
      address = address.substring(0, zone);
    }

    if (!InetAddresses.isInetAddress(address)) {
      return null;
    }

    return normalize(InetAddresses.forString(address));
  }

  /**
   * Checks whether the address matches this IP address or CIDR range.
   *
   * @param address the IP address
   * @return {@code true} if matching, {@code false} otherwise or if the address is invalid
   */
  public boolean matches(String address) {
    return matches(parseAddress(address));
  }

  /**
   * Checks whether the address matches this IP address or CIDR range.
   *
   * @param address the IP address
   * @return {@code true} if matching, {@code false} otherwise
   */
  public boolean matches(InetAddress address) {
    if (address == null) {
      return false;
    }
    final byte[] bytes = normalize(address).getAddress();
    return bytes.length == network.length && Arrays.equals(mask(bytes, prefixLength), network);
  }

  /** Returns the normalized IP address or CIDR range. */
  @Override
  public String toString() {
    return text;
  }

  private static InetAddress normalize(InetAddress address) {
    if (address instanceof Inet6Address) {
      final byte[] bytes = address.getAddress();
      if (isIPv4Mapped(bytes)) {
        return toInetAddress(Arrays.copyOfRange(bytes, 12, 16));
      }
    }
    return address;
  }

  private static boolean isIPv4Mapped(byte[] bytes) {
    for (int i = 0; i < 10; i++) {
      if (bytes[i] != 0) {
        return false;
      }
    }
    return bytes[10] == (byte) 0xFF && bytes[11] == (byte) 0xFF;
  }

  private static byte[] mask(byte[] bytes, int prefixLength) {
    final byte[] masked = bytes.clone();
    for (int i = 0; i < masked.length; i++) {
      final int bits = Math.clamp(prefixLength - (long) i * Byte.SIZE, 0, Byte.SIZE);
      masked[i] &= (byte) (0xFF << (Byte.SIZE - bits));
    }
    return masked;
  }

  private static InetAddress toInetAddress(byte[] bytes) {
    try {
      // no lookup is performed when creating an address from raw bytes
      return InetAddress.getByAddress(bytes);
    } catch (UnknownHostException e) {
      throw new IllegalArgumentException(e);
    }
  }
}
