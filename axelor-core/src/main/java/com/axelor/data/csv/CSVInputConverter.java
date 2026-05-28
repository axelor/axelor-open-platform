/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.data.csv;

import com.axelor.common.StringUtils;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.converters.reflection.ReflectionProvider;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

public class CSVInputConverter implements Converter {
  private final Converter defaultConverter;
  private final ReflectionProvider reflectionProvider;

  public CSVInputConverter(Converter defaultConverter, ReflectionProvider reflectionProvider) {
    this.defaultConverter = defaultConverter;
    this.reflectionProvider = reflectionProvider;
  }

  @Override
  public boolean canConvert(@SuppressWarnings("rawtypes") Class type) {
    return CSVInput.class.isAssignableFrom(type);
  }

  @Override
  public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
    final String jsonModel = reader.getAttribute("json-model");
    return StringUtils.isBlank(jsonModel)
        ? newInstance(context)
        : newInstanceJson(context, jsonModel);
  }

  private Object newInstance(UnmarshallingContext context) {
    return newInstance(CSVInput.class, context);
  }

  private Object newInstanceJson(UnmarshallingContext context, String jsonModel) {
    final Object result = newInstance(CSVInputJson.class, context);
    reflectionProvider.writeField(result, "jsonModel", jsonModel, CSVInputJson.class);
    return result;
  }

  private Object newInstance(Class<?> resultType, UnmarshallingContext context) {
    final Object result = reflectionProvider.newInstance(resultType);
    return context.convertAnother(result, resultType, defaultConverter);
  }
}
