/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

public class TestUuidUtils {

  @Test
  public void uuidv4_testCompliance() {
    UUID uuid = UuidUtils.v4();

    assertEquals(4, uuid.version());
    assertTrue(UuidUtils.isV4(uuid));
    assertEquals(36, uuid.toString().length());
  }

  @Test
  public void uuidv7_testCompliance() {
    UUID uuid = UuidUtils.v7();

    assertEquals(7, uuid.version());
    assertTrue(UuidUtils.isV7(uuid));
    assertEquals(36, uuid.toString().length());
  }

  @Test
  void uuidv7_testTimestamp() {
    long toleranceMs = 2; // Allow 2ms buffer for execution time

    Instant before = Instant.now();
    UUID uuid = UuidUtils.v7();
    Instant after = Instant.now();

    Instant extracted = UuidUtils.getInstant(uuid);

    // Check not too old
    assertFalse(extracted.isBefore(before.minusMillis(toleranceMs)));
    // Check not in future
    assertFalse(extracted.isAfter(after.plusMillis(toleranceMs)));
  }

  @Test
  void uuidv7_testSortability() throws InterruptedException {
    UUID u1 = UuidUtils.v7();
    Thread.sleep(2);
    UUID u2 = UuidUtils.v7();
    Thread.sleep(2);
    UUID u3 = UuidUtils.v7();

    assertTrue(u1.compareTo(u2) < 0);
    assertTrue(u2.compareTo(u3) < 0);
  }

  @Test
  void uuidv7_testUniqueness() throws InterruptedException {
    // 128 threads * 10_000 iterations = 1_128_000 UUIDs
    int threadCount = 128;
    int iterationCount = 10_000;

    Map<UUID, Boolean> uuidMap = new ConcurrentHashMap<>();
    AtomicLong collisionCount = new AtomicLong();
    CountDownLatch endLatch = new CountDownLatch(threadCount);

    for (long i = 0; i < threadCount; i++) {
      new Thread(
              () -> {
                for (long j = 0; j < iterationCount; j++) {
                  UUID uuid = UuidUtils.v7();
                  if (uuidMap.putIfAbsent(uuid, true) != null) {
                    collisionCount.incrementAndGet();
                  }
                }
                endLatch.countDown();
              })
          .start();
    }

    endLatch.await();

    assertEquals(0, collisionCount.get());
    assertEquals(threadCount * iterationCount, uuidMap.size());
  }

  @Test
  void uuidv7_testParse() {
    long timestamp = 1764751085542L;
    UUID uuid = UUID.fromString("019ae35c-8fe6-7b4e-affd-01b84864fbc7");

    assertTrue(UuidUtils.isV7(uuid));
    assertEquals(timestamp, UuidUtils.getInstant(uuid).toEpochMilli());
  }

  @Nested
  class IsValidTest {

    @ParameterizedTest
    @ValueSource(
        strings = {
          "550e8400-e29b-41d4-a716-446655440000", // standard v4
          "6ba7b810-9dad-11d1-80b4-00c04fd430c8", // v1
          "019c0514-9f12-724b-961d-123456789012", // v7
          "00000000-0000-0000-0000-000000000000", // nil UUID
          "ffffffff-ffff-ffff-ffff-ffffffffffff", // max UUID
          "FFFFFFFF-FFFF-FFFF-FFFF-FFFFFFFFFFFF", // uppercase
          "550E8400-E29B-41D4-A716-446655440000", // mixed case
          "abcdefab-abcd-abcd-abcd-abcdefabcdef" // all letters
        })
    void validUuids(String input) {
      assertTrue(UuidUtils.isValid(input));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(
        strings = {
          "550e8400-e29b-41d4-a716-44665544000", // 35 chars (too short)
          "550e8400-e29b-41d4-a716-4466554400000", // 37 chars (too long)
          "550e8400-e29b-41d4-a716-1234", // truncated last segment
          "550e8400e29b41d4a716446655440000", // no hyphens (32 chars)
          "550e8400-e29b-41d4-a716-44665544000g", // 'g' is invalid
          "550e8400-e29b-41d4-a716-44665544000G", // 'G' is invalid
          "550e8400-e29b-41d4-a716-4466554400 0", // space
          "550e8400-e29b-41d4-a716-44665544000!", // special char
          "550e8400-e29b-41d4-a716-44665544000z", // 'z' is invalid
          "gggggggg-gggg-gggg-gggg-gggggggggggg", // all invalid
          "550e8400e-29b-41d4-a716-44665544000", // hyphen at wrong position
          "550e840-0e29b-41d4-a716-446655440000", // hyphen at wrong position
          "550e8400-e29b-41d4-a716_446655440000", // underscore instead of hyphen
          "550e8400-e29b-41d4-a71-6446655440000", // shifted hyphen
          "550e8400-e29b041d4-a716-44665544000" // missing hyphen
        })
    void invalidUuids(String input) {
      assertFalse(UuidUtils.isValid(input));
    }
  }

  @Nested
  class ParseTest {

    @ParameterizedTest
    @ValueSource(
        strings = {
          "550e8400-e29b-41d4-a716-446655440000",
          "6ba7b810-9dad-11d1-80b4-00c04fd430c8",
          "019c0514-9f12-724b-961d-123456789012",
          "00000000-0000-0000-0000-000000000000",
          "ffffffff-ffff-ffff-ffff-ffffffffffff"
        })
    void validUuids(String input) {
      UUID result = UuidUtils.parse(input);

      assertNotNull(result);
      assertEquals(input.toLowerCase(), result.toString());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(
        strings = {
          "not-a-uuid",
          "550e8400-e29b-41d4-a716-1234", // too short
          "550e8400-e29b-41d4-a716-44665544000g", // invalid char
          "550e8400e29b41d4a716446655440000" // missing hyphens
        })
    void invalidUuids(String input) {
      assertThrows(IllegalArgumentException.class, () -> UuidUtils.parse(input));
    }
  }
}
