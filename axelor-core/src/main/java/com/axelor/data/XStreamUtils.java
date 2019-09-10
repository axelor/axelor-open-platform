/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2019 Axelor (<http://axelor.com>).
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
    XStream.setupDefaultSecurity(xStream);
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
