/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.rpc;

import com.axelor.common.ObjectUtils;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
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
      allRecords = List.of(data);
    } else {
      allRecords = Collections.emptyList();
    }

    return allRecords;
  }

  private static void processRecords(
      Collection<Object> records, Consumer<Map<String, Object>> consumer) {
    records.stream()
        .filter(Map.class::isInstance)
        .forEach(
            record -> {
              @SuppressWarnings("unchecked")
              final Map<String, Object> values = (Map<String, Object>) record;
              consumer.accept(values);
            });
  }
}
