/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.data.xml;

import com.axelor.common.StringUtils;
import com.axelor.db.mapper.JsonProperty;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.converters.reflection.ReflectionProvider;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

public class XMLBindConverter implements Converter {
  private final Converter defaultConverter;
  private final ReflectionProvider reflectionProvider;

  public XMLBindConverter(Converter defaultConverter, ReflectionProvider reflectionProvider) {
    this.defaultConverter = defaultConverter;
    this.reflectionProvider = reflectionProvider;
  }

  @Override
  public boolean canConvert(@SuppressWarnings("rawtypes") Class type) {
    return XMLBind.class.isAssignableFrom(type);
  }

  @Override
  public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
    final String field = reader.getAttribute("to");

    if (StringUtils.isBlank(field)) {
      final String jsonModel = reader.getAttribute("json-model");
      return StringUtils.isBlank(jsonModel)
          ? newInstance(context)
          : newInstanceJson(context, jsonModel);
    }

    return field.startsWith(JsonProperty.KEY_JSON_PREFIX)
        ? newInstanceJson(context, reader.getAttribute("json-model"))
        : newInstance(context);
  }

  private Object newInstance(UnmarshallingContext context) {
    return newInstance(XMLBind.class, context);
  }

  private Object newInstanceJson(UnmarshallingContext context, String jsonModel) {
    final Object result = newInstance(XMLBindJson.class, context);

    if (StringUtils.notBlank(jsonModel)) {
      reflectionProvider.writeField(result, "jsonModel", jsonModel, XMLBindJson.class);
    }

    return result;
  }

  private Object newInstance(Class<?> resultType, UnmarshallingContext context) {
    final Object result = reflectionProvider.newInstance(resultType);
    return context.convertAnother(result, resultType, defaultConverter);
  }
}
