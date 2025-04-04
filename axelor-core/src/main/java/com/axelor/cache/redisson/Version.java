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
