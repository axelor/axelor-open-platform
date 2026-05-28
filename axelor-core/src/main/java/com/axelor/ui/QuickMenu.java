/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
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
