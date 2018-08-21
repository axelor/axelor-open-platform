/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2018 Axelor (<http://axelor.com>).
 *
 * This program is free software: you can redistribute it and/or  modify
 * it under the terms of the GNU Affero General Public License, version 3,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.axelor.common.crypto;

import java.security.SecureRandom;
import java.util.Arrays;
import org.junit.Assert;
import org.junit.Test;

public class TestEncryptors {

  private static final String SECRET_KEY = "MySuperSecretKey";

  private byte[] generateRandomBytes(int size) {
    byte[] value = new byte[size];
    new SecureRandom().nextBytes(value);
    return value;
  }

  private void testBytesEncryptor(BytesEncryptor encryptor) {

    final byte[] value = generateRandomBytes(18);

    byte[] encrypted = encryptor.encrypt(value);
    byte[] decrypted = encryptor.decrypt(encrypted);

    Assert.assertNotNull(encrypted);
    Assert.assertFalse(Arrays.equals(encrypted, value));
    Assert.assertTrue(Arrays.equals(decrypted, value));

    // make sure to have special prefix
    byte[] prefix = new byte[BytesEncryptor.PREFIX_BYTES.length];
    System.arraycopy(encrypted, 0, prefix, 0, prefix.length);

    Assert.assertTrue(Arrays.equals(BytesEncryptor.PREFIX_BYTES, prefix));
  }

  private void testStringEncryptor(StringEncryptor encryptor) {

    final String value = "Hello World!!!";

    String encrypted = encryptor.encrypt(value);
    String decrypted = encryptor.decrypt(encrypted);

    Assert.assertNotNull(encrypted);
    Assert.assertNotEquals(encrypted, value);
    Assert.assertEquals(decrypted, value);
  }

  @Test
  public void testBytesCBC() {
    testBytesEncryptor(BytesEncryptor.cbc(SECRET_KEY));
  }

  @Test
  public void testBytesGCM() {
    testBytesEncryptor(BytesEncryptor.gcm(SECRET_KEY));
  }

  @Test
  public void testStringCBC() {
    testStringEncryptor(StringEncryptor.cbc(SECRET_KEY));
  }

  @Test
  public void testStringGCM() {
    testStringEncryptor(StringEncryptor.gcm(SECRET_KEY));
  }
}
