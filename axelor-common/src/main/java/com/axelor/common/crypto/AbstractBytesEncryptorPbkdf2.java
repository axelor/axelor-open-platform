/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.common.crypto;

import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;
import java.util.Arrays;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Abstract base for AES byte-array encryptors backed by a PBKDF2 key derivation function.
 *
 * @see BytesEncryptorPbkdf2Sha256
 * @see BytesEncryptorPbkdf2Sha512
 */
abstract class AbstractBytesEncryptorPbkdf2 implements Encryptor<byte[], byte[]> {

  private static final String AES_ALGORITHM = "AES";
  private static final SecureRandom SECURE_RANDOM = new SecureRandom();

  public static final int DEFAULT_GCM_IV_SIZE = 12;
  public static final int DEFAULT_CBC_IV_SIZE = 16;
  private static final int DEFAULT_SALT_SIZE = 16;
  private static final int KEY_LENGTH = 256;
  private static final int TAG_BIT_LENGTH = 128;

  private final OperationMode mode;
  private final String transformation;
  private final String password;
  private final String keyAlgorithm;
  private final int iterations;
  private final int saltSize;
  private final int ivSize;
  private final byte[] prefixBytes;
  private final int minPayloadSize;

  /**
   * Returns the standard {@link PaddingScheme} for the given {@link OperationMode}: {@link
   * PaddingScheme#PKCS5} for CBC, {@link PaddingScheme#NONE} for GCM.
   */
  static PaddingScheme paddingFor(OperationMode mode) {
    return mode == OperationMode.CBC ? PaddingScheme.PKCS5 : PaddingScheme.NONE;
  }

  /**
   * Standard constructor. Uses the default salt size ({@value DEFAULT_SALT_SIZE} bytes) and the
   * mode-appropriate IV size ({@link #DEFAULT_GCM_IV_SIZE} or {@link #DEFAULT_CBC_IV_SIZE}).
   *
   * @param mode the AES operation mode (GCM or CBC)
   * @param password the password used for key derivation
   * @param keyAlgorithm the PBKDF2 algorithm name
   * @param iterations the PBKDF2 iteration count
   * @param prefixBytes the binary prefix that identifies the ciphertext format
   */
  protected AbstractBytesEncryptorPbkdf2(
      OperationMode mode,
      String password,
      String keyAlgorithm,
      int iterations,
      byte[] prefixBytes) {
    this(mode, password, keyAlgorithm, iterations, prefixBytes, DEFAULT_SALT_SIZE);
  }

  /**
   * Constructor with a custom salt size. The IV size defaults to the mode-appropriate value.
   *
   * @param mode the AES operation mode (GCM or CBC)
   * @param password the password used for key derivation
   * @param keyAlgorithm the PBKDF2 algorithm name
   * @param iterations the PBKDF2 iteration count
   * @param prefixBytes the binary prefix that identifies the ciphertext format
   * @param saltSize the salt length in bytes
   */
  AbstractBytesEncryptorPbkdf2(
      OperationMode mode,
      String password,
      String keyAlgorithm,
      int iterations,
      byte[] prefixBytes,
      int saltSize) {
    this(
        mode,
        password,
        keyAlgorithm,
        iterations,
        prefixBytes,
        saltSize,
        mode == OperationMode.GCM ? DEFAULT_GCM_IV_SIZE : DEFAULT_CBC_IV_SIZE);
  }

  /**
   * Full constructor with explicit salt and IV sizes. Intended for testing only.
   *
   * @param mode the AES operation mode (GCM or CBC)
   * @param password the password used for key derivation
   * @param keyAlgorithm the PBKDF2 algorithm name
   * @param iterations the PBKDF2 iteration count
   * @param prefixBytes the binary prefix that identifies the ciphertext format
   * @param saltSize the salt length in bytes
   * @param ivSize the IV length in bytes
   */
  AbstractBytesEncryptorPbkdf2(
      OperationMode mode,
      String password,
      String keyAlgorithm,
      int iterations,
      byte[] prefixBytes,
      int saltSize,
      int ivSize) {
    this.mode = mode;
    this.password = password;
    this.transformation = "%s/%s/%s".formatted(AES_ALGORITHM, mode, paddingFor(mode));
    this.keyAlgorithm = keyAlgorithm;
    this.iterations = iterations;
    this.saltSize = saltSize;
    this.ivSize = ivSize;
    this.prefixBytes = prefixBytes;
    // prefix(n) + iterations(4) + salt_size(1) + iv_size(1) + salt(n) + iv(n)
    this.minPayloadSize = prefixBytes.length + 4 + 1 + 1 + saltSize + ivSize;
  }

