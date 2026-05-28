/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.rpc;

import com.axelor.JpaTest;
import com.axelor.common.ResourceUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Inject;
import java.io.InputStreamReader;

abstract class RpcTest extends JpaTest {

  @Inject protected ObjectMapper mapper;

  protected InputStreamReader read(String json) {
    return new InputStreamReader(ResourceUtils.getResourceStream("json/" + json));
  }

  protected String toJson(Object value) {
    try {
      return mapper.writeValueAsString(value);
    } catch (Exception e) {
      throw new IllegalArgumentException(e);
    }
  }

  protected <T> T fromJson(InputStreamReader reader, Class<T> klass) {
    try {
      return mapper.readValue(reader, klass);
    } catch (Exception e) {
      throw new IllegalArgumentException(e);
    }
  }

  protected <T> T fromJson(String json, Class<T> klass) {
    if (json.endsWith(".json")) return fromJson(read(json), klass);
    try {
      return mapper.readValue(json, klass);
    } catch (Exception e) {
      throw new IllegalArgumentException(e);
    }
  }
}
