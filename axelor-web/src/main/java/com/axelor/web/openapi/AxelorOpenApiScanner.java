/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2024 Axelor (<http://axelor.com>).
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
package com.axelor.web.openapi;

import com.axelor.common.ObjectUtils;
import io.swagger.v3.jaxrs2.integration.JaxrsApplicationAndAnnotationScanner;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class AxelorOpenApiScanner extends JaxrsApplicationAndAnnotationScanner {

  private static final Set<String> IGNORED = new HashSet<>();

  static {
    IGNORED.add("org.jboss.resteasy.core.AsynchronousDispatcher");
  }

  public AxelorOpenApiScanner() {
    super();
  }

  @Override
  public Set<Class<?>> classes() {
    Set<Class<?>> classes = super.classes();
    return classes.stream()
        .filter(aClass -> !isIgnored(aClass.getName()))
        .collect(Collectors.toSet());
  }

  /** */
  @Override
  protected boolean isIgnored(String classOrPackageName) {
    if (ObjectUtils.isEmpty(classOrPackageName)) {
      return true;
    }
    return IGNORED.stream().anyMatch(classOrPackageName::startsWith);
  }
}
