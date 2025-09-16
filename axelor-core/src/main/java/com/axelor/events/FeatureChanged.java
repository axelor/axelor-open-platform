/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.events;

public class FeatureChanged {
  private final String featureName;
  private final boolean enabled;

  public FeatureChanged(String featureName, boolean enabled) {
    this.featureName = featureName;
    this.enabled = enabled;
  }

  public String getFeatureName() {
    return featureName;
  }

  public boolean isEnabled() {
    return enabled;
  }
}
