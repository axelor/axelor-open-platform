/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2022 Axelor (<http://axelor.com>).
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
package com.axelor.tools.code.entity.model;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;

@XmlType
public class Namespace {

  @XmlAttribute(name = "name", required = true)
  private String name;

  @XmlAttribute(name = "package", required = true)
  private String packageName;

  @XmlAttribute(name = "repo-package")
  private String repoPackageName;

  @XmlAttribute(name = "table-prefix")
  private String tablePrefix;

  public String getName() {
    return name;
  }

  public void setName(String value) {
    this.name = value;
  }

  public String getPackageName() {
    return packageName;
  }

  public void setPackageName(String value) {
    this.packageName = value;
  }

  public String getRepoPackageName() {
    return repoPackageName;
  }

  public void setRepoPackageName(String value) {
    this.repoPackageName = value;
  }

  public String getTablePrefix() {
    return tablePrefix;
  }

  public void setTablePrefix(String value) {
    this.tablePrefix = value;
  }

  @Override
  public String toString() {
    return "Namespace [name="
        + name
        + ", package="
        + packageName
        + ", repoPackage="
        + repoPackageName
        + ", tablePrefix="
        + tablePrefix
        + "]";
  }
}
