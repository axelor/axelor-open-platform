/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2019 Axelor (<http://axelor.com>).
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

/**
 * The unchecked exception is thrown by {@link Encryptor} classes if there is any error while
 * encrypting or decrypting values.
 */
public class EncryptorException extends RuntimeException {

  private static final long serialVersionUID = 7340536599422956645L;

  public EncryptorException(String message, Throwable cause) {
    super(message, cause);
  }

  public EncryptorException(String message) {
    super(message);
  }

  public EncryptorException(Throwable cause) {
    super(cause);
  }
}
