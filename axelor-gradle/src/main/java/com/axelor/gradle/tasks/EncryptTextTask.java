/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
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
