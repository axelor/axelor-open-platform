package com.axelor.meta.schema.views;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;

@XmlType
@JsonInclude(Include.NON_NULL)
public class ChartSearchField extends BaseSearchField {

  @XmlAttribute private Boolean multiple;

  public Boolean getMultiple() {
    return multiple;
  }

  public void setMultiple(Boolean multiple) {
    this.multiple = multiple;
  }
}
