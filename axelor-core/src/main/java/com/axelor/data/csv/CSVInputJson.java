/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.data.csv;

import com.axelor.common.StringUtils;
import com.axelor.db.JpaRepository;
import com.axelor.meta.db.MetaJsonRecord;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@XStreamAlias("input")
public class CSVInputJson extends CSVInput {

  @XStreamAlias("json-model")
  @XStreamAsAttribute
  private String jsonModel;

  private List<CSVBind> jsonBindings = new ArrayList<>();

  @Override
  public String getTypeName() {
    return MetaJsonRecord.class.getName();
  }

  public String getJsonModel() {
    return jsonModel;
  }

  public void setJsonModel(String jsonModel) {
    this.jsonModel = jsonModel;
  }

  @Override
  public String getSearch() {
    final String search = super.getSearch();

    if (StringUtils.isBlank(search)) {
      return search;
    }

    return Stream.of(search, "self.jsonModel = '%s'".formatted(getJsonModel()))
        .map("(%s)"::formatted)
        .collect(Collectors.joining(" AND "));
  }

  @Override
  public List<CSVBind> getBindings() {
    return Stream.concat(getJsonBindings().stream(), super.getBindings().stream())
        .collect(Collectors.toList());
  }

  private List<CSVBind> getJsonBindings() {
    if (jsonBindings == null) {
      final CSVBind jsonModelBinding = new CSVBind();
      jsonModelBinding.setField("jsonModel");
      jsonModelBinding.setExpression("'%s'".formatted(getJsonModel()));
      jsonBindings = Collections.singletonList(jsonModelBinding);
    }

    return jsonBindings;
  }

  @Override
  public Object postProcess(Object bean) {
    return JpaRepository.of(MetaJsonRecord.class).save((MetaJsonRecord) bean);
  }
}
