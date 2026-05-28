/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.axelor.db.mapper.Adapter;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class UUIDTest {

  @Test
  void testUUID() {
    // Test null value
    {
      final Object actual = Adapter.adapt(null, UUID.class, null, null);
      assertNull(actual);
    }

    // Test UUID directly
    {
      final UUID expected = UUID.randomUUID();
      final Object actual = Adapter.adapt(expected, UUID.class, null, null);
      assertEquals(expected, actual);
    }

    // Test String to UUID conversion
    {
      final UUID expected = UUID.randomUUID();
      final String value = expected.toString();
      final Object actual = Adapter.adapt(value, UUID.class, null, null);
      assertEquals(expected, actual);
    }

    // Test empty String conversion
    {
      final Object actual = Adapter.adapt("", UUID.class, null, null);
      assertNull(actual);
    }

    // Test blank String conversion
    {
      final Object actual = Adapter.adapt("   ", UUID.class, null, null);
      assertNull(actual);
    }
  }
}
