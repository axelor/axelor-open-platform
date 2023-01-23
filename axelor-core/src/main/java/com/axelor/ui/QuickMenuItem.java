/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2023 Axelor (<http://axelor.com>).
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

import com.axelor.rpc.Context;
import com.google.common.base.MoreObjects;
import java.util.Optional;

/** An item in the quick menu dropdown */
public class QuickMenuItem {

  private String title;

  private String action;

  private Context context;

  private boolean selected;

  /** Create a new instance of the {@link QuickMenuItem} */
  public QuickMenuItem() {}

  /**
   * Create a new instance of the {@link QuickMenuItem}
   *
   * @param title title
   * @param action action
   * @param context context
   * @param selected selected state
   */
  public QuickMenuItem(String title, String action, Context context, boolean selected) {
    this.title = title;
    this.action = action;
    this.context = context;
    this.selected = selected;
  }

  /**
   * Create a new instance of the {@link QuickMenuItem}
   *
   * @param title title
   * @param action action
   * @param context context
   */
  public QuickMenuItem(String title, String action, Context context) {
    this(title, action, context, false);
  }

  /**
   * Create a new instance of the {@link QuickMenuItem}
   *
   * @param title title
   * @param action action
   */
  public QuickMenuItem(String title, String action) {
    this(title, action, null);
  }

  /**
   * Title of the item
   *
   * @return item title
   */
  public String getTitle() {
    return title;
  }

  /**
   * Set the title of the item
   *
   * @param title title
   */
  public void setTitle(String title) {
    this.title = title;
  }

  /**
   * Name of action triggered when item is selected
   *
   * @return action
   */
  public String getAction() {
    return action;
  }

  /**
   * Set the action triggered when item is selected
   *
   * @param action action
   */
  public void setAction(String action) {
    this.action = action;
  }

  /**
   * Context to pass to the action
   *
   * @return action context
   */
  public Context getContext() {
    return context;
  }

  /**
   * Set the context to pass to the action
   *
   * @param context context
   */
  public void setContext(Context context) {
    this.context = context;
  }

  /**
   * Model of the context
   *
   * @return context model
   */
  public Class<?> getModel() {
    return Optional.ofNullable(getContext()).map(Context::getContextClass).orElse(null);
  }

  /**
   * Selected state of the item
   *
   * @return selected state
   */
  public boolean isSelected() {
    return selected;
  }

  /**
   * Set the selected state of the item
   *
   * @param selected true or false
   */
  public void setSelected(boolean selected) {
    this.selected = selected;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("title", getTitle())
        .add("action", getAction())
        .add("context", getContext())
        .add("selected", isSelected())
        .omitNullValues()
        .toString();
  }
}
