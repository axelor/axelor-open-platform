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
import java.security.spec.AlgorithmParameterSpec;
import java.util.Arrays;
import java.util.List;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import com.google.common.base.MoreObjects;
import com.google.common.primitives.Bytes;

public class BytesEncryptor implements Encryptor<byte[], byte[]> {

	private static final String AES_ALGORITHM = "AES";
	private static final String KEY_ALGORITHM = "PBKDF2WithHmacSHA1";

	private static final SecureRandom SECURE_RANDOM = new SecureRandom();

	private static final int IV_SIZE = 16;
	private static final int SALT_SIZE = 8;
	private static final int TAG_BIT_LENGTH = 128;

	static final String PREFIX = "$AES$";
	static final byte[] PREFIX_BYTES = PREFIX.getBytes();

	private final OperationMode mode;
	private final String password;
	private final String transformation;

	private final byte[] encryptionSalt;
	private final SecretKey encryptionKey;
	
	private final int payloadSize;

	public BytesEncryptor(OperationMode mode, PaddingScheme paddingScheme, String password) {
		this.mode = mode;
		this.password = password;
		this.transformation = String.format("%s/%s/%s", AES_ALGORITHM, mode, paddingScheme);
		this.encryptionSalt = generateRandomBytes(SALT_SIZE);
		this.encryptionKey = newSecretKey(password, this.encryptionSalt);
		this.payloadSize = mode == OperationMode.CBC
				? PREFIX_BYTES.length + SALT_SIZE
				: PREFIX_BYTES.length + SALT_SIZE + IV_SIZE;
	}

	public BytesEncryptor(String password) {
		this(OperationMode.CBC, PaddingScheme.PKCS5, password);
	}
	
	public static BytesEncryptor cbc(String password) {
		return new BytesEncryptor(OperationMode.CBC, PaddingScheme.PKCS5, password);
	}

	public static BytesEncryptor gcm(String password) {
		return new BytesEncryptor(OperationMode.GCM, PaddingScheme.NONE, password);
	}
	
	public String getTransformation() {
		return transformation;
	}

	private byte[] generateRandomBytes(int size) {
		byte[] bytes = new byte[size];
		SECURE_RANDOM.nextBytes(bytes);
		return bytes;
	}

	private SecretKey newSecretKey(String password, byte[] salt) {
		try {
			final PBEKeySpec keySpec = new PBEKeySpec(password.toCharArray(), salt, 1024, 256);
			final SecretKeyFactory keyFactory = SecretKeyFactory.getInstance(KEY_ALGORITHM);
			final SecretKey tmp = keyFactory.generateSecret(keySpec);
			return new SecretKeySpec(tmp.getEncoded(), AES_ALGORITHM);
		} catch (Exception e) {
			throw new EncryptorException(e);
		}
	}
	
	private AlgorithmParameterSpec newParameterSpec(byte[] iv) {
		return this.mode == OperationMode.CBC ? new IvParameterSpec(iv) : new GCMParameterSpec(TAG_BIT_LENGTH, iv);
	}

	private Cipher newCipher(int mode, SecretKey key, byte[] iv) {
		try {
			final AlgorithmParameterSpec paramSpec = newParameterSpec(iv);
			final Cipher cipher = Cipher.getInstance(this.transformation);
			cipher.init(mode, key, paramSpec);
			return cipher;
		} catch (Exception e) {
			throw new EncryptorException(e);
		}
	}

	private byte[] doFinal(Cipher cipher, byte[] data) {
		try {
			return cipher.doFinal(data);
		} catch (Exception e) {
			throw new EncryptorException(e);
		}
	}

	@Override
	public boolean isEncrypted(byte[] bytes) {
		if (bytes == null || bytes.length < this.payloadSize) {
			return false;
		}
		final byte[] prefix = new byte[PREFIX_BYTES.length];
		System.arraycopy(bytes, 0, prefix, 0, prefix.length);
		return Arrays.equals(prefix, PREFIX_BYTES);
	}

	@Override
	public byte[] encrypt(byte[] bytes) {
		if (bytes == null || isEncrypted(bytes)) {
			return bytes;
		}

		final byte[] iv = this.mode == OperationMode.CBC ? new byte[IV_SIZE] : generateRandomBytes(IV_SIZE);
		final Cipher cipher = newCipher(Cipher.ENCRYPT_MODE, this.encryptionKey, iv);
		final byte[] encrypted = doFinal(cipher, bytes);

		return this.mode == OperationMode.CBC
				? Bytes.concat(PREFIX_BYTES, this.encryptionSalt, encrypted)
				: Bytes.concat(PREFIX_BYTES, this.encryptionSalt, iv, encrypted);
	}

	@Override
	public byte[] decrypt(byte[] bytes) {
		if (bytes == null || !isEncrypted(bytes)) {
			return bytes;
		}

		final byte[] salt = new byte[SALT_SIZE];
		final byte[] iv = new byte[IV_SIZE];
		final byte[] data = new byte[bytes.length - this.payloadSize];

		final List<byte[]> sections = this.mode == OperationMode.CBC
				? Arrays.asList(salt, data)
				: Arrays.asList(salt, iv, data);

		int index = PREFIX_BYTES.length;
		for (byte[] section : sections) {
			System.arraycopy(bytes, index, section, 0, section.length);
			index += section.length;
		}

		final SecretKey key = newSecretKey(password, salt);
		final Cipher cipher = newCipher(Cipher.DECRYPT_MODE, key, iv);
		return doFinal(cipher, data);
	}
	
	@Override
	public String toString() {
		return MoreObjects.toStringHelper(BytesEncryptor.class)
				.addValue(this.transformation)
				.toString();
	}
}
