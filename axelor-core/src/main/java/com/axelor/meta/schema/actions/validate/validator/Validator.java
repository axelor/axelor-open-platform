/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.meta.schema.actions.validate.validator;

import com.axelor.common.StringUtils;
import com.axelor.i18n.I18n;
import com.axelor.meta.schema.actions.Action;
import jakarta.xml.bind.annotation.XmlAttribute;
import java.util.HashMap;
import java.util.Map;

public abstract class Validator extends Action.Element {

  @XmlAttribute(name = "message")
  private String message;

  @XmlAttribute(name = "action")
  private String action;

  @XmlAttribute(name = "title")
  private String title;

  @XmlAttribute(name = "confirm-btn-title")
  private String confirmBtnTitle;

  @XmlAttribute(name = "cancel-btn-title")
  private String cancelBtnTitle;

  public abstract String getKey();

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public String getAction() {
    return action;
  }

  public void setAction(String action) {
    this.action = action;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getConfirmBtnTitle() {
    return confirmBtnTitle;
  }

  public void setConfirmBtnTitle(String confirmBtnTitle) {
    this.confirmBtnTitle = confirmBtnTitle;
  }

  public String getCancelBtnTitle() {
    return cancelBtnTitle;
  }

  public void setCancelBtnTitle(String cancelBtnTitle) {
    this.cancelBtnTitle = cancelBtnTitle;
  }

  public String getLocalizedTitle() {
    return I18n.get(title);
  }

  public String getLocalizedConfirmBtnTitle() {
    return I18n.get(confirmBtnTitle);
  }

  public String getLocalizedCancelBtnTitle() {
    return I18n.get(cancelBtnTitle);
  }

  public Map<String, String> toMap(String value) {
    final Map<String, String> map = new HashMap<>();
    map.put("message", value);
    if (StringUtils.notBlank(title)) {
      map.put("title", getLocalizedTitle());
    }
    if (StringUtils.notBlank(confirmBtnTitle)) {
      map.put("confirmBtnTitle", getLocalizedConfirmBtnTitle());
    }
    if (StringUtils.notBlank(cancelBtnTitle)) {
      map.put("cancelBtnTitle", getLocalizedCancelBtnTitle());
    }
    if (StringUtils.notBlank(action)) {
      map.put("action", action);
    }
    return map;
  }
}
