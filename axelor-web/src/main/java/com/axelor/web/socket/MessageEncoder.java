/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.web.socket;

import com.axelor.inject.Beans;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.websocket.EncodeException;
import jakarta.websocket.Encoder;
import jakarta.websocket.EndpointConfig;

public class MessageEncoder implements Encoder.Text<Message> {

  @Override
  public void init(EndpointConfig config) {}

  @Override
  public void destroy() {}

  @Override
  public String encode(Message object) throws EncodeException {
    try {
      return Beans.get(ObjectMapper.class).writeValueAsString(object);
    } catch (JsonProcessingException e) {
      throw new EncodeException(object, e.getMessage(), e);
    }
  }
}
