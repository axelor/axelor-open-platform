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
package com.axelor.ui;

import com.google.common.base.MoreObjects;
import java.util.Arrays;
import java.util.List;

/** A quick menu displayed on default page */
public class QuickMenu {

  private String title;

  private int order;

  private boolean showingSelected;

  private List<QuickMenuItem> items;

  /** Create a new instance of the {@link QuickMenu} */
  public QuickMenu() {}

  /**
   * Create a new instance of the {@link QuickMenu}
   *
   * @param title title
   * @param order order
   * @param showingSelected whether to show selected state on items
   * @param items list of {@link QuickMenuItem}
   */
  public QuickMenu(String title, int order, boolean showingSelected, List<QuickMenuItem> items) {
    this.title = title;
    this.order = order;
    this.showingSelected = showingSelected;
    this.items = items;
  }

  /**
   * Create a new instance of the {@link QuickMenu}
   *
   * @param title title
   * @param order order
   * @param items list of {@link QuickMenuItem}
   */
  public QuickMenu(String title, int order, List<QuickMenuItem> items) {
    this(title, order, false, items);
  }

  /**
   * Create a new instance of the {@link QuickMenu}
   *
   * @param title title
   * @param order order
   * @param showingSelected whether to show selected state on items
   * @param items items of {@link QuickMenuItem}
   */
  public QuickMenu(String title, int order, boolean showingSelected, QuickMenuItem... items) {
    this(title, order, showingSelected, Arrays.asList(items));
  }

  /**
   * Create a new instance of the {@link QuickMenu}
   *
   * @param title title
   * @param order order
   * @param items items of {@link QuickMenuItem}
   */
  public QuickMenu(String title, int order, QuickMenuItem... items) {
    this(title, order, false, items);
  }

  /**
   * Title of the menu
   *
   * @return menu title
   */
  public String getTitle() {
    return title;
  }

  /**
   * Set title of the menu
   *
   * @param title title
   */
  public void setTitle(String title) {
    this.title = title;
  }

  /**
   * Order of the menu
   *
   * @return menu order
   */
  public int getOrder() {
    return order;
  }

  /**
   * Set the order of the menu
   *
   * @param order order
   */
  public void setOrder(int order) {
    this.order = order;
  }

  /**
   * Whether to show selected state on items
   *
   * @return showing selected
   */
  public boolean isShowingSelected() {
    return showingSelected;
  }

  /**
   * Whether to show selected state on items
   *
   * @param showingSelected true or false
   */
  public void setShowingSelected(boolean showingSelected) {
    this.showingSelected = showingSelected;
  }

  /**
   * Items in the menu dropdown
   *
   * @return list of {@link QuickMenuItem}
   */
  public List<QuickMenuItem> getItems() {
    return items;
  }

  /**
   * Set items in the menu dropdown
   *
   * @param items list of {@link QuickMenuItem}
   */
  public void setItems(List<QuickMenuItem> items) {
    this.items = items;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("title", getTitle())
        .add("order", getOrder())
        .add("showingSelected", isShowingSelected())
        .add("items", getItems())
        .omitNullValues()
        .toString();
  }
}
