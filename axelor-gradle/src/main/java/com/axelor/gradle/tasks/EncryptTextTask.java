/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2023 Axelor (<http://axelor.com>).
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
package com.axelor.gradle.tasks;

import com.axelor.common.StringUtils;
import org.gradle.api.tasks.options.Option;

public class EncryptTextTask extends AbstractEncryptTask {

  public static final String TASK_NAME = "encryptText";
  public static final String TASK_DESCRIPTION = "Encrypt the given text.";

  private String text;
  private String encryptedText;

  @Option(option = "text", description = "String to encrypt")
  public void setText(String text) {
    this.text = text;
  }

  @Override
  public void validate() {
    super.validate();

    if (StringUtils.isBlank(this.text)) {
      throw new IllegalStateException("--text is required");
    }
  }

  @Override
  public void doEncrypt() {
    this.encryptedText = getEncryptor().encrypt(this.text);
  }

  @Override
  public void printOutput() {
    log("");
    log("-------OUTPUT-------");
    log(this.encryptedText);
  }
}
