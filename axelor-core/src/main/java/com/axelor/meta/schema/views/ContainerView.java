/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.meta.schema.views;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.xml.bind.annotation.XmlTransient;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public interface ContainerView {
  List<AbstractWidget> getItems();

  @XmlTransient
  @JsonIgnore
  default List<AbstractWidget> getExtraItems() {
    return Collections.emptyList();
  }

  @XmlTransient
  @JsonIgnore
  default Set<String> getExtraNames() {
    return Collections.emptySet();
  }
}
