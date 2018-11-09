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
package com.axelor.meta.schema.views;

import java.util.List;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlType
public class Extend {

  @XmlAttribute private String target;

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
