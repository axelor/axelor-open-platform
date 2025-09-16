/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.meta.schema.views;

import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;
import java.util.List;

@XmlType
public class Extend {

  @XmlAttribute private String target;

  @XmlAttribute(name = "if-feature")
  private String featureToCheck;

  @XmlAttribute(name = "if-module")
  private String moduleToCheck;

  @XmlElement(name = "insert")
  private List<ExtendItemInsert> inserts;

  @XmlElement(name = "replace")
  private List<ExtendItemReplace> replaces;

  @XmlElement(name = "move")
  private List<ExtendItemMove> moves;

  @XmlElement(name = "attribute")
  private List<ExtendItemAttribute> attributes;

  public String getTarget() {
    return target;
  }

  public void setTarget(String target) {
    this.target = target;
  }

  public String getFeatureToCheck() {
    return featureToCheck;
  }

  public void setFeatureToCheck(String featureToCheck) {
    this.featureToCheck = featureToCheck;
  }

  public String getModuleToCheck() {
    return moduleToCheck;
  }

  public void setModuleToCheck(String moduleToCheck) {
    this.moduleToCheck = moduleToCheck;
  }

  public List<ExtendItemInsert> getInserts() {
    return inserts;
  }

  public void setInserts(List<ExtendItemInsert> inserts) {
    this.inserts = inserts;
  }

  public List<ExtendItemReplace> getReplaces() {
    return replaces;
  }

  public void setReplaces(List<ExtendItemReplace> replaces) {
    this.replaces = replaces;
  }

  public List<ExtendItemMove> getMoves() {
    return moves;
  }

  public void setMoves(List<ExtendItemMove> moves) {
    this.moves = moves;
  }

  public List<ExtendItemAttribute> getAttributes() {
    return attributes;
  }

  public void setAttributes(List<ExtendItemAttribute> attributes) {
    this.attributes = attributes;
  }
}
