/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.common;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.axelor.common.VersionUtils.Version;
import org.junit.jupiter.api.Test;

public class TestVersionUtils {

  @Test
  public void test() {

    Version v1 = new Version("3.0.1");
    Version v2 = new Version("3.0.1-rc1");
    Version v3 = new Version("3.0.1-SNAPSHOT");

    assertEquals("3.0", v1.feature);
    assertEquals("3.0", v2.feature);
    assertEquals("3.0", v3.feature);

    assertEquals(3, v1.major);
    assertEquals(0, v1.minor);
    assertEquals(1, v1.patch);
    assertEquals(0, v1.rc);

    assertEquals(3, v2.major);
    assertEquals(0, v2.minor);
    assertEquals(1, v2.patch);
    assertEquals(1, v2.rc);

    assertEquals(3, v3.major);
    assertEquals(0, v3.minor);
    assertEquals(1, v3.patch);
    assertEquals(0, v3.rc);
  }
}
