/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.tools.code.entity.model;

import jakarta.xml.bind.annotation.XmlEnum;

@XmlEnum
public enum TrackEvent {
  ALWAYS,

  CREATE,

  UPDATE;

  public String value() {
    return name();
  }

  public static TrackEvent fromValue(String v) {
    return valueOf(v);
  }
}