  /**
   * Returns the JCE transformation string (e.g. {@code AES/GCM/NoPadding}) used by this encryptor.
   */
  public String getTransformation() {
    return transformation;
  }

  private byte[] generateRandomBytes(int size) {
    final byte[] bytes = new byte[size];
    SECURE_RANDOM.nextBytes(bytes);
    return bytes;
  }

  private SecretKey newSecretKey(byte[] salt, int iterationCount) {
    try {
      final PBEKeySpec keySpec =
          new PBEKeySpec(password.toCharArray(), salt, iterationCount, KEY_LENGTH);
      final SecretKeyFactory keyFactory = SecretKeyFactory.getInstance(keyAlgorithm);
      final SecretKey tmp = keyFactory.generateSecret(keySpec);
      return new SecretKeySpec(tmp.getEncoded(), AES_ALGORITHM);
    } catch (Exception e) {
      throw new EncryptorException(e);
    }
  }

  private AlgorithmParameterSpec newParameterSpec(byte[] iv) {
    return this.mode == OperationMode.CBC
        ? new IvParameterSpec(iv)
        : new GCMParameterSpec(TAG_BIT_LENGTH, iv);
  }

  @Override
  public boolean isEncrypted(byte[] bytes) {
    if (bytes == null || bytes.length < minPayloadSize) {
      return false;
    }
    final byte[] prefix = new byte[prefixBytes.length];
    System.arraycopy(bytes, 0, prefix, 0, prefix.length);
    return Arrays.equals(prefix, prefixBytes);
  }

  @Override
  public byte[] encrypt(byte[] bytes) {
    if (bytes == null || isEncrypted(bytes)) {
      return bytes;
    }

    final byte[] salt = generateRandomBytes(saltSize);
    final byte[] iv = generateRandomBytes(ivSize);
    final SecretKey key = newSecretKey(salt, iterations);

    try {
      final Cipher cipher = Cipher.getInstance(transformation);
      cipher.init(Cipher.ENCRYPT_MODE, key, newParameterSpec(iv));
      final byte[] encrypted = cipher.doFinal(bytes);

      final ByteBuffer out =
          ByteBuffer.allocate(
              prefixBytes.length + 4 + 1 + 1 + salt.length + iv.length + encrypted.length);
      out.put(prefixBytes);
      out.putInt(iterations);
      out.put((byte) saltSize);
      out.put((byte) ivSize);
      out.put(salt);
      out.put(iv);
      out.put(encrypted);

      return out.array();
    } catch (Exception e) {
      throw new EncryptorException(e);
    }
  }

  @Override
  public byte[] decrypt(byte[] bytes) {
    if (bytes == null || !isEncrypted(bytes)) {
      return bytes;
    }

    try {
      final ByteBuffer buf = ByteBuffer.wrap(bytes);
      buf.position(prefixBytes.length);

      final int iterationCount = buf.getInt();
      final int saltSize = Byte.toUnsignedInt(buf.get());
      final int ivSize = Byte.toUnsignedInt(buf.get());

      final byte[] salt = new byte[saltSize];
      buf.get(salt);

      final byte[] iv = new byte[ivSize];
      buf.get(iv);

      final byte[] encrypted = new byte[buf.remaining()];
      buf.get(encrypted);

      final SecretKey key = newSecretKey(salt, iterationCount);
      final Cipher cipher = Cipher.getInstance(transformation);
      cipher.init(Cipher.DECRYPT_MODE, key, newParameterSpec(iv));
      return cipher.doFinal(encrypted);
    } catch (Exception e) {
      throw new EncryptorException(e);
    }
  }
}
