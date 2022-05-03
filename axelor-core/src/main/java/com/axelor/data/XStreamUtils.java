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
package com.axelor.data;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAliasType;
import com.thoughtworks.xstream.annotations.XStreamInclude;

public class XStreamUtils {

  public static XStream createXStream() {
    return setupSecurity(new XStream());
  }

  public static XStream setupSecurity(XStream xStream) {
    // Permission for any type which is annotated with an XStream annotation.
    xStream.addPermission(
        type -> {
          if (type == null) {
            return false;
          }
          return ((Class<?>) type).isAnnotationPresent(XStreamAlias.class)
              || ((Class<?>) type).isAnnotationPresent(XStreamAliasType.class)
              || ((Class<?>) type).isAnnotationPresent(XStreamInclude.class);
        });
    return xStream;
  }
}
