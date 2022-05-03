/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2022 Axelor (<http://axelor.com>).
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
package com.axelor.data.csv;

import com.axelor.common.StringUtils;
import com.axelor.db.JpaRepository;
import com.axelor.db.mapper.JsonProperty;
import com.axelor.meta.MetaStore;
import com.axelor.meta.db.MetaJsonRecord;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@XStreamAlias("bind")
public class CSVBindJson extends CSVBind {

  @XStreamAlias("json-model")
  @XStreamAsAttribute
  private String jsonModel;

  private String domain;

  private CSVInput input;

  private CSVBind parent;

  private List<CSVBind> jsonBindings;

  private boolean initialized;

  private void initialize() {
    try {
      if (StringUtils.notBlank(jsonModel)) {
        setType(MetaJsonRecord.class.getName());
        domain = String.format("self.jsonModel = '%s'", jsonModel);
        return;
      }

      final Object parentObject;
      final Class<?> jsonClass;
      final Function<Object, String> jsonModelFunc;
      final String parentType;

      if (input != null) {
        parentObject = input;
        jsonClass = CSVInputJson.class;
        jsonModelFunc = value -> ((CSVInputJson) value).getJsonModel();
        parentType = input.getTypeName();
      } else if (parent != null) {
        parentObject = parent;
        jsonClass = CSVBindJson.class;
        jsonModelFunc = value -> ((CSVBindJson) value).getJsonModel();
        parentType = parent.getType();
      } else {
        return;
      }

      final Optional<String> parentJsonModel =
          Optional.of(parentObject)
              .filter(jsonClass::isInstance)
              .map(jsonClass::cast)
              .map(jsonModelFunc)
              .filter(StringUtils::notBlank);
      final List<String> fieldParts =
          Arrays.asList(
              getField().substring(JsonProperty.KEY_JSON_PREFIX.length()).split("\\.", 2));

      @SuppressWarnings("unchecked")
      final Map<String, Object> jsonField =
          (Map<String, Object>)
              Optional.ofNullable(
                      parentJsonModel.isPresent()
                          ? MetaStore.findJsonFields(parentJsonModel.get())
                          : MetaStore.findJsonFields(parentType, fieldParts.get(0)))
                  .map(fields -> fields.get(fieldParts.get(1)))
                  .orElse(Collections.emptyMap());
      jsonModel = (String) jsonField.get("jsonTarget");
      setType((String) jsonField.get("target"));
      domain = (String) jsonField.get("domain");
    } finally {
      initialized = true;
    }
  }

  public String getJsonModel() {
    if (!initialized) {
      initialize();
    }

    return jsonModel;
  }

  public void setJsonModel(String jsonModel) {
    this.jsonModel = jsonModel;
    initialized = false;
  }

  @Override
  public String getType() {
    if (!initialized) {
      initialize();
    }

    return super.getType();
  }

  public CSVInput getInput() {
    return input;
  }

  public void setInput(CSVInput input) {
    this.input = input;
  }

  public CSVBind getParent() {
    return parent;
  }

  public void setParent(CSVBind parent) {
    this.parent = parent;
  }

  @Override
  public String getSearch() {
    final String search = super.getSearch();

    if (StringUtils.isBlank(getJsonModel()) || StringUtils.isBlank(search)) {
      return search;
    }

    return Optional.of(
            Stream.of(search, domain)
                .filter(StringUtils::notBlank)
                .map(item -> String.format("(%s)", item))
                .collect(Collectors.joining(" AND ")))
        .filter(StringUtils::notBlank)
        .orElse(null);
  }

  @Override
  public List<CSVBind> getBindings() {
    if (StringUtils.notBlank(getJsonModel())) {
      final List<CSVBind> bindings =
          Optional.ofNullable(super.getBindings()).orElse(Collections.emptyList());
      return Stream.concat(getJsonBindings().stream(), bindings.stream())
          .collect(Collectors.toList());
    }

    return super.getBindings();
  }

  private List<CSVBind> getJsonBindings() {
    if (jsonBindings == null) {
      final CSVBind jsonModelBinding = new CSVBind();
      jsonModelBinding.setField("jsonModel");
      jsonModelBinding.setExpression(String.format("'%s'", getJsonModel()));
      jsonBindings = Collections.singletonList(jsonModelBinding);
    }

    return jsonBindings;
  }

  @Override
  public Object postProcess(Object bean) {
    if (StringUtils.notBlank(getJsonModel())) {
      return JpaRepository.of(MetaJsonRecord.class).save((MetaJsonRecord) bean);
    }

    return super.postProcess(bean);
  }
}
