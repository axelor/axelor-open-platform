/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.meta.loader;

class DemoLoader extends DataLoader {

  private static final String DATA_DIR_NAME = "data-demo";

  @Override
  protected String getDirName() {
    return DATA_DIR_NAME;
  }
}
