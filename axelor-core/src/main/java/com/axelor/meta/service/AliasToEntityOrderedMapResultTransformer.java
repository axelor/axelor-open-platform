/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2020 Axelor (<http://axelor.com>).
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
package com.axelor.meta.service;

import java.util.LinkedHashMap;
import java.util.Map;
import org.hibernate.transform.AliasedTupleSubsetResultTransformer;

public final class AliasToEntityOrderedMapResultTransformer
    extends AliasedTupleSubsetResultTransformer {

  public static final AliasToEntityOrderedMapResultTransformer INSTANCE =
      new AliasToEntityOrderedMapResultTransformer();

  private static final long serialVersionUID = 5067366436822068155L;

  /** Disallow instantiation */
  private AliasToEntityOrderedMapResultTransformer() {}

  @Override
  public Object transformTuple(Object[] tuple, String[] aliases) {
    final Map<String, Object> result = new LinkedHashMap<>(tuple.length);
    for (int i = 0; i < tuple.length; ++i) {
      final String alias = aliases[i];
      if (alias != null) {
        result.put(alias, tuple[i]);
      }
    }
    return result;
  }

  @Override
  public boolean isTransformedValueATupleElement(String[] aliases, int tupleLength) {
    return false;
  }

  /**
   * Serialization hook for ensuring singleton uniqueing.
   *
   * @return The singleton instance : {@link #INSTANCE}
   */
  private Object readResolve() {
    return INSTANCE;
  }
}
