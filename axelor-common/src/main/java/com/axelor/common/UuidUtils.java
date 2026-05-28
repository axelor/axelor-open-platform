/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.common;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.UUID;

/**
 * Utility class for generating and handling UUIDs (Universally Unique Identifiers).
 *
 * <p>Supports generation of version 4 (random) and version 7 (time-based) UUIDs, as well as methods
 * to check UUID versions and extract timestamps from version 7 UUIDs.
 */
public class UuidUtils {

  private static final java.util.Random random = new SecureRandom();

  /**
   * Generates a version 4 (random) UUID.
   *
   * @return a version 4 UUID
   */
  public static UUID v4() {
    return UUID.randomUUID();
  }

  /**
   * Generates a version 7 (time-based) UUID.
   *
   * @return a version 7 UUID
   */
  public static UUID v7() {
    var unixMillis = Instant.now().toEpochMilli();

    // --- 48 bits timestamp ---
    var ms = unixMillis & 0xFFFFFFFFFFFFL;

    // --- random bits ---
    var randA = random.nextInt(1 << 12); // 12 bits
    var randB = random.nextLong() & 0x3FFFFFFFFFFFFFFFL; // 62 bits

    // Construct MSB (timestamp + version 7)
    var msb =
        (ms << 16)
            | 0x7000L // version = 7 (bits 12–15)
            | randA;

    // Construct LSB (variant + 62 random bits)
    var lsb = (0x8000000000000000L) | randB; // variant 2 (RFC-4122)

    return new UUID(msb, lsb);
  }

  /**
   * Checks if the provided UUID is of version 4 (random).
   *
   * @param uuid the UUID to be checked
   * @return true if the UUID is of version 4, false otherwise
   */
  public static boolean isV4(UUID uuid) {
    return uuid.version() == 4;
  }

  /**
   * Checks if the provided UUID is of version 7 (time-based).
   *
   * @param uuid the UUID to be checked
   * @return true if the UUID is of version 7, false otherwise
   */
  public static boolean isV7(UUID uuid) {
    return uuid.version() == 7;
  }

  /**
   * Extracts the {@link Instant} timestamp from a version 7 (time-based) UUID.
   *
   * @param uuid the UUID from which the timestamp will be extracted
   * @return the {@link Instant} representing the timestamp embedded in the version 7 UUID
   * @throws IllegalArgumentException if the given UUID is not a version 7 UUID
   */
  public static Instant getInstant(UUID uuid) {
    if (isV7(uuid)) {
      var msb = uuid.getMostSignificantBits();
      var timestamp = (msb >>> 16) & 0xFFFFFFFFFFFFL;
      return Instant.ofEpochMilli(timestamp);
    }
    throw new IllegalArgumentException("UUID is not version 7");
  }

  /**
   * Parses a string representation of a UUID and returns a {@link UUID} object.
   *
   * <p>This method enforces a strict 36-character format (e.g.,
   * "f81d4fae-7dec-11d0-a765-00a0c91e6bf6"). It is recommended over {@link UUID#fromString(String)}
   * which can be unsafe as it accepts UUIDs in formats other than the standard 36-character
   * representation. If the input does not meet this requirement, an {@link
   * IllegalArgumentException} is thrown.
   *
   * @param input the string representation of the UUID to be parsed
   * @return the {@link UUID} object represented by the input string
   * @throws IllegalArgumentException if the input does not conform to the standard 36-character
   *     UUID format
   */
  public static UUID parse(String input) {
    if (!isValid(input)) {
      throw new IllegalArgumentException(
          "UUID has to be represented by the standard 36-char representation");
    }
    return UUID.fromString(input);
  }

  /**
   * Validates if the given input string conforms to the standard 36-character UUID format
   *
   * @param input the string to be validated
   * @return true if the input string is a valid UUID format, false otherwise
   */
  public static boolean isValid(String input) {
    if (input == null || input.length() != 36) {
      return false;
    }

    for (int i = 0; i < 36; i++) {
      char c = input.charAt(i);
      if (i == 8 || i == 13 || i == 18 || i == 23) {
        if (c != '-') return false;
      } else {
        if (!isHexDigit(c)) return false;
      }
    }
    return true;
  }

  /**
   * Determines whether the provided character is a valid hexadecimal digit.
   *
   * <p>A hexadecimal digit is any of the characters '0' through '9', 'a' through 'f', or 'A'
   * through 'F'.
   *
   * @param c the character to be checked
   * @return true if the character is a valid hexadecimal digit, false otherwise
   */
  private static boolean isHexDigit(char c) {
    return (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
  }
}
