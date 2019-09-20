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
package com.axelor.rpc;

import com.axelor.common.ObjectUtils;
import com.google.common.collect.ImmutableList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.function.Consumer;

public class RequestUtils {

  private RequestUtils() {}

  public static void processRequest(Request request, Consumer<Map<String, Object>> consumer) {
    final Collection<Object> records = getRecords(request.getRecords(), request.getData());
    processRecords(records, consumer);
  }

  public static void processResponse(Response response, Consumer<Map<String, Object>> consumer) {
    final Collection<Object> records = getRecords(Collections.emptyList(), response.getData());
    processRecords(records, consumer);
  }

  private static Collection<Object> getRecords(Collection<Object> records, Object data) {
    final Collection<Object> allRecords;

    if (ObjectUtils.notEmpty(records)) {
      allRecords = records;
    } else if (data instanceof Collection) {
      @SuppressWarnings("unchecked")
      final Collection<Object> dataAsCollection = (Collection<Object>) data;
      allRecords = dataAsCollection;
    } else if (data != null) {
      allRecords = ImmutableList.of(data);
    } else {
      allRecords = Collections.emptyList();
    }

    return allRecords;
  }

  private static void processRecords(
      Collection<Object> records, Consumer<Map<String, Object>> consumer) {
    records.stream()
        .filter(record -> record instanceof Map)
        .forEach(
            record -> {
              @SuppressWarnings("unchecked")
              final Map<String, Object> values = (Map<String, Object>) record;
              consumer.accept(values);
            });
  }
}
