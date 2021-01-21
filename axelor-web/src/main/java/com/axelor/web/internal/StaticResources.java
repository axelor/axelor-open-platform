/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2021 Axelor (<http://axelor.com>).
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
package com.axelor.web.internal;

import com.axelor.meta.MetaScanner;
import com.axelor.web.StaticResourceProvider;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public final class StaticResources {

  private static final StaticResources INSTANCE = new StaticResources();

  private Set<String> styles;

  private Set<String> scripts;

  private StaticResources() {
    List<String> resources = new ArrayList<>();
    MetaScanner.findSubTypesOf(StaticResourceProvider.class).find().stream()
        .map(
            clazz -> {
              try {
                return clazz.getDeclaredConstructor().newInstance();
              } catch (Exception e) {
                throw new RuntimeException("Invalid static resource provider: " + clazz, e);
              }
            })
        .forEach(provider -> provider.register(resources));

    styles = resources.stream().filter(res -> res.endsWith(".css")).collect(Collectors.toSet());
    scripts = resources.stream().filter(res -> res.endsWith(".js")).collect(Collectors.toSet());
  }

  public static Set<String> getStyles() {
    return INSTANCE.styles;
  }

  public static Set<String> getScripts() {
    return INSTANCE.scripts;
  }
}
