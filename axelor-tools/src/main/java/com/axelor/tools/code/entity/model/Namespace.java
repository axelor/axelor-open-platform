/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.tools.code.entity.model;

import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

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
