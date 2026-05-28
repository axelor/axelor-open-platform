/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.cache.redisson;

import com.google.common.primitives.Ints;
import java.util.Optional;

public record Version(int major, int minor, int patch) implements Comparable<Version> {

  public static Version UNKNOWN = of(0);

  public static Version of(int major, int minor, int patch) {
    return new Version(major, minor, patch);
  }

  public static Version of(int major, int minor) {
    return of(major, minor, 0);
  }

  public static Version of(int major) {
    return of(major, 0);
  }

  public static Version parse(String version) {
    var stringParts = version.split("\\.");
    var parts = new int[3];
    var length = Math.min(stringParts.length, 3);

    for (int i = 0; i < length; ++i) {
      parts[i] = Optional.ofNullable(Ints.tryParse(stringParts[i])).orElse(0);
    }

    return of(parts[0], parts[1], parts[2]);
  }

  @Override
  public int compareTo(Version other) {
    if (major != other.major) {
      return major - other.major;
    }
    if (minor != other.minor) {
      return minor - other.minor;
    }
    return patch - other.patch;
  }

  public String toString() {
    return major + "." + minor + "." + patch;
  }
}
