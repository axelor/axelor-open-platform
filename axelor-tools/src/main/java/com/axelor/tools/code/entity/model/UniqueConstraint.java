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

import static com.axelor.tools.code.entity.model.Utils.*;

import com.axelor.tools.code.JavaAnnotation;
import com.axelor.tools.code.JavaCode;
import javax.xml.bind.annotation.XmlType;

@XmlType
public class UniqueConstraint extends Index {

  @Override
  public JavaAnnotation toJavaAnnotation(Entity entity) {
    JavaAnnotation annotation = new JavaAnnotation("javax.persistence.UniqueConstraint");
    if (notBlank(getName())) {
      annotation.param("name", "{0:s}", getName());
    }
    annotation.param("columnNames", list(getColumnList(entity)), s -> new JavaCode("{0:s}", s));
    return annotation;
  }
}
