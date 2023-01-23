/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2023 Axelor (<http://axelor.com>).
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
package com.axelor.web.socket;

import com.axelor.common.StringUtils;
import com.axelor.inject.Beans;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import javax.websocket.DecodeException;
import javax.websocket.Decoder;
import javax.websocket.EndpointConfig;

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
