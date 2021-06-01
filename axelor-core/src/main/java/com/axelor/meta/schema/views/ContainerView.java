package com.axelor.meta.schema.views;

import java.util.Collections;
import java.util.List;
import java.util.Set;

public interface ContainerView {
  List<AbstractWidget> getItems();

  default List<AbstractWidget> getExtraItems() {
    return Collections.emptyList();
  }

  default Set<String> getExtraNames() {
    return Collections.emptySet();
  }
}
