/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.tools.code.entity.model;

import static com.axelor.tools.code.entity.model.Utils.*;

import com.axelor.tools.code.JavaAnnotation;
import com.axelor.tools.code.JavaCode;
import jakarta.xml.bind.annotation.XmlType;

@XmlType
public class UniqueConstraint extends Index {

  @Override
  public JavaAnnotation toJavaAnnotation(Entity entity) {
    JavaAnnotation annotation = new JavaAnnotation("jakarta.persistence.UniqueConstraint");
    if (notBlank(getName())) {
      annotation.param("name", "{0:s}", getName());
    }
    annotation.param("columnNames", list(getColumnList(entity)), s -> new JavaCode("{0:s}", s));
    return annotation;
  }
}
