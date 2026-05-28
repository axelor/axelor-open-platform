/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.ui;

import com.axelor.common.ObjectUtils;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Singleton
public class QuickMenuService {

  private Set<QuickMenuCreator> handlers = new HashSet<>();

  @Inject
  public QuickMenuService(Set<QuickMenuCreator> handlers) {
    this.handlers = handlers;
  }

  public List<QuickMenu> get() {
    return handlers.stream()
        .map(QuickMenuCreator::create)
        .filter(menu -> menu != null && ObjectUtils.notEmpty(menu.getItems()))
        .sorted(Comparator.comparing(QuickMenu::getOrder))
        .collect(Collectors.toList());
  }
}
