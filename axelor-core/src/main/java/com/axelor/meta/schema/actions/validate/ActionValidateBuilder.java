/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.meta.schema.actions.validate;

import com.axelor.common.StringUtils;
import com.axelor.meta.schema.actions.validate.validator.ValidatorType;
import com.axelor.rpc.ActionResponse;
import jakarta.ws.rs.core.Response;
import java.util.HashMap;
import java.util.Map;

/**
 * The {@link ActionValidateBuilder} can be used to quickly define {@link ActionValidate} manually,
 * especially when setting message to {@link ActionResponse#setData(Object)} or with {@link
 * Response}.
 */
public class ActionValidateBuilder {

  private final ValidatorType validatorType;
  private String message;
  private String cancelBtnTitle;
  private String title;
  private String action;
  private String confirmBtnTitle;

  public ActionValidateBuilder(ValidatorType validatorType) {
    this.validatorType = validatorType;
  }

  /**
   * Set the message to show
   *
   * @param message the message to show
   * @return this
   */
  public ActionValidateBuilder setMessage(String message) {
    this.message = message;
    return this;
  }

  /**
   * Set the title of the modal/notification
   *
   * @param title the title of the modal/notification
   * @return this
   */
  public ActionValidateBuilder setTitle(String title) {
    this.title = title;
    return this;
  }

  /**
   * Set the action to be executed on error or alert message to make corrective measures, when error
   * dialog is closed or alert dialog is canceled.
   *
   * @param action the action to be executed
   * @return this
   */
  public ActionValidateBuilder setAction(String action) {
    this.action = action;
    return this;
  }

  /**
   * Set the title of the confirm button
   *
   * @param confirmBtnTitle the title of the confirm button
   * @return this
   */
  public ActionValidateBuilder setConfirmBtnTitle(String confirmBtnTitle) {
    this.confirmBtnTitle = confirmBtnTitle;
    return this;
  }

  /**
   * Set the title of the cancel button
   *
   * @param cancelBtnTitle the title of the cancel button
   * @return this
   */
  public ActionValidateBuilder setCancelBtnTitle(String cancelBtnTitle) {
    this.cancelBtnTitle = cancelBtnTitle;
    return this;
  }

  /**
   * Return a {@link Map} that represents the action validate.
   *
   * @return a {@link Map}
   */
  public Map<String, Map<String, String>> build() {
    final Map<String, String> map = new HashMap<>();
    map.put("message", this.message);
    if (StringUtils.notBlank(this.title)) {
      map.put("title", this.title);
    }
    if (StringUtils.notBlank(this.confirmBtnTitle)) {
      map.put("confirmBtnTitle", this.confirmBtnTitle);
    }
    if (StringUtils.notBlank(this.cancelBtnTitle)) {
      map.put("cancelBtnTitle", this.cancelBtnTitle);
    }
    if (StringUtils.notBlank(this.action)) {
      map.put("action", this.action);
    }
    return Map.of(validatorType.toString(), map);
  }
}
