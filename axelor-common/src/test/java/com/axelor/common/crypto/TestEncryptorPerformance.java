/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.common.crypto;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/** Performance comparison between algo and iterations. */
@Disabled
public class TestEncryptorPerformance {

  private static final String SECRET_KEY = "MySuperSecretKey";
  private static final byte[] PLAINTEXT =
      "Hello, performance benchmark!".getBytes(StandardCharsets.UTF_8);

  private static final int WARMUP_ROUNDS = 2;
  private static final int MEASURE_ROUNDS = 50;

  record Result(String label, long avgEncryptMs, long avgDecryptMs) {}

  private Result measure(String label, Encryptor<byte[], byte[]> encryptor) {
    // Warmup — allows the JVM to JIT-compile the hot path before measuring
    for (int i = 0; i < WARMUP_ROUNDS; i++) {
      encryptor.decrypt(encryptor.encrypt(PLAINTEXT));
    }

    // Measure encrypt
    final byte[][] ciphertexts = new byte[MEASURE_ROUNDS][];
    long encryptTotal = 0;
    for (int i = 0; i < MEASURE_ROUNDS; i++) {
      final long start = System.nanoTime();
      ciphertexts[i] = encryptor.encrypt(PLAINTEXT);
      encryptTotal += System.nanoTime() - start;
    }

    // Measure decrypt (using the ciphertexts produced above)
    long decryptTotal = 0;
    for (int i = 0; i < MEASURE_ROUNDS; i++) {
      final long start = System.nanoTime();
      encryptor.decrypt(ciphertexts[i]);
      decryptTotal += System.nanoTime() - start;
    }

    return new Result(
        label,
        TimeUnit.NANOSECONDS.toMillis(encryptTotal / MEASURE_ROUNDS),
        TimeUnit.NANOSECONDS.toMillis(decryptTotal / MEASURE_ROUNDS));
  }

  private void printResults(Result... results) {
    System.out.printf("%n%-45s %14s %14s%n", "Algorithm", "Encrypt (ms)", "Decrypt (ms)");
    System.out.println("-".repeat(75));
    for (Result r : results) {
      System.out.printf("%-45s %14d %14d%n", r.label(), r.avgEncryptMs(), r.avgDecryptMs());
    }
    System.out.printf(
        "(average over %d runs, after %d warmup rounds)%n%n", MEASURE_ROUNDS, WARMUP_ROUNDS);
  }

  @Test
  void compareGcm() {
    final Result sha256_lessIteration =
        measure(
            "PBKDF2WithHmacSHA256 / GCM (100,000 iter)",
            new BytesEncryptorPbkdf2Sha256(OperationMode.GCM, SECRET_KEY, 100_000));
    final Result sha256 =
        measure(
            "PBKDF2WithHmacSHA256 / GCM (600,000 iter)",
            BytesEncryptorPbkdf2Sha256.gcm(SECRET_KEY));
    final Result sha512 =
        measure(
            "PBKDF2WithHmacSHA512 / GCM (210,000 iter)",
            BytesEncryptorPbkdf2Sha512.gcm(SECRET_KEY));
    printResults(sha256_lessIteration, sha256, sha512);
  }

  @Test
  void compareCbc() {
    final Result sha256_lessIteration =
        measure(
            "PBKDF2WithHmacSHA256 / CBC (100,000 iter)",
            new BytesEncryptorPbkdf2Sha256(OperationMode.CBC, SECRET_KEY, 100_000));
    final Result sha256 =
        measure(
            "PBKDF2WithHmacSHA256 / CBC (600,000 iter)",
            BytesEncryptorPbkdf2Sha256.cbc(SECRET_KEY));
    final Result sha512 =
        measure(
            "PBKDF2WithHmacSHA512 / CBC (210,000 iter)",
            BytesEncryptorPbkdf2Sha512.cbc(SECRET_KEY));
    printResults(sha256_lessIteration, sha256, sha512);
  }
}
