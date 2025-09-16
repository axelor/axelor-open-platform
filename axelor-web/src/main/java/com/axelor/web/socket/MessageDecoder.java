/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.web.socket;

import com.axelor.common.StringUtils;
import com.axelor.inject.Beans;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.websocket.DecodeException;
import jakarta.websocket.Decoder;
import jakarta.websocket.EndpointConfig;

public class MessageDecoder implements Decoder.Text<Message> {

  @Override
  public void init(EndpointConfig config) {}

  @Override
  public void destroy() {}

  @Override
  public Message decode(String s) throws DecodeException {
    try {
      return Beans.get(ObjectMapper.class).readValue(s, Message.class);
    } catch (JsonProcessingException e) {
      throw new DecodeException(s, e.getMessage(), e);
    }
  }

  @Override
  public boolean willDecode(String s) {
    return StringUtils.notBlank(s);
  }
}
